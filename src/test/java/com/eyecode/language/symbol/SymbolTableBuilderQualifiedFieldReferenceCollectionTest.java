package com.eyecode.language.symbol;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.ast.AstNodeKind;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.semantic.JavaReferenceFinder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SymbolTableBuilderQualifiedFieldReferenceCollectionTest {

    private JavaFileModel modelOf(String source) {
        JavaLexerService lexer = new JavaLexerService();
        JavaTokenStream stream = new JavaTokenStream(
                lexer.lex(DocumentSnapshot.oneShot(source)).tokens(), source);
        return new JavaParser(stream).parse();
    }

    private SymbolTable tableOf(String source) {
        return new SymbolTableBuilder(modelOf(source), 1, "Test.java", source)
                .build().symbolTable();
    }

    private List<AstNode> fieldAccesses(AstNode root) {
        List<AstNode> result = new ArrayList<>();
        collectFieldAccesses(root, result);
        return result;
    }

    private void collectFieldAccesses(AstNode node, List<AstNode> result) {
        if (node.kind() == AstNodeKind.FIELD_ACCESS_EXPRESSION) {
            result.add(node);
        }
        for (AstNode child : node.children()) {
            collectFieldAccesses(child, result);
        }
    }

    private AstNode qualifiedAccess(String source, String text) {
        return fieldAccesses(modelOf(source).getAstRoot()).stream()
                .filter(node -> source.substring(node.range().startOffset(), node.range().endOffset())
                        .equals(text))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void objectField_read_hasCompleteStructuralRange() {
        String source = "class Example { void test(User user) { String x = user.name; } }";
        AstNode access = qualifiedAccess(source, "user.name");
        assertEquals(AstNodeKind.NAME_EXPRESSION, access.children().get(0).kind());
        assertEquals("user.name", source.substring(access.range().startOffset(), access.range().endOffset()));
    }

    @Test
    void objectField_assignment_hasCompleteStructuralRange() {
        String source = "class Example { void test(User user) { user.name = \"A\"; } }";
        AstNode access = qualifiedAccess(source, "user.name");
        assertEquals(TextRange.of(source.indexOf("user.name"), source.indexOf("user.name") + 9), access.range());
    }

    @Test
    void chainedFieldAccess_preservesAllComponents() {
        String source = "class Example { void test(Account account) { String x = account.owner.name; } }";
        AstNode access = qualifiedAccess(source, "account.owner.name");
        assertEquals("account.owner.name", source.substring(access.range().startOffset(), access.range().endOffset()));
        assertEquals(AstNodeKind.FIELD_ACCESS_EXPRESSION, access.children().get(0).kind());
    }

    @Test
    void unknownQualifiedField_doesNotFabricateSymbol() {
        String source = "class Example { void test(User user) { user.unknown = 1; } }";
        SymbolTable table = tableOf(source);
        assertTrue(allReferences(table).isEmpty());
    }

    @Test
    void qualifiedField_collectionDoesNotResolveReceiverType() {
        String source = "class Example { void test(User user) { user.name = \"A\"; String x = user.name; } }";
        SymbolTable table = tableOf(source);
        assertTrue(allReferences(table).isEmpty());
        assertFalse(source.contains("class User"));
    }

    @Test
    void thisField_remainsSimpleAndIndexedOnce() {
        String source = "class Example { int name; void test() { this.name = 1; } }";
        SymbolTable table = tableOf(source);
        Symbol field = findSymbol(table, "name", SymbolKind.FIELD);
        assertEquals(1, table.referencesTo(field.id()).size());
        assertEquals(SymbolReferenceKind.SIMPLE, table.referencesTo(field.id()).get(0).kind());
    }

    @Test
    void superField_remainsUnindexed() {
        String source = "class Parent { int name; } class Child extends Parent { void test() { super.name = 1; } }";
        assertTrue(allReferences(tableOf(source)).isEmpty());
    }

    @Test
    void simpleName_remainsSimple() {
        String source = "class Example { int name; void test() { name = 1; } }";
        SymbolTable table = tableOf(source);
        Symbol field = findSymbol(table, "name", SymbolKind.FIELD);
        assertEquals(SymbolReferenceKind.SIMPLE, table.referencesTo(field.id()).get(0).kind());
    }

    @Test
    void qualifiedFactory_preservesTextRangeAndKind() {
        SymbolReference reference = SymbolReference.qualified(
                "user.name", 7, TextRange.of(20, 29));
        assertEquals(SymbolReferenceKind.QUALIFIED_NAME, reference.kind());
        assertEquals("user.name", reference.name());
        assertEquals(TextRange.of(20, 29), reference.range());
    }

    @Test
    void repeatedQualifiedAccesses_haveDeterministicAstRanges() {
        String source = "class Example { void test(User user) { user.name = 1; String x = user.name; } }";
        List<AstNode> first = fieldAccesses(modelOf(source).getAstRoot());
        List<AstNode> second = fieldAccesses(modelOf(source).getAstRoot());
        assertEquals(first.stream().map(AstNode::range).toList(), second.stream().map(AstNode::range).toList());
    }

    @Test
    void referenceFinder_doesNotReturnUnresolvedQualifiedReferences() {
        String source = "class Example { void test(User user) { user.name = 1; } }";
        SymbolTable table = tableOf(source);
        assertTrue(allReferences(table).isEmpty());
        assertTrue(new JavaReferenceFinder().findReferences(
                findSymbol(table, "test", SymbolKind.METHOD), table).isEmpty());
    }

    @Test
    void buildingSameSourceDoesNotChangeIndexedReferences() {
        String source = "class Example { int value; void test(User user) { user.name = 1; this.value = 2; } }";
        SymbolTable first = tableOf(source);
        SymbolTable second = tableOf(source);
        assertEquals(normalizeReferences(first), normalizeReferences(second));
    }

    @Test
    void qualifiedNamesAreNotAddedAsSimpleTerminalReferences() {
        String source = "class Example { void test(User user) { user.name = 1; } }";
        SymbolTable table = tableOf(source);
        assertTrue(allReferences(table).stream().noneMatch(ref -> ref.name().equals("name")));
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

    private List<String> normalizeReferences(SymbolTable table) {
        return allReferences(table).stream()
                .map(reference -> reference.name() + "|"
                        + reference.range().startOffset() + ":"
                        + reference.range().endOffset() + "|"
                        + reference.kind())
                .toList();
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
}
