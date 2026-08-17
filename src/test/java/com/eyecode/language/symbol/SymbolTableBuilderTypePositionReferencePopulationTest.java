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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SymbolTableBuilderTypePositionReferencePopulationTest {

    private JavaFileModel parse(String source) {
        JavaLexerService service = new JavaLexerService();
        JavaTokenStream stream = new JavaTokenStream(
                service.lex(DocumentSnapshot.oneShot(source)).tokens(), source);
        return new JavaParser(stream).parse();
    }

    private SymbolTable tableOf(String source) {
        return new SymbolTableBuilder(parse(source), 1, "Test.java", source)
                .build()
                .symbolTable();
    }

    private SymbolTable tableOfNoSource(String source) {
        return new SymbolTableBuilder(parse(source), 1, "Test.java")
                .build()
                .symbolTable();
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
    void localVariableType_isResolved() {
        String source = "class Foo {}\n" +
                "class Bar {\n" +
                "    void m() {\n" +
                "        Foo value;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol fooType = findSymbol(table, "Foo", SymbolKind.TYPE);
        List<SymbolReference> refs = table.referencesTo(fooType.id());

        assertEquals(1, refs.size());
        TextRange expectedRange = rangeOf(source, "Foo value");
        assertEquals(expectedRange.startOffset(), refs.get(0).range().startOffset());
    }

    @Test
    void fieldType_isResolved() {
        String source = "class Foo {}\n" +
                "class Bar {\n" +
                "    Foo field;\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol fooType = findSymbol(table, "Foo", SymbolKind.TYPE);
        List<SymbolReference> refs = table.referencesTo(fooType.id());

        assertEquals(1, refs.size());
        TextRange expectedRange = rangeOf(source, "Foo field");
        assertEquals(expectedRange.startOffset(), refs.get(0).range().startOffset());
    }

    @Test
    void methodReturnType_isResolved() {
        String source = "class Foo {}\n" +
                "class Bar {\n" +
                "    Foo test() { return null; }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol fooType = findSymbol(table, "Foo", SymbolKind.TYPE);
        List<SymbolReference> refs = table.referencesTo(fooType.id());

        assertEquals(1, refs.size());
    }

    @Test
    void parameterType_isResolved() {
        String source = "class Foo {}\n" +
                "class Bar {\n" +
                "    void test(Foo arg) {}\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol fooType = findSymbol(table, "Foo", SymbolKind.TYPE);
        List<SymbolReference> refs = table.referencesTo(fooType.id());

        assertEquals(1, refs.size());
    }

    @Test
    void newExpressionType_isResolved() {
        String source = "class Foo {}\n" +
                "class Bar {\n" +
                "    void test() {\n" +
                "        Object x = new Foo();\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol fooType = findSymbol(table, "Foo", SymbolKind.TYPE);
        List<SymbolReference> refs = table.referencesTo(fooType.id());

        assertEquals(1, refs.size());
        int expectedStart = source.indexOf("Foo();");
        assertEquals(expectedStart, refs.get(0).range().startOffset());
    }

    @Test
    void castType_isResolved() {
        String source = "class Foo {}\n" +
                "class Bar {\n" +
                "    void test(Object arg) {\n" +
                "        Foo x = (Foo) arg;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol fooType = findSymbol(table, "Foo", SymbolKind.TYPE);
        List<SymbolReference> refs = table.referencesTo(fooType.id());

        assertEquals(2, refs.size());
        Set<Long> starts = new HashSet<>();
        for (SymbolReference r : refs) starts.add(Long.valueOf(r.range().startOffset()));
        int castTypeStart = source.indexOf("(Foo) arg") + 1;
        assertTrue(starts.contains(Long.valueOf(castTypeStart)), "missing cast type ref at offset " + castTypeStart);
    }

    @Test
    void instanceofType_isResolved() {
        String source = "class Foo {}\n" +
                "class Bar {\n" +
                "    void test(Object x) {\n" +
                "        if (x instanceof Foo) {}\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol fooType = findSymbol(table, "Foo", SymbolKind.TYPE);
        List<SymbolReference> refs = table.referencesTo(fooType.id());

        assertEquals(1, refs.size());
        int expectedStart = source.indexOf("instanceof Foo") + "instanceof ".length();
        assertEquals(expectedStart, refs.get(0).range().startOffset());
    }

    @Test
    void multipleTypeOccurrencesOfSameType_allIndexed() {
        String source = "class Foo {}\n" +
                "class Bar {\n" +
                "    Foo field;\n" +
                "    Foo test(Foo arg) {\n" +
                "        Foo local = new Foo();\n" +
                "        Object x = (Foo) arg;\n" +
                "        if (x instanceof Foo) {}\n" +
                "        return arg;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol fooType = findSymbol(table, "Foo", SymbolKind.TYPE);
        List<SymbolReference> refs = table.referencesTo(fooType.id());

        assertEquals(7, refs.size());
    }

    @Test
    void unresolvedType_doesNotCreateReference() {
        String source = "class Bar {\n" +
                "    Unknown value;\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        int totalTypeRefs = 0;
        for (SymbolScope scope : allScopes(table)) {
            for (Symbol s : table.symbolsIn(scope.id())) {
                totalTypeRefs += table.referencesTo(s.id()).size();
            }
        }
        assertEquals(0, totalTypeRefs);
    }

    @Test
    void typeReferenceRanges_areExact() {
        String source = "class Foo {}\n" +
                "class Bar {\n" +
                "    Foo value;\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol fooType = findSymbol(table, "Foo", SymbolKind.TYPE);
        List<SymbolReference> refs = table.referencesTo(fooType.id());

        assertEquals(1, refs.size());
        TextRange refRange = refs.get(0).range();
        assertEquals(3, refRange.length());
        assertEquals("Foo", source.substring(refRange.startOffset(), refRange.endOffset()));
    }

    @Test
    void typeReferences_foundViaReferenceFinder() {
        String source = "class Foo {}\n" +
                "class Bar {\n" +
                "    Foo field;\n" +
                "    Foo test(Foo arg) {\n" +
                "        return arg;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol fooType = findSymbol(table, "Foo", SymbolKind.TYPE);
        List<ReferenceLocation> result = new JavaReferenceFinder().findReferences(fooType, table);

        assertNotNull(result);
        assertEquals(3, result.size());
        for (ReferenceLocation loc : result) {
            assertNotNull(loc.reference());
            assertEquals(fooType.id(), loc.reference().target());
        }
    }

    @Test
    void typeReferences_areDeduped() {
        String source = "class Foo {}\n" +
                "class Bar {\n" +
                "    Foo value;\n" +
                "}\n";
        SymbolTable table1 = tableOf(source);
        SymbolTable table2 = tableOf(source);

        Symbol foo1 = findSymbol(table1, "Foo", SymbolKind.TYPE);
        Symbol foo2 = findSymbol(table2, "Foo", SymbolKind.TYPE);

        List<SymbolReference> refs1 = table1.referencesTo(foo1.id());
        List<SymbolReference> refs2 = table2.referencesTo(foo2.id());

        assertEquals(1, refs1.size());
        assertEquals(1, refs2.size());

        Set<Long> starts1 = new HashSet<>();
        for (SymbolReference r : refs1) starts1.add(Long.valueOf(r.range().startOffset()));
        assertEquals(1, starts1.size());
    }

    @Test
    void typeReferenceConstruction_isDeterministic() {
        String source = "class Foo {}\n" +
                "class Bar {\n" +
                "    Foo field;\n" +
                "    void m() {\n" +
                "        Foo local = new Foo();\n" +
                "    }\n" +
                "}\n";

        SymbolTable table1 = tableOf(source);
        SymbolTable table2 = tableOf(source);

        Symbol foo1 = findSymbol(table1, "Foo", SymbolKind.TYPE);
        Symbol foo2 = findSymbol(table2, "Foo", SymbolKind.TYPE);

        List<SymbolReference> refs1 = table1.referencesTo(foo1.id());
        List<SymbolReference> refs2 = table2.referencesTo(foo2.id());

        assertEquals(refs1.size(), refs2.size());
        for (int i = 0; i < refs1.size(); i++) {
            assertEquals(refs1.get(i).range().startOffset(), refs2.get(i).range().startOffset());
        }
    }

    @Test
    void typeReferenceConstruction_doesNotMutateSymbolTable() {
        String source = "class Foo {}\n" +
                "class Bar {\n" +
                "    Foo field;\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol fooType = findSymbol(table, "Foo", SymbolKind.TYPE);
        SymbolId idBefore = fooType.id();
        long scopeIdBefore = fooType.ownerScopeId();
        int refsBefore = table.referencesTo(fooType.id()).size();

        for (int i = 0; i < 3; i++) {
            List<SymbolReference> refs = table.referencesTo(fooType.id());
            assertEquals(refsBefore, refs.size());
        }

        Symbol fooAfter = findSymbol(table, "Foo", SymbolKind.TYPE);
        assertEquals(idBefore, fooAfter.id());
        assertEquals(scopeIdBefore, fooAfter.ownerScopeId());
        assertEquals(refsBefore, table.referencesTo(fooType.id()).size());
    }

    @Test
    void typeReference_localVariableShadowsType_isNotRegistered() {
        String source = "class Foo {}\n" +
                "class Bar {\n" +
                "    void m() {\n" +
                "        int Foo = 1;\n" +
                "        Foo value;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol fooType = findSymbol(table, "Foo", SymbolKind.TYPE);
        List<SymbolReference> refs = table.referencesTo(fooType.id());

        assertEquals(0, refs.size());
    }

    @Test
    void typeReference_integrationWithRealTypeSymbol() {
        String source = "class Foo {\n" +
                "    static Foo instance() { return null; }\n" +
                "}\n" +
                "class Bar {\n" +
                "    Foo value = Foo.instance();\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol fooType = findSymbol(table, "Foo", SymbolKind.TYPE);
        List<SymbolReference> typeRefs = table.referencesTo(fooType.id());

        assertTrue(typeRefs.size() >= 2);
    }

    @Test
    void typeReference_disabledWhenSourceNotProvided() {
        String source = "class Foo {}\n" +
                "class Bar {\n" +
                "    Foo value;\n" +
                "}\n";
        SymbolTable table = tableOfNoSource(source);

        Symbol fooType = findSymbol(table, "Foo", SymbolKind.TYPE);
        List<SymbolReference> refs = table.referencesTo(fooType.id());

        assertEquals(0, refs.size());
    }

    @Test
    void typeReference_interfaceKind_isResolved() {
        String source = "interface Foo {}\n" +
                "class Bar {\n" +
                "    Foo value;\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        Symbol fooType = findSymbol(table, "Foo", SymbolKind.INTERFACE);
        List<SymbolReference> refs = table.referencesTo(fooType.id());

        assertEquals(1, refs.size());
    }

    @Test
    void typeReference_primitiveKeyword_isNotIndexed() {
        String source = "class Bar {\n" +
                "    int value;\n" +
                "    void m() {\n" +
                "        int local;\n" +
                "    }\n" +
                "}\n";
        SymbolTable table = tableOf(source);

        int totalTypeRefs = 0;
        for (SymbolScope scope : allScopes(table)) {
            for (Symbol s : table.symbolsIn(scope.id())) {
                totalTypeRefs += table.referencesTo(s.id()).size();
            }
        }
        assertEquals(0, totalTypeRefs);
    }
}
