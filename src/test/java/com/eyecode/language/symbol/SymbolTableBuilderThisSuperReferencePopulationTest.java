package com.eyecode.language.symbol;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.semantic.JavaReferenceFinder;
import com.eyecode.language.semantic.ReferenceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SymbolTableBuilderThisSuperReferencePopulationTest {

    private SymbolTable tableOf(String source) {
        JavaLexerService lexer = new JavaLexerService();
        JavaTokenStream stream = new JavaTokenStream(
                lexer.lex(DocumentSnapshot.oneShot(source)).tokens(), source);
        JavaFileModel model = new JavaParser(stream).parse();
        return new SymbolTableBuilder(model, 1, "Test.java", source).build().symbolTable();
    }

    private Symbol findSymbol(SymbolTable table, String name, SymbolKind kind) {
        for (SymbolScope scope : allScopes(table)) {
            for (Symbol symbol : table.symbolsIn(scope.id())) {
                if (symbol.name().equals(name) && symbol.kind() == kind) {
                    return symbol;
                }
            }
        }
        throw new IllegalStateException("Missing symbol: " + kind + " " + name);
    }

    private List<SymbolScope> allScopes(SymbolTable table) {
        List<SymbolScope> scopes = new ArrayList<>();
        collectScopes(table.rootScope(), scopes);
        return scopes;
    }

    private void collectScopes(SymbolScope scope, List<SymbolScope> result) {
        result.add(scope);
        for (SymbolScope child : scope.children()) {
            collectScopes(child, result);
        }
    }

    @Test
    void thisField_assignment_isIndexed() {
        String source = "class Example { int value; void test() { this.value = 10; } }";
        SymbolTable table = tableOf(source);
        Symbol field = findSymbol(table, "value", SymbolKind.FIELD);
        assertEquals(1, table.referencesTo(field.id()).size());
    }

    @Test
    void thisField_read_isIndexed() {
        String source = "class Example { int value; void test() { int x = this.value; } }";
        SymbolTable table = tableOf(source);
        Symbol field = findSymbol(table, "value", SymbolKind.FIELD);
        assertEquals(1, table.referencesTo(field.id()).size());
    }

    @Test
    void multipleThisField_occurrences_areIndexed() {
        String source = "class Example { int value; void test() { this.value = 1; int x = this.value; } }";
        SymbolTable table = tableOf(source);
        Symbol field = findSymbol(table, "value", SymbolKind.FIELD);
        assertEquals(2, table.referencesTo(field.id()).size());
    }

    @Test
    void thisField_range_containsTerminalOnly() {
        String source = "class Example { int value; void test() { this.value = 1; } }";
        SymbolTable table = tableOf(source);
        Symbol field = findSymbol(table, "value", SymbolKind.FIELD);
        SymbolReference reference = table.referencesTo(field.id()).get(0);
        int terminal = source.indexOf("this.value") + "this.".length();
        assertEquals(terminal, reference.range().startOffset());
        assertEquals(terminal + "value".length(), reference.range().endOffset());
        assertEquals("value", reference.name());
    }

    @Test
    void thisField_referenceFinder_returnsTerminalReference() {
        String source = "class Example { int value; void test() { this.value = 1; } }";
        SymbolTable table = tableOf(source);
        Symbol field = findSymbol(table, "value", SymbolKind.FIELD);
        List<ReferenceLocation> references = new JavaReferenceFinder().findReferences(field, table);
        assertEquals(1, references.size());
        assertEquals("value", references.get(0).reference().name());
    }

    @Test
    void thisField_doesNotDuplicateNormalNameReference() {
        String source = "class Example { int value; void test() { this.value = 1; value = 2; } }";
        SymbolTable table = tableOf(source);
        Symbol field = findSymbol(table, "value", SymbolKind.FIELD);
        List<SymbolReference> references = table.referencesTo(field.id());
        assertEquals(2, references.size());
        Set<Integer> starts = new HashSet<>();
        for (SymbolReference reference : references) {
            starts.add(reference.range().startOffset());
        }
        assertEquals(2, starts.size());
    }

    @Test
    void thisField_localShadowing_doesNotChangeTarget() {
        String source = "class Example { int value; void test() { int value = 0; this.value = value; } }";
        SymbolTable table = tableOf(source);
        Symbol field = findSymbol(table, "value", SymbolKind.FIELD);
        Symbol local = findSymbol(table, "value", SymbolKind.LOCAL_VARIABLE);
        assertEquals(1, table.referencesTo(field.id()).size());
        assertEquals(1, table.referencesTo(local.id()).size());
    }

    @Test
    void thisUnknown_isNotIndexed() {
        String source = "class Example { void test() { this.unknown = 1; } }";
        SymbolTable table = tableOf(source);
        int references = 0;
        for (SymbolScope scope : allScopes(table)) {
            for (Symbol symbol : table.symbolsIn(scope.id())) {
                references += table.referencesTo(symbol.id()).size();
            }
        }
        assertEquals(0, references);
    }

    @Test
    void isolatedThis_isNotIndexed() {
        String source = "class Example { void test() { Object x = this; } }";
        SymbolTable table = tableOf(source);
        int references = 0;
        for (SymbolScope scope : allScopes(table)) {
            for (Symbol symbol : table.symbolsIn(scope.id())) {
                references += table.referencesTo(symbol.id()).size();
            }
        }
        assertEquals(0, references);
    }

    @Test
    void superField_isNotIndexed_withoutInheritanceResolution() {
        String source = "class Parent { int value; } class Child extends Parent { void test() { super.value = 1; } }";
        SymbolTable table = tableOf(source);
        Symbol parentField = findSymbol(table, "value", SymbolKind.FIELD);
        assertEquals(0, table.referencesTo(parentField.id()).size());
    }

    @Test
    void superUnknown_isNotIndexed() {
        String source = "class Child { void test() { super.unknown = 1; } }";
        SymbolTable table = tableOf(source);
        int references = 0;
        for (SymbolScope scope : allScopes(table)) {
            for (Symbol symbol : table.symbolsIn(scope.id())) {
                references += table.referencesTo(symbol.id()).size();
            }
        }
        assertEquals(0, references);
    }

    @Test
    void thisField_multipleFields_resolvesTerminalName() {
        String source = "class Example { int first; int second; void test() { this.first = this.second; } }";
        SymbolTable table = tableOf(source);
        Symbol first = findSymbol(table, "first", SymbolKind.FIELD);
        Symbol second = findSymbol(table, "second", SymbolKind.FIELD);
        assertEquals(1, table.referencesTo(first.id()).size());
        assertEquals(1, table.referencesTo(second.id()).size());
    }

    @Test
    void thisField_referencesAreDeterministic() {
        String source = "class Example { int value; void test() { this.value = 1; int x = this.value; } }";
        SymbolTable firstTable = tableOf(source);
        SymbolTable secondTable = tableOf(source);
        Symbol first = findSymbol(firstTable, "value", SymbolKind.FIELD);
        Symbol second = findSymbol(secondTable, "value", SymbolKind.FIELD);
        List<SymbolReference> firstRefs = firstTable.referencesTo(first.id());
        List<SymbolReference> secondRefs = secondTable.referencesTo(second.id());
        assertEquals(firstRefs.size(), secondRefs.size());
        for (int i = 0; i < firstRefs.size(); i++) {
            assertEquals(firstRefs.get(i).range(), secondRefs.get(i).range());
        }
    }

    @Test
    void thisField_symbolTableRemainsStableAfterQueries() {
        String source = "class Example { int value; void test() { this.value = 1; } }";
        SymbolTable table = tableOf(source);
        Symbol field = findSymbol(table, "value", SymbolKind.FIELD);
        SymbolId id = field.id();
        long ownerScope = field.ownerScopeId();
        int count = table.referencesTo(id).size();
        assertEquals(1, count);
        assertEquals(id, findSymbol(table, "value", SymbolKind.FIELD).id());
        assertEquals(ownerScope, findSymbol(table, "value", SymbolKind.FIELD).ownerScopeId());
        assertEquals(count, table.referencesTo(id).size());
    }

    @Test
    void thisField_kindIsSimpleAndTargetIsField() {
        String source = "class Example { int value; void test() { this.value = 1; } }";
        SymbolTable table = tableOf(source);
        Symbol field = findSymbol(table, "value", SymbolKind.FIELD);
        SymbolReference reference = table.referencesTo(field.id()).get(0);
        assertEquals(SymbolReferenceKind.SIMPLE, reference.kind());
        assertEquals(field.id(), reference.target());
        assertNotNull(reference.scopeId());
    }

    @Test
    void thisField_classWithoutFields_hasNoReferences() {
        String source = "class Example { void test() { this.unknown; } }";
        SymbolTable table = tableOf(source);
        assertTrue(table.symbolsIn(table.rootScope().id()).stream()
                .noneMatch(symbol -> !table.referencesTo(symbol.id()).isEmpty()));
    }
}
