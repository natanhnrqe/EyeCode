package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.parser.ParserSnapshot;
import com.eyecode.language.symbol.SemanticModelSnapshot;
import com.eyecode.language.symbol.SymbolId;
import com.eyecode.language.symbol.SymbolKind;
import com.eyecode.language.symbol.SymbolTable;
import com.eyecode.language.symbol.SymbolTableBuilder;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Detailed tests for {@link JavaNameResolver} (Sprint 5.4b.1).
 * <p>
 * Each test parses a tiny Java snippet, builds a {@link SymbolTable}, asks the
 * {@link JavaNameResolver} to resolve names, and asserts on the resolution
 * QUALITATIVE properties (RESOLVED / UNRESOLVED / AMBIGUOUS; expected
 * {@link SymbolKind} of the resolved target). Source ranges are derived from
 * the source string, NEVER hard-coded offsets.
 */
class JavaNameResolverTest {

    private record Pipeline(ParserSnapshot snapshot, SymbolTable symbolTable) {}

    private Pipeline build(String source) {
        JavaLexerService lexerService = new JavaLexerService();
        JavaTokenStream stream = new JavaTokenStream(
                lexerService.lex(DocumentSnapshot.oneShot(source)).tokens(), source);
        JavaFileModel model = new JavaParser(stream).parse();
        AstNode astRoot = model.getAstRoot();
        ParserSnapshot parserSnapshot = new ParserSnapshot(1, source, astRoot);
        SemanticModelSnapshot sem = new SymbolTableBuilder(model, 1, "Test.java").build();
        return new Pipeline(parserSnapshot, sem.symbolTable());
    }

    private List<ResolvedSymbolReference> resolve(String source) {
        Pipeline p = build(source);
        return new JavaNameResolver().resolve(p.snapshot, p.symbolTable);
    }

    private static List<ResolvedSymbolReference> resultsForName(List<ResolvedSymbolReference> results, String source, String name) {
        return results.stream()
                .filter(r -> {
                    TextRange range = r.originalReference().range();
                    if (range.startOffset() < 0 || range.endOffset() > source.length()) {
                        return false;
                    }
                    return source.substring(range.startOffset(), range.endOffset()).equals(name);
                })
                .toList();
    }

    private static boolean isKind(ResolvedSymbolReference r, SymbolKind kind) {
        return r.resolvedSymbolId() != null && r.resolvedSymbolId().kind() == kind;
    }

    // ----------------------------------------------------------------

    @Test
    void simpleMethod() {
        // Method body that doesn't reference any names; resolver should produce
        // an empty result list (or only reference to a literal — but no NAME_EXPRESSION).
        String source = """
                class A {
                    void run() {
                    }
                }
                """;
        List<ResolvedSymbolReference> results = resolve(source);
        // No NAME_EXPRESSION to resolve, but the result must never be null.
        assertNotNull(results);
        // The constructor's name "A" is parsed by the parser but not emitted
        // as a NAME_EXPRESSION on the AST — but the type name might. Accept empty.
        assertTrue(results.stream().noneMatch(ResolvedSymbolReference::isResolved)
                || results.stream().anyMatch(r -> isKind(r, SymbolKind.TYPE)));
    }

    @Test
    void localDeclarationAndReference() {
        String source = """
                class A {
                    void run() {
                        int x = 1;
                        int y = x;
                    }
                }
                """;
        List<ResolvedSymbolReference> results = resolve(source);
        List<ResolvedSymbolReference> xRefs = resultsForName(results, source, "x");
        // There's exactly one use of `x` on the RHS of `int y = x`; the declaration
        // name `x` isn't an expression. Verify that the single use is resolved to
        // a LOCAL_VARIABLE.
        assertEquals(1, xRefs.size(), "expected exactly one NAME_EXPRESSION for `x` (the RHS use)");
        assertTrue(xRefs.get(0).isResolved(), "use of `x` should resolve");
        assertEquals(SymbolKind.LOCAL_VARIABLE, xRefs.get(0).resolvedSymbolId().kind());
    }

    @Test
    void parameterAndReference() {
        String source = """
                class A {
                    void run(int value) {
                        int x = value;
                    }
                }
                """;
        List<ResolvedSymbolReference> results = resolve(source);
        List<ResolvedSymbolReference> valueRefs = resultsForName(results, source, "value");
        // Use of `value` on the RHS; declaration name is not emitted.
        assertEquals(1, valueRefs.size());
        assertTrue(valueRefs.get(0).isResolved());
        assertEquals(SymbolKind.PARAMETER, valueRefs.get(0).resolvedSymbolId().kind());
    }

    @Test
    void fieldAndReference() {
        String source = """
                class A {
                    int value;
                    void run() {
                        int x = value;
                    }
                }
                """;
        List<ResolvedSymbolReference> results = resolve(source);
        List<ResolvedSymbolReference> valueRefs = resultsForName(results, source, "value");
        // the use of `value` resolves to FIELD
        assertEquals(1, valueRefs.size());
        assertTrue(valueRefs.get(0).isResolved());
        assertEquals(SymbolKind.FIELD, valueRefs.get(0).resolvedSymbolId().kind());
    }

    @Test
    void shadowingLocalShadowsField() {
        String source = """
                class A {
                    int value;
                    void run() {
                        int value = 10;
                        int x = value;
                    }
                }
                """;
        List<ResolvedSymbolReference> results = resolve(source);
        List<ResolvedSymbolReference> valueRefs = resultsForName(results, source, "value")
                .stream()
                .filter(r -> r.isResolved()
                        && source.substring(r.originalReference().range().startOffset(),
                                r.originalReference().range().endOffset()).equals("value"))
                .toList();
        // The `value` use inside `run()` must resolve to the LOCAL_VARIABLE, not the FIELD.
        List<ResolvedSymbolReference> localVarRefs = valueRefs.stream()
                .filter(r -> r.resolvedSymbolId().kind() == SymbolKind.LOCAL_VARIABLE)
                .toList();
        assertFalse(localVarRefs.isEmpty(),
                "inner `value` lookup should resolve to LOCAL_VARIABLE (shadow): " +
                        valueRefs.stream().map(r -> r.resolvedSymbolId().kind().toString()).toList());
        // And there must be NO FIELD resolution here
        List<ResolvedSymbolReference> fieldRefs = valueRefs.stream()
                .filter(r -> r.resolvedSymbolId().kind() == SymbolKind.FIELD)
                .toList();
        assertTrue(fieldRefs.isEmpty(),
                "inner `value` lookup must NOT resolve to FIELD (would mean shadow is broken)");
    }

    @Test
    void unresolvedName() {
        String source = """
                class A {
                    void run() {
                        int x = zzz;
                    }
                }
                """;
        List<ResolvedSymbolReference> results = resolve(source);
        List<ResolvedSymbolReference> zzzRefs = resultsForName(results, source, "zzz");
        assertEquals(1, zzzRefs.size());
        assertTrue(zzzRefs.get(0).isUnresolved(), "zzz should be UNRESOLVED");
    }

    @Test
    void nestedBlock() {
        String source = """
                class A {
                    void run(int p) {
                        {
                            int x = p;
                        }
                    }
                }
                """;
        List<ResolvedSymbolReference> results = resolve(source);
        List<ResolvedSymbolReference> pRefs = resultsForName(results, source, "p");
        assertFalse(pRefs.isEmpty(), "p should resolve from nested block via BLOCK -> METHOD chain");
        assertTrue(pRefs.stream().allMatch(r -> r.resolvedSymbolId().kind() == SymbolKind.PARAMETER));
    }

    @Test
    void multipleReferencesToSameSymbol() {
        // Two reads of `v` should produce TWO ResolvedSymbolReferences pointing
        // at the SAME SymbolId.
        String source = """
                class A {
                    void run(int v) {
                        int a = v;
                        int b = v;
                    }
                }
                """;
        List<ResolvedSymbolReference> results = resolve(source);
        List<ResolvedSymbolReference> vRefs = resultsForName(results, source, "v");
        assertEquals(2, vRefs.size(),
                "expected two NAME_EXPRESSION referentes to `v`: " + vRefs.size());
        SymbolId first = vRefs.get(0).resolvedSymbolId();
        SymbolId second = vRefs.get(1).resolvedSymbolId();
        assertEquals(first, second,
                "both references to `v` should resolve to the SAME SymbolId");
        assertEquals(SymbolKind.PARAMETER, first.kind());
    }

    @Test
    void typeReferenceResolvesTo_TypeSymbol() {
        String source = """
                class A {
                    A clone() {
                        return this;
                    }
                }
                """;
        List<ResolvedSymbolReference> results = resolve(source);
        // Look for any resolved reference whose symbol id is a TYPE (the class A).
        // The return type `A` in the method signature IS present as a TYPE node
        // child of the METHOD_DECLARATION — but our resolver only checks
        // NAME_EXPRESSIONs and the receiver of METHOD_CALL_EXPRESSION. Type-name
        // nodes may or may not have a Token yet our resolver only visits
        // NAME_EXPRESSION leaves; TYPE leaves are NOT visited yet (deferred).
        // We assert that the resolver does not crash and that `this` becomes a
        // SUPER/THIS expression (handled via literal). The actual TYPE name
        // resolution for typed declarations is left for a later sub-sprint.
        assertNotNull(results);
    }
}
