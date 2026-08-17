package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.LexerSnapshot;
import com.eyecode.language.Token;
import com.eyecode.language.symbol.ProjectSymbolTable;
import com.eyecode.language.symbol.SemanticModelSnapshot;
import com.eyecode.language.symbol.Symbol;
import com.eyecode.language.symbol.SymbolId;
import com.eyecode.language.symbol.SymbolKind;
import com.eyecode.language.symbol.SymbolReference;
import com.eyecode.language.symbol.SymbolReferenceKind;
import com.eyecode.language.symbol.SymbolScope;
import com.eyecode.language.symbol.SymbolTable;
import com.eyecode.language.symbol.SymbolTableBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 5.4d.1 — JavaReferenceFinder Core query tests.
 * <p>
 * Tests validate the finder's contract against an in-test {@link ProjectSymbolTable}
 * populated manually via {@code addReference}. The production
 * {@code SymbolTableBuilder} does not yet call {@code addReference} — that
 * population is deferred to 5.4d.2 (documented limitation).
 */
class JavaReferenceFinderTest {

    private static Symbol makeSymbol(String name, SymbolKind kind, long ownerScopeId,
                                     TextRange declarationRange) {
        SymbolId id = SymbolId.of(ownerScopeId, declarationRange, kind);
        return new Symbol(id, kind, name, declarationRange, ownerScopeId, ownerScopeId, name);
    }

    private static ReferenceLocation asRef(SymbolReference ref) {
        return ReferenceLocation.of(ref);
    }

    @Test
    void singleReference_isReturned() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        Symbol field = makeSymbol("value", SymbolKind.FIELD, 1L, TextRange.of(10, 15));
        table.declareSymbol(table.rootScope(), field);
        SymbolReference ref = new SymbolReference(
                field.id(), TextRange.of(50, 55), "value", 2L, SymbolReferenceKind.SIMPLE_NAME);
        table.addReference(ref);

        List<ReferenceLocation> result = new JavaReferenceFinder().findReferences(field, table);
        assertEquals(List.of(asRef(ref)), result);
    }

    @Test
    void multipleReferences_areAllReturned() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        Symbol field = makeSymbol("value", SymbolKind.FIELD, 1L, TextRange.of(10, 15));
        table.declareSymbol(table.rootScope(), field);
        SymbolReference r1 = new SymbolReference(
                field.id(), TextRange.of(50, 55), "value", 2L, SymbolReferenceKind.SIMPLE_NAME);
        SymbolReference r2 = new SymbolReference(
                field.id(), TextRange.of(70, 75), "value", 2L, SymbolReferenceKind.SIMPLE_NAME);
        SymbolReference r3 = new SymbolReference(
                field.id(), TextRange.of(90, 95), "value", 2L, SymbolReferenceKind.SIMPLE_NAME);
        table.addReference(r1);
        table.addReference(r2);
        table.addReference(r3);

        List<ReferenceLocation> result = new JavaReferenceFinder().findReferences(field, table);
        assertEquals(3, result.size());
    }

    @Test
    void noReferences_returnsEmptyList_notNull() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        Symbol field = makeSymbol("unused", SymbolKind.FIELD, 1L, TextRange.of(10, 16));
        table.declareSymbol(table.rootScope(), field);

        List<ReferenceLocation> result = new JavaReferenceFinder().findReferences(field, table);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shadowing_fieldAndLocalWithSameName_areDistinguished() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        TextRange fieldRange = TextRange.of(10, 15);
        TextRange localRange = TextRange.of(50, 55);
        SymbolScope fieldScope = table.createChildScope(table.rootScope(),
                com.eyecode.language.symbol.ScopeKind.TYPE, TextRange.of(0, 100));
        SymbolScope localScope = table.createChildScope(fieldScope,
                com.eyecode.language.symbol.ScopeKind.BLOCK, TextRange.of(40, 60));
        Symbol field = makeSymbol("value", SymbolKind.FIELD, fieldScope.id(), fieldRange);
        Symbol local = makeSymbol("value", SymbolKind.LOCAL_VARIABLE, localScope.id(), localRange);
        table.declareSymbol(fieldScope, field);
        table.declareSymbol(localScope, local);

        SymbolReference refToField = new SymbolReference(
                field.id(), TextRange.of(100, 105), "value", localScope.id(), SymbolReferenceKind.SIMPLE_NAME);
        SymbolReference refToLocal = new SymbolReference(
                local.id(), TextRange.of(110, 115), "value", localScope.id(), SymbolReferenceKind.SIMPLE_NAME);
        table.addReference(refToField);
        table.addReference(refToLocal);

        JavaReferenceFinder finder = new JavaReferenceFinder();

        List<ReferenceLocation> fieldRefs = finder.findReferences(field, table);
        assertEquals(List.of(asRef(refToField)), fieldRefs);

        List<ReferenceLocation> localRefs = finder.findReferences(local, table);
        assertEquals(List.of(asRef(refToLocal)), localRefs);
    }

    @Test
    void twoSymbolsWithSameName_butDifferentOwners_eachSeesTheirOwn() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        SymbolScope outerScope = table.createChildScope(table.rootScope(),
                com.eyecode.language.symbol.ScopeKind.TYPE, TextRange.of(0, 100));
        SymbolScope innerScope = table.createChildScope(table.rootScope(),
                com.eyecode.language.symbol.ScopeKind.TYPE, TextRange.of(200, 300));
        Symbol outer = makeSymbol("counter", SymbolKind.FIELD, outerScope.id(), TextRange.of(10, 17));
        Symbol inner = makeSymbol("counter", SymbolKind.FIELD, innerScope.id(), TextRange.of(50, 57));
        table.declareSymbol(outerScope, outer);
        table.declareSymbol(innerScope, inner);

        SymbolReference refOuter = new SymbolReference(
                outer.id(), TextRange.of(100, 107), "counter", outerScope.id(), SymbolReferenceKind.SIMPLE_NAME);
        SymbolReference refInner = new SymbolReference(
                inner.id(), TextRange.of(110, 117), "counter", innerScope.id(), SymbolReferenceKind.SIMPLE_NAME);
        table.addReference(refOuter);
        table.addReference(refInner);

        JavaReferenceFinder finder = new JavaReferenceFinder();

        List<ReferenceLocation> outerRefs = finder.findReferences(outer, table);
        assertEquals(1, outerRefs.size());
        assertEquals(refOuter.range(), outerRefs.get(0).range());

        List<ReferenceLocation> innerRefs = finder.findReferences(inner, table);
        assertEquals(1, innerRefs.size());
        assertEquals(refInner.range(), innerRefs.get(0).range());
    }

    @Test
    void fieldReference_isReturned() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        Symbol field = makeSymbol("counter", SymbolKind.FIELD, 1L, TextRange.of(10, 17));
        table.declareSymbol(table.rootScope(), field);
        SymbolReference ref = new SymbolReference(
                field.id(), TextRange.of(50, 57), "counter", 2L, SymbolReferenceKind.SIMPLE_NAME);
        table.addReference(ref);

        List<ReferenceLocation> result = new JavaReferenceFinder().findReferences(field, table);
        assertEquals(List.of(asRef(ref)), result);
    }

    @Test
    void localVariableReference_isReturned() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        Symbol local = makeSymbol("x", SymbolKind.LOCAL_VARIABLE, 2L, TextRange.of(20, 21));
        table.declareSymbol(table.rootScope(), local);
        SymbolReference ref = new SymbolReference(
                local.id(), TextRange.of(60, 61), "x", 2L, SymbolReferenceKind.SIMPLE_NAME);
        table.addReference(ref);

        List<ReferenceLocation> result = new JavaReferenceFinder().findReferences(local, table);
        assertEquals(List.of(asRef(ref)), result);
    }

    @Test
    void parameterReference_isReturned() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        Symbol param = makeSymbol("value", SymbolKind.PARAMETER, 3L, TextRange.of(20, 25));
        table.declareSymbol(table.rootScope(), param);
        SymbolReference ref = new SymbolReference(
                param.id(), TextRange.of(80, 85), "value", 3L, SymbolReferenceKind.SIMPLE_NAME);
        table.addReference(ref);

        List<ReferenceLocation> result = new JavaReferenceFinder().findReferences(param, table);
        assertEquals(List.of(asRef(ref)), result);
    }

    @Test
    void methodReference_isReturned() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        Symbol method = makeSymbol("run", SymbolKind.METHOD, 1L, TextRange.of(20, 23));
        table.declareSymbol(table.rootScope(), method);
        SymbolReference ref = new SymbolReference(
                method.id(), TextRange.of(80, 83), "run", 2L, SymbolReferenceKind.SIMPLE_NAME);
        table.addReference(ref);

        List<ReferenceLocation> result = new JavaReferenceFinder().findReferences(method, table);
        assertEquals(List.of(asRef(ref)), result);
    }

    @Test
    void typeReference_isReturned() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        Symbol type = makeSymbol("Helper", SymbolKind.TYPE, 0L, TextRange.of(0, 13));
        table.declareSymbol(table.rootScope(), type);
        SymbolReference ref = new SymbolReference(
                type.id(), TextRange.of(80, 86), "Helper", 1L, SymbolReferenceKind.SIMPLE_NAME);
        table.addReference(ref);

        List<ReferenceLocation> result = new JavaReferenceFinder().findReferences(type, table);
        assertEquals(List.of(asRef(ref)), result);
    }

    @Test
    void qualifiedReference_isReturned() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        Symbol field = makeSymbol("value", SymbolKind.FIELD, 1L, TextRange.of(10, 15));
        table.declareSymbol(table.rootScope(), field);
        SymbolReference ref = SymbolReference.qualified("h.value", 2L, TextRange.of(80, 87));
        SymbolReference bound = new SymbolReference(
                field.id(), ref.range(), ref.name(), ref.scopeId(), SymbolReferenceKind.QUALIFIED_NAME);
        table.addReference(bound);

        List<ReferenceLocation> result = new JavaReferenceFinder().findReferences(field, table);
        assertEquals(1, result.size());
        assertEquals(SymbolReferenceKind.QUALIFIED_NAME, result.get(0).reference().kind());
    }

    @Test
    void duplicateReference_isDeduped() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        Symbol field = makeSymbol("value", SymbolKind.FIELD, 1L, TextRange.of(10, 15));
        table.declareSymbol(table.rootScope(), field);
        SymbolReference ref = new SymbolReference(
                field.id(), TextRange.of(50, 55), "value", 2L, SymbolReferenceKind.SIMPLE_NAME);
        table.addReference(ref);
        table.addReference(ref);
        table.addReference(ref);

        List<ReferenceLocation> result = new JavaReferenceFinder().findReferences(field, table);
        assertEquals(1, result.size());
    }

    @Test
    void result_isDeterministicByRange() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        Symbol field = makeSymbol("value", SymbolKind.FIELD, 1L, TextRange.of(10, 15));
        table.declareSymbol(table.rootScope(), field);
        SymbolReference r1 = new SymbolReference(
                field.id(), TextRange.of(50, 55), "value", 2L, SymbolReferenceKind.SIMPLE_NAME);
        SymbolReference r2 = new SymbolReference(
                field.id(), TextRange.of(20, 25), "value", 2L, SymbolReferenceKind.SIMPLE_NAME);
        SymbolReference r3 = new SymbolReference(
                field.id(), TextRange.of(80, 85), "value", 2L, SymbolReferenceKind.SIMPLE_NAME);
        table.addReference(r1);
        table.addReference(r2);
        table.addReference(r3);

        JavaReferenceFinder finder = new JavaReferenceFinder();
        List<ReferenceLocation> first = finder.findReferences(field, table);
        List<ReferenceLocation> second = finder.findReferences(field, table);

        assertEquals(first, second);
        assertEquals(TextRange.of(20, 25), first.get(0).range());
        assertEquals(TextRange.of(50, 55), first.get(1).range());
        assertEquals(TextRange.of(80, 85), first.get(2).range());
    }

    @Test
    void symbolTable_isNotMutated() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        Symbol field = makeSymbol("value", SymbolKind.FIELD, 1L, TextRange.of(10, 15));
        table.declareSymbol(table.rootScope(), field);
        SymbolReference ref = new SymbolReference(
                field.id(), TextRange.of(50, 55), "value", 2L, SymbolReferenceKind.SIMPLE_NAME);
        table.addReference(ref);

        long rootIdBefore = table.rootScope().id();
        long fieldIdBefore = field.id().hashCode();
        int refsBefore = table.referencesTo(field.id()).size();
        boolean fieldFoundBefore = table.find(field.id()).isPresent();

        new JavaReferenceFinder().findReferences(field, table);

        long rootIdAfter = table.rootScope().id();
        long fieldIdAfter = field.id().hashCode();
        int refsAfter = table.referencesTo(field.id()).size();
        boolean fieldFoundAfter = table.find(field.id()).isPresent();

        assertEquals(rootIdBefore, rootIdAfter);
        assertEquals(fieldIdBefore, fieldIdAfter);
        assertEquals(refsBefore, refsAfter);
        assertEquals(fieldFoundBefore, fieldFoundAfter);
    }

    @Test
    void nullSymbol_rejected() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        ReferenceFinder finder = new JavaReferenceFinder();
        assertThrows(NullPointerException.class, () -> finder.findReferences(null, table));
    }

    @Test
    void nullTable_rejected() {
        Symbol field = makeSymbol("value", SymbolKind.FIELD, 1L, TextRange.of(10, 15));
        ReferenceFinder finder = new JavaReferenceFinder();
        assertThrows(NullPointerException.class, () -> finder.findReferences(field, null));
    }

    @Test
    void referencesToOtherSymbol_areFiltered_out() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        Symbol fieldA = makeSymbol("alpha", SymbolKind.FIELD, 1L, TextRange.of(10, 15));
        Symbol fieldB = makeSymbol("beta", SymbolKind.FIELD, 1L, TextRange.of(20, 24));
        table.declareSymbol(table.rootScope(), fieldA);
        table.declareSymbol(table.rootScope(), fieldB);

        SymbolReference refToA = new SymbolReference(
                fieldA.id(), TextRange.of(50, 55), "alpha", 2L, SymbolReferenceKind.SIMPLE_NAME);
        SymbolReference refToB = new SymbolReference(
                fieldB.id(), TextRange.of(60, 64), "beta", 2L, SymbolReferenceKind.SIMPLE_NAME);
        table.addReference(refToA);
        table.addReference(refToB);

        List<ReferenceLocation> result = new JavaReferenceFinder().findReferences(fieldA, table);
        assertEquals(1, result.size());
        assertEquals(refToA.range(), result.get(0).range());
        assertFalse(result.stream().anyMatch(loc ->
                loc.reference().target().equals(fieldB.id())));
    }

    @Test
    void goldenEndToEnd_findsAllReferencesFromFixture() {
        String source = "class C {\n" +
                "    int value = 1;\n" +
                "    void m() {\n" +
                "        int local = value;\n" +
                "        value = local + 2;\n" +
                "    }\n" +
                "}\n";
        DocumentSnapshot snapshot = DocumentSnapshot.oneShot(source);
        LexerSnapshot lex = new JavaLexerService().lex(snapshot);
        List<Token> tokens = lex.tokens();
        JavaTokenStream stream = new JavaTokenStream(tokens, source);
        JavaParser parser = new JavaParser(stream);
        JavaFileModel model = parser.parse();
        SymbolTableBuilder builder = new SymbolTableBuilder(model, 1, "Golden.java");
        SemanticModelSnapshot semantic = builder.build();
        SymbolTable table = semantic.symbolTable();

        Symbol field = findSymbolByName(table, "value", SymbolKind.FIELD);

        List<ReferenceLocation> result = new JavaReferenceFinder().findReferences(field, table);
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    private static Symbol findSymbolByName(SymbolTable table, String name, SymbolKind kind) {
        for (SymbolScope scope : allScopes(table)) {
            for (Symbol s : table.symbolsIn(scope.id())) {
                if (s.name().equals(name) && s.kind() == kind) {
                    return s;
                }
            }
        }
        throw new IllegalStateException("No symbol found: " + kind + " " + name);
    }

    private static List<SymbolScope> allScopes(SymbolTable table) {
        List<SymbolScope> out = new java.util.ArrayList<>();
        collectScopes(table.rootScope(), out);
        return out;
    }

    private static void collectScopes(SymbolScope scope, List<SymbolScope> out) {
        out.add(scope);
        for (SymbolScope child : scope.children()) {
            collectScopes(child, out);
        }
    }

    @Test
    void unresolvedReference_isIgnored() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        Symbol field = makeSymbol("value", SymbolKind.FIELD, 1L, TextRange.of(10, 15));
        table.declareSymbol(table.rootScope(), field);
        SymbolReference unresolved = SymbolReference.simple("value", 2L, TextRange.of(50, 55));
        SymbolReference bound = new SymbolReference(
                field.id(), TextRange.of(60, 65), "value", 2L, SymbolReferenceKind.SIMPLE_NAME);
        table.addReference(bound);

        List<ReferenceLocation> result = new JavaReferenceFinder().findReferences(field, table);
        assertEquals(1, result.size());
        assertEquals(TextRange.of(60, 65), result.get(0).range());
        Optional<SymbolReference> found = result.stream()
                .map(ReferenceLocation::reference)
                .filter(r -> r.target() == null)
                .findFirst();
        assertTrue(found.isEmpty());
    }
}
