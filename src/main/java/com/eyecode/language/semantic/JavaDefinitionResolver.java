package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.symbol.Symbol;
import com.eyecode.language.symbol.SymbolReference;
import com.eyecode.language.symbol.SymbolReferenceKind;
import com.eyecode.language.symbol.SymbolScope;
import com.eyecode.language.symbol.SymbolScopeImpl;
import com.eyecode.language.symbol.SymbolTable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Default {@link DefinitionResolver} for Java (Sprint 5.4c.1).
 * <p>
 * Reuses the existing simple-name and qualified-name resolution
 * pipelines — no new lookup algorithm is introduced:
 * <ul>
 *   <li>{@code SIMPLE} / {@code SIMPLE_NAME} references →
 *       {@link JavaNameResolver#resolve(SymbolReference, SymbolTable)}
 *       (5.4b.1/5.4b.2 semantics: hierarchical scope lookup, lexically
 *       innermost declaration wins, no overload/inheritance/type
 *       inference);</li>
 *   <li>{@code QUALIFIED_NAME} references →
 *       {@link QualifiedReferenceResolver#resolve(SymbolReference,
 *       SymbolScope, QualifiedMemberLookup)} (5.4b.7 semantics:
 *       sequential left-to-right chain via
 *       {@link QualifiedNameResolver} + a
 *       {@link ScopeBasedQualifiedMemberLookup} built over the same
 *       {@link SymbolTable}).</li>
 * </ul>
 * When the inner resolver returns a {@link Symbol}, the result is
 * wrapped in {@link DefinitionLocation#of(Symbol)} — the location
 * points at the symbol's own {@code declarationRange}. When the inner
 * resolver returns an unresolved / ambiguous / non-supported outcome,
 * the result is {@link Optional#empty()} — no fabricated location.
 * <p>
 * <b>Supported {@link com.eyecode.language.symbol.SymbolKind}s</b>
 * (this sprint): {@code LOCAL_VARIABLE}, {@code PARAMETER},
 * {@code FIELD}, {@code TYPE}, {@code INTERFACE}, {@code ENUM},
 * {@code ANNOTATION}, {@code METHOD}, {@code CONSTRUCTOR}. These are
 * the kinds the existing pipelines already produce. {@code TYPE_PARAMETER}
 * is intentionally not enumerated here because the symbol-table
 * builder does not currently emit it (see 5.4b.2 §12 — the
 * {@code TYPE_PARAMETER} case in {@code SimpleNameResolverTest} is
 * {@code @Disabled}). Overload / inheritance / generics / type
 * inference / imports are out of scope.
 * <p>
 * <b>Read-only contract</b>: this resolver never mutates the
 * {@link SymbolTable}, the supplied {@link SymbolReference}, the
 * scope, the qualified name, or any symbol. It only reads and wraps.
 * <p>
 * Pure Core: zero Swing / JavaFX / AWT / editor-ui / workbench imports.
 */
public final class JavaDefinitionResolver implements DefinitionResolver {

    @Override
    public Optional<DefinitionLocation> resolve(SymbolReference reference, SymbolTable symbolTable) {
        Objects.requireNonNull(reference, "reference must not be null");
        Objects.requireNonNull(symbolTable, "symbolTable must not be null");

        SymbolReferenceKind kind = reference.kind();
        if (kind == SymbolReferenceKind.SIMPLE || kind == SymbolReferenceKind.SIMPLE_NAME) {
            return resolveSimple(reference, symbolTable);
        }
        if (kind == SymbolReferenceKind.CONSTRUCTOR_CALL) {
            return resolveConstructor(reference, symbolTable);
        }
        if (kind == SymbolReferenceKind.QUALIFIED_NAME) {
            return resolveQualified(reference, symbolTable);
        }
        // Unknown kind — never fabricate a location.
        return Optional.empty();
    }

    /**
     * Simple-name path — delegates to {@link JavaNameResolver} and wraps
     * the resulting {@link Symbol} (when RESOLVED) into a
     * {@link DefinitionLocation}.
     */
    private static Optional<DefinitionLocation> resolveSimple(SymbolReference reference,
                                                             SymbolTable symbolTable) {
        ResolvedSymbolReference resolved = new JavaNameResolver()
                .resolve(reference, symbolTable);
        if (!resolved.isResolved()) {
            return Optional.empty();
        }
        Optional<Symbol> symbol = symbolTable.find(resolved.resolvedSymbolId());
        if (symbol.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(DefinitionLocation.of(symbol.get()));
    }

    /**
     * Qualified-name path — delegates to {@link QualifiedReferenceResolver}
     * and wraps the resulting {@link Symbol} (when RESOLVED) into a
     * {@link DefinitionLocation}.
     * <p>
     * The resolver needs a starting {@link SymbolScope} and a
     * {@link QualifiedMemberLookup}. The scope is derived from the
     * reference range via an internal DFS over the scope tree (depth →
     * smaller area → inner-kind tie-break, mirroring
     * {@code QualifiedNameResolverTest.scopeOf}); the member lookup is
     * a {@link ScopeBasedQualifiedMemberLookup} built over the supplied
     * {@link SymbolTable}.
     */
    private static Optional<DefinitionLocation> resolveQualified(SymbolReference reference,
                                                               SymbolTable symbolTable) {
        SymbolScope scope = innermostScopeContaining(symbolTable, reference.range());
        QualifiedMemberLookup memberLookup = new ScopeBasedQualifiedMemberLookup(symbolTable);
        for (QualifiedMemberExpectation expectation : List.of(
                QualifiedMemberExpectation.STATIC_FIELD,
                QualifiedMemberExpectation.STATIC_METHOD)) {
            QualifiedReferenceResolution r = new QualifiedReferenceResolver()
                    .resolve(reference, scope, memberLookup, expectation);
            if (!r.isResolved()) {
                continue;
            }
            Symbol symbol = r.resolvedSymbol().orElseThrow();
            return Optional.of(DefinitionLocation.of(symbol));
        }
        return Optional.empty();
    }

    private static Optional<DefinitionLocation> resolveConstructor(SymbolReference reference,
                                                                   SymbolTable symbolTable) {
        SymbolScope scope = innermostScopeContaining(symbolTable, reference.range());
        Optional<Symbol> type = symbolTable.lookup(scope.id(), reference.name());
        if (type.isEmpty()) {
            return Optional.empty();
        }
        if (!isTypeLike(type.get())) {
            return Optional.empty();
        }
        Optional<SymbolScope> typeScope = symbolTable.scope(type.get().scopeId());
        if (typeScope.isEmpty()) {
            return Optional.empty();
        }
        Optional<Symbol> constructor = typeScope.get().findLocal(reference.name());
        if (constructor.isEmpty() || constructor.get().kind() != com.eyecode.language.symbol.SymbolKind.CONSTRUCTOR) {
            return Optional.empty();
        }
        return Optional.of(DefinitionLocation.of(constructor.get()));
    }

    private static boolean isTypeLike(Symbol symbol) {
        return switch (symbol.kind()) {
            case TYPE, INTERFACE, ENUM, ANNOTATION -> true;
            default -> false;
        };
    }

    /**
     * DFS over the scope tree picking the best scope that contains the
     * given range. Best = higher depth, then smaller area, then inner
     * kind (BLOCK > METHOD > TYPE > PACKAGE > ROOT). Mirrors the helper
     * in {@code QualifiedNameResolverTest.scopeOf}. The root scope is
     * always the fallback (its range is empty {@code (0,0)} and treated
     * as "contains everything").
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
            boolean holds = scope == root || contains(scope.range(), refRange);
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

    private static boolean contains(TextRange outer, TextRange inner) {
        return outer.startOffset() <= inner.startOffset()
                && inner.endOffset() <= outer.endOffset();
    }

    private static int kindRank(SymbolScope scope) {
        if (scope instanceof SymbolScopeImpl) {
            return switch (scope.kind()) {
                case BLOCK -> 0;
                case METHOD -> 1;
                case TYPE -> 2;
                case PACKAGE -> 3;
                case ROOT -> 4;
            };
        }
        return Integer.MAX_VALUE;
    }
}
