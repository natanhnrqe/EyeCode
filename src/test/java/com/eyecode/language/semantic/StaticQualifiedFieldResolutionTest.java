package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.symbol.Symbol;
import com.eyecode.language.symbol.SymbolKind;
import com.eyecode.language.symbol.SymbolReference;
import com.eyecode.language.symbol.SymbolReferenceKind;
import com.eyecode.language.symbol.SymbolModifier;
import com.eyecode.language.symbol.SymbolScope;
import com.eyecode.language.symbol.SymbolTable;
import com.eyecode.language.symbol.SymbolTableBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticQualifiedFieldResolutionTest {

    private SymbolTable tableOf(String source) {
        JavaLexerService lexer = new JavaLexerService();
        JavaTokenStream stream = new JavaTokenStream(
                lexer.lex(DocumentSnapshot.oneShot(source)).tokens(), source);
        JavaFileModel model = new JavaParser(stream).parse();
        return new SymbolTableBuilder(model, 1, "Test.java", source).build().symbolTable();
    }

    private List<SymbolReference> allReferences(SymbolTable table) {
        List<SymbolReference> result = new ArrayList<>();
        for (SymbolScope scope : allScopes(table)) {
            for (Symbol symbol : table.symbolsIn(scope.id())) {
                result.addAll(table.referencesTo(symbol.id()));
            }
        }
        return result;
    }

    private List<SymbolScope> allScopes(SymbolTable table) {
        List<SymbolScope> result = new ArrayList<>();
        collectScopes(table.rootScope(), result);
        return result;
    }

    private void collectScopes(SymbolScope scope, List<SymbolScope> result) {
        result.add(scope);
        for (SymbolScope child : scope.children()) {
            collectScopes(child, result);
        }
    }

    private Symbol field(SymbolTable table, String name) {
        return allScopes(table).stream()
                .flatMap(scope -> table.symbolsIn(scope.id()).stream())
                .filter(symbol -> symbol.kind() == SymbolKind.FIELD && symbol.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void staticFieldReference_resolvesWithStaticMetadata() {
        String source = "class Constants { static int MAX = 10; } class Example { void test() { int x = Constants.MAX; } }";
        SymbolTable table = tableOf(source);
        Symbol max = field(table, "MAX");
        assertEquals(Set.of(SymbolModifier.STATIC), max.modifiers());
        assertEquals(1, table.referencesTo(max.id()).size());
    }

    @Test
    void staticFieldRead_resolvesTarget() {
        String source = "class Constants { static int MAX = 10; } class Example { void test() { int x = Constants.MAX; } }";
        SymbolTable table = tableOf(source);
        assertEquals(1, table.referencesTo(field(table, "MAX").id()).size());
    }

    @Test
    void staticFieldAssignment_resolvesTarget() {
        String source = "class Constants { static int MAX = 10; } class Example { void test() { Constants.MAX = 11; } }";
        SymbolTable table = tableOf(source);
        assertEquals(1, table.referencesTo(field(table, "MAX").id()).size());
    }

    @Test
    void multipleStaticFieldOccurrences_resolve() {
        String source = "class Constants { static int MAX = 10; } class Example { void test() { int a = Constants.MAX; int b = Constants.MAX; } }";
        SymbolTable table = tableOf(source);
        assertEquals(2, table.referencesTo(field(table, "MAX").id()).size());
    }

    @Test
    void unknownStaticField_remainsUnresolved() {
        String source = "class Constants { static int MAX = 10; } class Example { int x = Constants.UNKNOWN; }";
        assertTrue(allReferences(tableOf(source)).isEmpty());
    }

    @Test
    void instanceFieldAlsoRemainsUnresolved() {
        String source = "class Constants { int MAX; } class Example { int x = Constants.MAX; }";
        assertTrue(allReferences(tableOf(source)).isEmpty());
    }

    @Test
    void unknownTypeRemainsUnresolved() {
        String source = "class Example { int x = Missing.MAX; }";
        assertTrue(allReferences(tableOf(source)).isEmpty());
    }

    @Test
    void localVariableWithTypeNameDoesNotResolveAsType() {
        String source = "class Constants { static int MAX = 10; } class Example { void test() { int Constants = 0; int x = Constants.MAX; } }";
        assertTrue(allReferences(tableOf(source)).stream().noneMatch(ref -> ref.name().equals("Constants.MAX")));
    }

    @Test
    void objectFieldStillRemainsUnresolved() {
        String source = "class Example { void test(Config config) { int x = config.DEFAULT_PORT; } }";
        assertTrue(allReferences(tableOf(source)).isEmpty());
    }

    @Test
    void thisFieldKeepsExistingSimpleResolution() {
        String source = "class Example { int value; void test() { this.value = 1; } }";
        SymbolTable table = tableOf(source);
        List<SymbolReference> references = table.referencesTo(field(table, "value").id());
        assertEquals(1, references.size());
        assertEquals(SymbolReferenceKind.SIMPLE, references.get(0).kind());
    }

    @Test
    void superFieldStillRemainsUnresolved() {
        String source = "class Parent { int value; } class Child extends Parent { void test() { super.value = 1; } }";
        assertTrue(allReferences(tableOf(source)).isEmpty());
    }

    @Test
    void qualifiedReferenceShapeRemainsCompleteAndQualified() {
        SymbolReference reference = SymbolReference.qualified(
                "Constants.MAX_VALUE", 1, com.eyecode.editor.intelligence.document.TextRange.of(4, 23));
        assertEquals(SymbolReferenceKind.QUALIFIED_NAME, reference.kind());
        assertEquals("Constants.MAX_VALUE", reference.name());
        assertEquals(com.eyecode.editor.intelligence.document.TextRange.of(4, 23), reference.range());
        assertNull(reference.target());
    }

    @Test
    void referenceFinderDoesNotReturnUnresolvedStaticField() {
        String source = "class Constants { static int MAX = 10; } class Example { void test() { int x = Constants.MAX; } }";
        SymbolTable table = tableOf(source);
        Symbol target = field(table, "MAX");
        assertEquals(1, new JavaReferenceFinder().findReferences(target, table).size());
    }

    @Test
    void repeatedQueriesDoNotMutateSymbolTable() {
        String source = "class Constants { static int MAX = 10; } class Example { void test() { int x = Constants.MAX; } }";
        SymbolTable table = tableOf(source);
        Symbol target = field(table, "MAX");
        int scopeCount = allScopes(table).size();
        int symbolCount = allScopes(table).stream().mapToInt(scope -> table.symbolsIn(scope.id()).size()).sum();

        new JavaReferenceFinder().findReferences(target, table);
        new JavaReferenceFinder().findReferences(target, table);

        assertEquals(scopeCount, allScopes(table).size());
        assertEquals(symbolCount,
                allScopes(table).stream().mapToInt(scope -> table.symbolsIn(scope.id()).size()).sum());
        assertEquals(1, table.referencesTo(target.id()).size());
    }

    @Test
    void repeatedBuildsAreDeterministic() {
        String source = "class Constants { static int MAX = 10; } class Example { void test() { int x = Constants.MAX; } }";
        List<String> first = allReferences(tableOf(source)).stream()
                .map(ref -> ref.name() + ":" + ref.range() + ":" + ref.kind()).toList();
        List<String> second = allReferences(tableOf(source)).stream()
                .map(ref -> ref.name() + ":" + ref.range() + ":" + ref.kind()).toList();
        assertEquals(first, second);
    }

    @Test
    void qualifiedResolutionExposesQualifierAndTerminal() {
        String source = "class Constants { static int MAX; } class Use { void test() { int x = Constants.MAX; } }";
        SymbolTable table = tableOf(source);
        int start = source.indexOf("Constants.MAX");
        SymbolReference reference = SymbolReference.qualified(
                "Constants.MAX", scopeAt(table, start).id(), TextRange.of(start, start + 13));

        QualifiedReferenceResolution resolution = new QualifiedReferenceResolver().resolve(
                reference, scopeAt(table, start), new ScopeBasedQualifiedMemberLookup(table));

        assertTrue(resolution.isResolved());
        assertEquals(reference, resolution.reference());
        assertEquals("Constants", resolution.qualifierSymbol().orElseThrow().name());
        assertEquals(field(table, "MAX"), resolution.resolvedSymbol().orElseThrow());
    }

    @Test
    void indexedReferencePreservesExactQualifiedRange() {
        String source = "class Constants { static int MAX; } class Use { void test() { int x = Constants.MAX; } }";
        SymbolTable table = tableOf(source);
        SymbolReference reference = table.referencesTo(field(table, "MAX").id()).get(0);
        int start = source.indexOf("Constants.MAX");

        assertEquals(TextRange.of(start, start + "Constants.MAX".length()), reference.range());
        assertEquals("Constants.MAX", source.substring(reference.range().startOffset(), reference.range().endOffset()));
        assertEquals(SymbolReferenceKind.QUALIFIED_NAME, reference.kind());
    }

    @Test
    void indexedReferenceTargetsExactFieldIdentity() {
        String source = "class Constants { static int MAX; } class Use { void test() { int x = Constants.MAX; } }";
        SymbolTable table = tableOf(source);
        Symbol target = field(table, "MAX");

        assertEquals(target.id(), table.referencesTo(target.id()).get(0).target());
    }

    @Test
    void interfaceFieldResolvesAsImplicitlyStatic() {
        String source = "interface Config { int PORT = 80; } class Use { void test() { int x = Config.PORT; } }";
        SymbolTable table = tableOf(source);

        assertEquals(1, table.referencesTo(field(table, "PORT").id()).size());
    }

    @Test
    void nestedTypeStaticFieldResolvesThroughFullChain() {
        String source = "class Outer { static class Constants { static int MAX; } } class Use { void test() { int x = Outer.Constants.MAX; } }";
        SymbolTable table = tableOf(source);
        Symbol max = field(table, "MAX");

        assertEquals(1, table.referencesTo(max.id()).size());
        assertEquals("Outer.Constants.MAX", table.referencesTo(max.id()).get(0).name());
    }

    @Test
    void nonStaticFieldDirectResolutionIsUnresolved() {
        String source = "class Constants { int MAX; } class Use { void test() { int x = Constants.MAX; } }";
        SymbolTable table = tableOf(source);
        int start = source.indexOf("Constants.MAX");
        SymbolScope scope = scopeAt(table, start);
        SymbolReference reference = SymbolReference.qualified(
                "Constants.MAX", scope.id(), TextRange.of(start, start + 13));

        assertFalse(new QualifiedReferenceResolver().resolve(
                reference, scope, new ScopeBasedQualifiedMemberLookup(table),
                QualifiedMemberExpectation.STATIC_FIELD).isResolved());
        assertTrue(table.referencesTo(field(table, "MAX").id()).isEmpty());
    }

    @Test
    void definitionResolverReturnsStaticFieldDeclaration() {
        String source = "class Constants { static int MAX; } class Use { void test() { int x = Constants.MAX; } }";
        SymbolTable table = tableOf(source);
        Symbol target = field(table, "MAX");
        SymbolReference reference = table.referencesTo(target.id()).get(0);

        DefinitionLocation location = new JavaDefinitionResolver().resolve(reference, table).orElseThrow();

        assertEquals(target, location.symbol());
        assertEquals(target.declarationRange(), location.declarationRange());
    }

    @Test
    void referenceFinderReturnsAllExactLocationsWithoutDuplicates() {
        String source = "class Constants { static int MAX; } class Use { void test() { int a = Constants.MAX; int b = Constants.MAX; } }";
        SymbolTable table = tableOf(source);
        List<ReferenceLocation> locations = new JavaReferenceFinder().findReferences(field(table, "MAX"), table);

        assertEquals(2, locations.size());
        assertEquals(2, locations.stream().map(ReferenceLocation::range).distinct().count());
    }

    private SymbolScope scopeAt(SymbolTable table, int offset) {
        return allScopes(table).stream()
                .filter(scope -> scope == table.rootScope()
                        || scope.range().startOffset() <= offset && offset <= scope.range().endOffset())
                .max(java.util.Comparator.comparingInt(this::depth))
                .orElse(table.rootScope());
    }

    private int depth(SymbolScope scope) {
        int depth = 0;
        while (scope.parent().isPresent()) {
            depth++;
            scope = scope.parent().orElseThrow();
        }
        return depth;
    }
}
