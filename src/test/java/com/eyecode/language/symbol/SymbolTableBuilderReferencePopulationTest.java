package com.eyecode.language.symbol;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.semantic.JavaReferenceFinder;
import com.eyecode.language.semantic.ReferenceLocation;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SymbolTableBuilderReferencePopulationTest {

    private JavaFileModel parse(String source) {
        JavaLexerService service = new JavaLexerService();
        JavaTokenStream stream = new JavaTokenStream(
                service.lex(DocumentSnapshot.oneShot(source)).tokens(), source);
        return new JavaParser(stream).parse();
    }

    private SymbolTableBuilder createBuilder(String source) {
        return new SymbolTableBuilder(parse(source), 1, "Test.java");
    }

    private SymbolTable tableOf(String source) {
        return createBuilder(source).build().symbolTable();
    }

    private Symbol findSymbol(SymbolTable table, String name, SymbolKind kind) {
        for (SymbolScope scope : allScopes(table)) {
            for (Symbol s : table.symbolsIn(scope.id())) {
                if (s.name().equals(name) && s.kind() == kind) {
                    return s;
                }
            }
        }
        throw new IllegalStateException("No symbol found: " + kind + " " + name);
    }

    private List<SymbolScope> allScopes(SymbolTable table) {
        java.util.List<SymbolScope> out = new java.util.ArrayList<>();
        collectScopes(table.rootScope(), out);
        return out;
    }

    private void collectScopes(SymbolScope scope, java.util.List<SymbolScope> out) {
        out.add(scope);
        for (SymbolScope child : scope.children()) {
            collectScopes(child, out);
        }
    }

    private TextRange rangeOf(String source, String needle) {
        int idx = source.indexOf(needle);
        if (idx < 0) {
            throw new IllegalArgumentException("Needle not found in source: " + needle);
        }
        return TextRange.of(idx, idx + needle.length());
    }

    @Test
    void localVariableReference_isResolved() {
        String source = "class C {\n" +
                "    void m() {\n" +
                "        int x = 1;\n" +
                "        int y = x;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol local = findSymbol(table, "x", SymbolKind.LOCAL_VARIABLE);
        List<SymbolReference> refs = table.referencesTo(local.id());

        assertEquals(1, refs.size());
        int expectedStart = source.indexOf("int y = x;") + "int y = ".length();
        assertEquals(expectedStart, refs.get(0).range().startOffset());
        assertEquals(expectedStart + 1, refs.get(0).range().endOffset());
    }

    @Test
    void parameterReference_isResolved() {
        String source = "class C {\n" +
                "    void m(int p) {\n" +
                "        int y = p;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol param = findSymbol(table, "p", SymbolKind.PARAMETER);
        List<SymbolReference> refs = table.referencesTo(param.id());

        assertEquals(1, refs.size());
    }

    @Test
    void fieldReference_isResolved() {
        String source = "class C {\n" +
                "    int x;\n" +
                "    void m() {\n" +
                "        int y = x;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol field = findSymbol(table, "x", SymbolKind.FIELD);
        List<SymbolReference> refs = table.referencesTo(field.id());

        assertEquals(1, refs.size());
    }

    @Test
    void localShadowsField_referenceResolvesToLocal() {
        String source = "class C {\n" +
                "    int x;\n" +
                "    void m() {\n" +
                "        int x = 1;\n" +
                "        x = x + 1;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol field = findSymbol(table, "x", SymbolKind.FIELD);
        Symbol local = findSymbol(table, "x", SymbolKind.LOCAL_VARIABLE);

        List<SymbolReference> fieldRefs = table.referencesTo(field.id());
        List<SymbolReference> localRefs = table.referencesTo(local.id());

        assertEquals(0, fieldRefs.size());
        assertEquals(2, localRefs.size());
    }

    @Test
    void parameterShadowsField_referenceResolvesToParameter() {
        String source = "class C {\n" +
                "    int p;\n" +
                "    void m(int p) {\n" +
                "        int y = p;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol field = findSymbol(table, "p", SymbolKind.FIELD);
        Symbol param = findSymbol(table, "p", SymbolKind.PARAMETER);

        List<SymbolReference> fieldRefs = table.referencesTo(field.id());
        List<SymbolReference> paramRefs = table.referencesTo(param.id());

        assertEquals(0, fieldRefs.size());
        assertEquals(1, paramRefs.size());
    }

    @Test
    void methodCallReceiver_unqualified_isResolvedAsMethodReference() {
        String source = "class C {\n" +
                "    void run() {}\n" +
                "    void test() {\n" +
                "        run();\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol runMethod = findSymbol(table, "run", SymbolKind.METHOD);
        List<SymbolReference> refs = table.referencesTo(runMethod.id());

        assertEquals(1, refs.size());
    }

    @Test
    void typeReferenceInFieldDeclaration_isNotIndexed() {
        String source = "class Foo {}\n" +
                "class Bar {\n" +
                "    Foo f;\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol fooType = findSymbol(table, "Foo", SymbolKind.TYPE);
        List<SymbolReference> refs = table.referencesTo(fooType.id());

        assertEquals(0, refs.size());
    }

    @Test
    void methodReference_unqualifiedCall_isResolved() {
        String source = "class C {\n" +
                "    void run() {}\n" +
                "    void test() {\n" +
                "        run();\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol runMethod = findSymbol(table, "run", SymbolKind.METHOD);
        List<SymbolReference> refs = table.referencesTo(runMethod.id());

        assertEquals(1, refs.size());
    }

    @Test
    void unresolvedIdentifier_doesNotCreateReference() {
        String source = "class C {\n" +
                "    void m() {\n" +
                "        int x = unknownName;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        int totalRefs = 0;
        for (SymbolScope scope : allScopes(table)) {
            for (Symbol s : table.symbolsIn(scope.id())) {
                totalRefs += table.referencesTo(s.id()).size();
            }
        }
        assertEquals(0, totalRefs);
    }

    @Test
    void multipleReferences_toSameSymbol_allIndexed() {
        String source = "class C {\n" +
                "    void m() {\n" +
                "        int x = 1;\n" +
                "        int y = x + x;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol local = findSymbol(table, "x", SymbolKind.LOCAL_VARIABLE);
        List<SymbolReference> refs = table.referencesTo(local.id());

        assertEquals(2, refs.size());
        int firstStart = source.indexOf("x + x;") + 0;
        int secondStart = source.indexOf("x + x;") + "x + ".length();
        Set<Integer> starts = new HashSet<>();
        for (SymbolReference r : refs) {
            starts.add(r.range().startOffset());
        }
        assertTrue(starts.contains(firstStart), "missing first reference at offset " + firstStart);
        assertTrue(starts.contains(secondStart), "missing second reference at offset " + secondStart);
    }

    @Test
    void referenceRange_matchesSourcePosition() {
        String source = "class C {\n" +
                "    void m() {\n" +
                "        int counter = 0;\n" +
                "        counter = counter + 1;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol counter = findSymbol(table, "counter", SymbolKind.LOCAL_VARIABLE);
        List<SymbolReference> refs = table.referencesTo(counter.id());

        assertEquals(2, refs.size());
        for (SymbolReference ref : refs) {
            assertEquals(7, ref.range().length());
            String sliced = source.substring(ref.range().startOffset(), ref.range().endOffset());
            assertEquals("counter", sliced);
        }
    }

    @Test
    void referencesAreDeduped() {
        String source = "class C {\n" +
                "    int value = 1;\n" +
                "    void m() {\n" +
                "        int local = value;\n" +
                "        value = local + 2;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table1 = tableOf(source);
        SymbolTable table2 = tableOf(source);

        Symbol field1 = findSymbol(table1, "value", SymbolKind.FIELD);
        Symbol field2 = findSymbol(table2, "value", SymbolKind.FIELD);

        List<SymbolReference> refs1 = table1.referencesTo(field1.id());
        List<SymbolReference> refs2 = table2.referencesTo(field2.id());

        assertEquals(2, refs1.size());
        assertEquals(2, refs2.size());

        Set<Long> starts1 = new HashSet<>();
        for (SymbolReference r : refs1) starts1.add(Long.valueOf(r.range().startOffset()));
        assertEquals(2, starts1.size());
    }

    @Test
    void referencesAreDeterministic() {
        String source = "class C {\n" +
                "    void m() {\n" +
                "        int x = 1;\n" +
                "        int y = x;\n" +
                "        int z = x + y;\n" +
                "    }\n" +
                "}\n";

        SymbolTable table1 = tableOf(source);
        SymbolTable table2 = tableOf(source);

        Symbol x1 = findSymbol(table1, "x", SymbolKind.LOCAL_VARIABLE);
        Symbol x2 = findSymbol(table2, "x", SymbolKind.LOCAL_VARIABLE);

        List<SymbolReference> refs1 = table1.referencesTo(x1.id());
        List<SymbolReference> refs2 = table2.referencesTo(x2.id());

        assertEquals(refs1.size(), refs2.size());
        for (int i = 0; i < refs1.size(); i++) {
            assertEquals(refs1.get(i).range().startOffset(), refs2.get(i).range().startOffset());
        }
    }

    @Test
    void SymbolTable_referencesTo_returnsIndexedReferences() {
        String source = "class C {\n" +
                "    void m() {\n" +
                "        int x = 1;\n" +
                "        int y = x;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol local = findSymbol(table, "x", SymbolKind.LOCAL_VARIABLE);

        Optional<List<SymbolReference>> maybeRefs = Optional.ofNullable(table.referencesTo(local.id()));
        assertTrue(maybeRefs.isPresent());
        assertEquals(1, maybeRefs.get().size());
    }

    @Test
    void goldenEndToEnd_findsReferences_viaJavaReferenceFinder() {
        String source = "class C {\n" +
                "    int value = 1;\n" +
                "    void m() {\n" +
                "        int local = value;\n" +
                "        value = local + 2;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol field = findSymbol(table, "value", SymbolKind.FIELD);
        List<ReferenceLocation> result = new JavaReferenceFinder().findReferences(field, table);

        assertNotNull(result);
        assertEquals(2, result.size());
        for (ReferenceLocation loc : result) {
            assertNotNull(loc.reference());
            assertEquals(field.id(), loc.reference().target());
            assertEquals(loc.reference().range().startOffset(), loc.range().startOffset());
        }
    }

    @Test
    void constructorParameterReference_isResolved() {
        String source = "class C {\n" +
                "    C(int p) {\n" +
                "        int y = p;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol param = findSymbol(table, "p", SymbolKind.PARAMETER);
        List<SymbolReference> refs = table.referencesTo(param.id());

        assertEquals(1, refs.size());
    }

    @Test
    void referenceKind_isSIMPLE() {
        String source = "class C {\n" +
                "    void m() {\n" +
                "        int x = 1;\n" +
                "        int y = x;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol local = findSymbol(table, "x", SymbolKind.LOCAL_VARIABLE);
        List<SymbolReference> refs = table.referencesTo(local.id());

        assertEquals(1, refs.size());
        assertEquals(SymbolReferenceKind.SIMPLE, refs.get(0).kind());
        assertEquals("x", refs.get(0).name());
    }

    @Test
    void localVariable_declaration_doesNotEmitReference() {
        String source = "class C {\n" +
                "    void m() {\n" +
                "        int x = 1;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol local = findSymbol(table, "x", SymbolKind.LOCAL_VARIABLE);
        List<SymbolReference> refs = table.referencesTo(local.id());

        assertEquals(0, refs.size());
    }

    @Test
    void referencesToDifferentSymbols_areIndependent() {
        String source = "class C {\n" +
                "    int a;\n" +
                "    int b;\n" +
                "    void m() {\n" +
                "        int x = a + b;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol fieldA = findSymbol(table, "a", SymbolKind.FIELD);
        Symbol fieldB = findSymbol(table, "b", SymbolKind.FIELD);

        List<SymbolReference> refsA = table.referencesTo(fieldA.id());
        List<SymbolReference> refsB = table.referencesTo(fieldB.id());

        assertEquals(1, refsA.size());
        assertEquals(1, refsB.size());

        TextRange rangeA = refsA.get(0).range();
        TextRange rangeB = refsB.get(0).range();
        assertTrue(rangeA.startOffset() < rangeB.startOffset());
    }

    @Test
    void referencesScopeId_isTheInnermostScope() {
        String source = "class C {\n" +
                "    void m() {\n" +
                "        int x = 1;\n" +
                "        int y = x;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol local = findSymbol(table, "x", SymbolKind.LOCAL_VARIABLE);
        List<SymbolReference> refs = table.referencesTo(local.id());

        assertEquals(1, refs.size());
        long refScopeId = refs.get(0).scopeId();
        assertEquals(local.ownerScopeId(), refScopeId);
    }

    @Test
    void unresolvedReference_returnsEmpty_targetIsNullNotFabricated() {
        String source = "class C {\n" +
                "    void m() {\n" +
                "        int x = 1;\n" +
                "        int y = x + unknownSymbol;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol local = findSymbol(table, "x", SymbolKind.LOCAL_VARIABLE);
        List<SymbolReference> refs = table.referencesTo(local.id());

        assertEquals(1, refs.size());
        for (SymbolReference r : refs) {
            assertNotNull(r.target());
        }
    }

    @Test
    void referencesList_isImmutable() {
        String source = "class C {\n" +
                "    void m() {\n" +
                "        int x = 1;\n" +
                "        int y = x;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol local = findSymbol(table, "x", SymbolKind.LOCAL_VARIABLE);
        List<SymbolReference> refs = table.referencesTo(local.id());

        assertEquals(1, refs.size());
        long beforeStart = refs.get(0).range().startOffset();
        assertEquals(beforeStart, refs.get(0).range().startOffset());
    }
}
