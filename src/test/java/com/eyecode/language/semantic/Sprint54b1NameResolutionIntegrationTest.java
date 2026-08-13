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
import com.eyecode.language.symbol.Symbol;
import com.eyecode.language.symbol.SymbolId;
import com.eyecode.language.symbol.SymbolKind;
import com.eyecode.language.symbol.SymbolTable;
import com.eyecode.language.symbol.SymbolTableBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for Sprint 5.4b.1: end-to-end pipeline
 * <pre>
 *   source -> parser -> SymbolTable -> JavaNameResolver -> ResolvedSymbolReference[]
 * </pre>
 * Verifies that the resolved {@link SymbolId}s of name references point at the
 * actual declaration Symbols in the {@link SymbolTable} produced from the same
 * source. Real TextRanges are used (no hardcoded offsets).
 */
class Sprint54b1NameResolutionIntegrationTest {

    private record Engine(JavaNameResolver resolver, ParserSnapshot snapshot, SymbolTable symbolTable, String source) {}

    private Engine build(String source) {
        JavaLexerService lexerService = new JavaLexerService();
        JavaTokenStream stream = new JavaTokenStream(
                lexerService.lex(DocumentSnapshot.oneShot(source)).tokens(), source);
        JavaFileModel model = new JavaParser(stream).parse();
        AstNode astRoot = model.getAstRoot();
        ParserSnapshot parserSnapshot = new ParserSnapshot(1, source, astRoot);
        SemanticModelSnapshot sem = new SymbolTableBuilder(model, 1, "Test.java").build();
        return new Engine(new JavaNameResolver(), parserSnapshot, sem.symbolTable(), source);
    }

    @Test
    void parameterResolvesToParameterDeclarationSymbol() {
        String source = """
                class Foo {
                    void test(int parameter) {
                        int local = parameter;
                    }
                }
                """;
        Engine e = build(source);
        List<ResolvedSymbolReference> results = e.resolver.resolve(e.snapshot, e.symbolTable);

        ResolvedSymbolReference parameterUse = firstNamed(results, e.source, "parameter", true);
        SymbolId resolvedId = parameterUse.resolvedSymbolId();
        // The resolved SymbolId should match a Symbol in the symbol table whose
        // kind is PARAMETER and whose name is "parameter".
        Optional<Symbol> hit = e.symbolTable.find(resolvedId);
        assertTrue(hit.isPresent(),
                "resolved SymbolId must point at a real Symbol in the SymbolTable");
        assertEquals(SymbolKind.PARAMETER, hit.get().kind());
        assertEquals("parameter", hit.get().name());
    }

    @Test
    void localVariableResolvesToLocalDeclarationSymbol() {
        String source = """
                class Foo {
                    void test() {
                        int local = 1;
                        int other = local;
                    }
                }
                """;
        Engine e = build(source);
        List<ResolvedSymbolReference> results = e.resolver.resolve(e.snapshot, e.symbolTable);

        ResolvedSymbolReference localUse = firstNamed(results, e.source, "local", true);
        SymbolId id = localUse.resolvedSymbolId();
        Optional<Symbol> hit = e.symbolTable.find(id);
        assertTrue(hit.isPresent());
        assertEquals(SymbolKind.LOCAL_VARIABLE, hit.get().kind());
        assertEquals("local", hit.get().name());
    }

    @Test
    void fieldResolvesToFieldDeclarationSymbol() {
        String source = """
                class Foo {
                    int value;
                    void test() {
                        int x = value;
                    }
                }
                """;
        Engine e = build(source);
        List<ResolvedSymbolReference> results = e.resolver.resolve(e.snapshot, e.symbolTable);

        ResolvedSymbolReference valueUse = firstNamed(results, e.source, "value", true);
        SymbolId id = valueUse.resolvedSymbolId();
        Optional<Symbol> hit = e.symbolTable.find(id);
        assertTrue(hit.isPresent());
        assertEquals(SymbolKind.FIELD, hit.get().kind());
        assertEquals("value", hit.get().name());
    }

    @Test
    void shadowingPrefersInnerLocal() {
        String source = """
                class Foo {
                    int value;
                    void test() {
                        int value = 10;
                        value++;
                    }
                }
                """;
        Engine e = build(source);
        List<ResolvedSymbolReference> results = e.resolver.resolve(e.snapshot, e.symbolTable);

        ResolvedSymbolReference valueUse = firstNamed(results, e.source, "value", true);
        SymbolId id = valueUse.resolvedSymbolId();
        Optional<Symbol> hit = e.symbolTable.find(id);
        assertTrue(hit.isPresent());
        assertEquals(SymbolKind.LOCAL_VARIABLE, hit.get().kind(),
                "shadowed `value` should resolve to the LOCAL_VARIABLE, not the FIELD");
        assertEquals("value", hit.get().name());
    }

    @Test
    void unresolvedReferenceProducesUnresolvedNoException() {
        String source = """
                class Foo {
                    void test() {
                        int x = zzz;
                    }
                }
                """;
        Engine e = build(source);
        // Resolve must NOT throw even though `zzz` cannot be resolved.
        List<ResolvedSymbolReference> results =
                org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                        () -> e.resolver.resolve(e.snapshot, e.symbolTable));
        ResolvedSymbolReference zzz = firstNamed(results, e.source, "zzz", false);
        assertTrue(zzz.isUnresolved());
        assertNotNull(zzz.originalReference().range());
    }

    @Test
    void multipleSameNameReferencesShareSymbolId() {
        String source = """
                class Foo {
                    void test(int v) {
                        int a = v;
                        int b = v;
                    }
                }
                """;
        Engine e = build(source);
        List<ResolvedSymbolReference> results = e.resolver.resolve(e.snapshot, e.symbolTable);
        List<ResolvedSymbolReference> vRefs = namedReferences(results, e.source, "v");
        assertEquals(2, vRefs.size());
        SymbolId a = vRefs.get(0).resolvedSymbolId();
        SymbolId b = vRefs.get(1).resolvedSymbolId();
        assertEquals(a, b, "both references to `v` must resolve to the same SymbolId");

        // The SymbolId points at the PARAMETER symbol
        Optional<Symbol> hit = e.symbolTable.find(a);
        assertTrue(hit.isPresent());
        assertEquals(SymbolKind.PARAMETER, hit.get().kind());
        assertEquals("v", hit.get().name());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static List<ResolvedSymbolReference> namedReferences(List<ResolvedSymbolReference> results, String source, String name) {
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

    private static ResolvedSymbolReference firstNamed(List<ResolvedSymbolReference> results, String source, String name, boolean resolved) {
        List<ResolvedSymbolReference> refs = namedReferences(results, source, name);
        org.junit.jupiter.api.Assertions.assertFalse(
                refs.isEmpty(), "expected at least one reference whose text equals `" + name + "`");
        if (resolved) {
            List<ResolvedSymbolReference> resolvedRefs = refs.stream()
                    .filter(ResolvedSymbolReference::isResolved)
                    .toList();
            org.junit.jupiter.api.Assertions.assertFalse(
                    resolvedRefs.isEmpty(), "at least one `" + name + "` reference should be RESOLVED");
            return resolvedRefs.get(0);
        } else {
            return refs.get(0);
        }
    }
}
