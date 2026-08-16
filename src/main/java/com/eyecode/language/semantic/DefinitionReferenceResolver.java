package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.Token;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.JavaTokenType;
import com.eyecode.language.java.LexerSnapshot;
import com.eyecode.language.symbol.SymbolReference;
import com.eyecode.language.symbol.SymbolScope;
import com.eyecode.language.symbol.SymbolTable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves the textual reference under a given caret offset
 * (Sprint 5.4c.2).
 * <p>
 * The caret-driven counterpart of the simple-name and qualified-name
 * resolution pipelines: given the document source text, a caret
 * offset, and the current {@link SymbolTable}, returns a
 * {@link SymbolReference} describing the textual occurrence under the
 * caret — or empty when the caret sits on whitespace, a comment, a
 * string literal, a character literal, or outside the document.
 * <p>
 * <b>Pipeline</b>:
 * <ol>
 *   <li>lex the source text via {@link JavaLexerService} (anonymous
 *       one-shot — definition lookups are read-only and per-call);</li>
 *   <li>find the {@link JavaTokenType#IDENTIFIER IDENTIFIER} token
 *       containing the caret offset (none → empty);</li>
 *   <li>extend the match to the left/right collecting
 *       {@code IDENTIFIER . IDENTIFIER . … } sequences with no
 *       intervening whitespace or other token — if more than one
 *       component is collected the result is a
 *       {@link SymbolReferenceKind#QUALIFIED_NAME} reference;
 *       otherwise it is a
 *       {@link SymbolReferenceKind#SIMPLE SIMPLE} reference;</li>
 *   <li>compute the lookup start scope via a DFS over the scope tree
 *       (depth → smaller area → inner-kind tie-break, mirroring
 *       {@code JavaDefinitionResolver.innermostScopeContaining});</li>
 *   <li>build a {@link SymbolReference} with the exact textual range
 *       and the computed scope id.</li>
 * </ol>
 * <p>
 * <b>Contract</b>:
 * <ul>
 *   <li>caret in {@link JavaTokenType#WHITESPACE WHITESPACE},
 *       {@link JavaTokenType#COMMENT COMMENT},
 *       {@link JavaTokenType#STRING STRING} or
 *       {@link JavaTokenType#CHARACTER CHARACTER} → empty (no
 *       reference can exist in literals / trivia);</li>
 *   <li>caret outside the document bounds (< 0 or >= length)
 *       → empty;</li>
 *   <li>no fabricated reference — only what the existing lexer
 *       recognizes as an identifier (or a contiguous dotted sequence
 *       of identifiers) produces a reference;</li>
 *   <li>{@link SymbolTable} is read-only — never mutated;</li>
 *   <li>no new parser, no new lexer — reuses the existing
 *       {@link JavaLexerService} pipeline.</li>
 * </ul>
 * <p>
 * <b>What it does NOT do</b> (deferred):
 * <ul>
 *   <li>{@code this} / {@code super} — these are {@code KEYWORD}
 *       tokens in the current lexer, so {@code this.field} lexes as
 *       {@code KEYWORD . IDENTIFIER}. Clicking on {@code field}
 *       yields a {@code SIMPLE} reference to {@code field}; clicking
 *       on {@code this} yields no reference (it's a keyword);</li>
 *   <li>annotation references ({@code @Foo}) — {@code @} is an
 *       {@code AT} token, the annotation name is an
 *       {@code IDENTIFIER}; clicking on the name yields a
 *       {@code SIMPLE} reference to {@code Foo};</li>
 *   <li>qualified names with whitespace around dots
 *       ({@code foo . bar}) — the lexer emits whitespace tokens
 *       between the identifier and the dot, so no contiguous
 *       identifier/dot chain can be assembled and the resolver falls
 *       back to a single-component {@code SIMPLE} reference;</li>
 *   <li>method-call receiver disambiguation
 *       ({@code obj.foo()} vs {@code foo()});</li>
 *   <li>editor navigation — pure Core, no Swing / JavaFX coupling.</li>
 * </ul>
 */
public final class DefinitionReferenceResolver {

    private final JavaLexerService lexerService;

    /**
     * Creates a resolver backed by a fresh {@link JavaLexerService}.
     */
    public DefinitionReferenceResolver() {
        this(new JavaLexerService());
    }

    /**
     * Creates a resolver backed by an explicit {@link JavaLexerService}
     * (testability / observability).
     */
    public DefinitionReferenceResolver(JavaLexerService lexerService) {
        this.lexerService = Objects.requireNonNull(lexerService, "lexerService must not be null");
    }

    /**
     * Resolves the {@link SymbolReference} under the caret.
     *
     * @param source   the document source text; never null
     * @param offset   the caret offset (0-based, in {@code [0, source.length()]})
     * @param table    the current symbol table; never null
     * @return the textual reference under the caret, or empty when the
     *         caret is not on an identifier (or contiguous qualified
     *         name)
     * @throws NullPointerException if any argument is null
     */
    public Optional<SymbolReference> resolve(String source, int offset, SymbolTable table) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(table, "table must not be null");

        if (offset < 0 || offset > source.length()) {
            return Optional.empty();
        }

        LexerSnapshot snapshot = lexerService.lex(DocumentSnapshot.oneShot(source));
        List<Token> tokens = snapshot.tokens();

        Token anchor = findAnchorIdentifier(tokens, offset);
        if (anchor == null) {
            return Optional.empty();
        }

        // Build the contiguous IDENTIFIER ( . IDENTIFIER )* range
        // by walking left and right. Both sides require:
        //   * the separator token is '.'
        //   * the tokens are immediately adjacent in the source
        //     (no whitespace / comment / etc. between)
        //   * the token on the far side of the separator is an
        //     IDENTIFIER (not a KEYWORD like `this` / `super`).
        int startIdx = indexOf(tokens, anchor);
        int endIdx = startIdx;
        // Walk left.
        while (canExtendLeft(tokens, startIdx)) {
            startIdx -= 2; // skip the '.' and the IDENTIFIER before it
        }
        // Walk right.
        while (canExtendRight(tokens, endIdx)) {
            endIdx += 2; // skip the '.' and consume the IDENTIFIER
        }

        Token first = tokens.get(startIdx);
        Token last = tokens.get(endIdx);
        TextRange refRange = TextRange.of(first.range().startOffset(),
                                          last.range().endOffset());

        SymbolScope scope = innermostScopeContaining(table, refRange);
        long scopeId = scope.id();

        int componentCount = (endIdx - startIdx) / 2 + 1;
        if (componentCount >= 2) {
            String name = source.substring(refRange.startOffset(), refRange.endOffset());
            return Optional.of(SymbolReference.qualified(name, scopeId, refRange));
        }
        return Optional.of(SymbolReference.simple(first.text(), scopeId, refRange));
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Finds the {@link JavaTokenType#IDENTIFIER} token containing the
     * caret offset (using the canonical inclusive range convention
     * {@code startOffset <= offset <= endOffset}). Returns {@code null}
     * if the offset falls on a non-IDENTIFIER token or outside any
     * token's range.
     */
    private static Token findAnchorIdentifier(List<Token> tokens, int offset) {
        for (Token t : tokens) {
            if (t.type() != JavaTokenType.IDENTIFIER) {
                continue;
            }
            if (t.range().startOffset() <= offset && offset <= t.range().endOffset()) {
                return t;
            }
        }
        return null;
    }

    private static boolean isDotSeparator(Token t) {
        return t.type() == JavaTokenType.SEPARATOR
                && t.text() != null
                && t.text().length() == 1
                && t.text().charAt(0) == '.';
    }

    /**
     * Whether the leftmost component can be extended leftward by one
     * IDENTIFIER. The previous token must be a {@code '.'} SEPARATOR
     * directly adjacent to the current anchor (no whitespace /
     * comment), and the token before that must be an IDENTIFIER (not a
     * KEYWORD — {@code this} / {@code super} are keywords and must
     * never participate in a {@code QUALIFIED_NAME} reference built
     * by this resolver).
     */
    private static boolean canExtendLeft(List<Token> tokens, int anchorIdx) {
        if (anchorIdx < 2) {
            return false;
        }
        Token sep = tokens.get(anchorIdx - 1);
        if (!isDotSeparator(sep)) {
            return false;
        }
        if (sep.range().endOffset() != tokens.get(anchorIdx).range().startOffset()) {
            return false;
        }
        Token leftIdent = tokens.get(anchorIdx - 2);
        if (leftIdent.type() != JavaTokenType.IDENTIFIER) {
            return false;
        }
        // The IDENTIFIER must be directly adjacent to the separator
        // (no whitespace / comment between).
        return leftIdent.range().endOffset() == sep.range().startOffset();
    }

    /**
     * Whether the rightmost component can be extended rightward by one
     * IDENTIFIER. Symmetric to {@link #canExtendLeft}.
     */
    private static boolean canExtendRight(List<Token> tokens, int anchorIdx) {
        int sepIdx = anchorIdx + 1;
        if (sepIdx + 1 >= tokens.size()) {
            return false;
        }
        Token sep = tokens.get(sepIdx);
        if (!isDotSeparator(sep)) {
            return false;
        }
        if (tokens.get(anchorIdx).range().endOffset() != sep.range().startOffset()) {
            return false;
        }
        Token rightIdent = tokens.get(sepIdx + 1);
        if (rightIdent.type() != JavaTokenType.IDENTIFIER) {
            return false;
        }
        return sep.range().endOffset() == rightIdent.range().startOffset();
    }

    /**
     * Linear lookup helper (token lists are short — full O(n) scan is
     * acceptable for a definition-lookup helper).
     */
    private static int indexOf(List<Token> tokens, Token target) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i) == target) {
                return i;
            }
        }
        return -1;
    }

    /**
     * DFS over the scope tree picking the best scope that contains the
     * given range. Mirrors {@code JavaDefinitionResolver.innermostScopeContaining}.
     */
    private static SymbolScope innermostScopeContaining(SymbolTable table, TextRange refRange) {
        SymbolScope root = table.rootScope();
        SymbolScope best = null;
        int bestDepth = -1;
        int bestArea = Integer.MAX_VALUE;

        Deque<Map.Entry<SymbolScope, Integer>> stack = new ArrayDeque<>();
        stack.push(Map.entry(root, 0));
        while (!stack.isEmpty()) {
            Map.Entry<SymbolScope, Integer> e = stack.pop();
            SymbolScope scope = e.getKey();
            int depth = e.getValue();
            boolean holds = scope == root
                    || (scope.range().startOffset() <= refRange.startOffset()
                        && refRange.endOffset() <= scope.range().endOffset());
            if (!holds) {
                continue;
            }
            int area = scope.range().endOffset() - scope.range().startOffset();
            boolean better;
            if (scope == root) {
                better = (best == null);
            } else if (depth > bestDepth) {
                better = true;
            } else if (depth == bestDepth) {
                if (area < bestArea) {
                    better = true;
                } else if (area == bestArea) {
                    better = kindRank(scope) < kindRank(best);
                } else {
                    better = false;
                }
            } else {
                better = false;
            }
            if (better) {
                best = scope;
                bestDepth = depth;
                bestArea = area;
            }
            for (SymbolScope c : scope.children()) {
                stack.push(Map.entry(c, depth + 1));
            }
        }
        return best != null ? best : root;
    }

    private static int kindRank(SymbolScope scope) {
        return switch (scope.kind()) {
            case BLOCK -> 0;
            case METHOD -> 1;
            case TYPE -> 2;
            case PACKAGE -> 3;
            case ROOT -> 4;
        };
    }
}
