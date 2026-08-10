package com.eyecode.language.java.parser;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.ast.AstNodeKind;
import com.eyecode.language.java.JavaLexerService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParserSnapshotTest {

    private static AstNode parseAst(String text) {
        JavaLexerService lexer = new JavaLexerService();
        var lex = lexer.lex(DocumentSnapshot.oneShot(text));
        JavaTokenStream stream = new JavaTokenStream(lex.tokens(), text);
        return new JavaParser(stream).parse().getAstRoot();
    }

    @Test
    void versionMatchesDocument() {
        AstNode root = AstNode.of(AstNodeKind.COMPILATION_UNIT, TextRange.of(0, 5), List.of());
        ParserSnapshot snapshot = new ParserSnapshot(42, "hello", root);
        assertEquals(42, snapshot.version());
    }

    @Test
    void astRootIsAccessible() {
        AstNode root = AstNode.of(AstNodeKind.COMPILATION_UNIT, TextRange.of(0, 5), List.of());
        ParserSnapshot snapshot = new ParserSnapshot(1, "hello", root);
        assertSame(root, snapshot.astRoot());
    }

    @Test
    void textIsAccessible() {
        AstNode root = AstNode.of(AstNodeKind.COMPILATION_UNIT, TextRange.of(0, 5), List.of());
        ParserSnapshot snapshot = new ParserSnapshot(1, "hello", root);
        assertEquals("hello", snapshot.text());
    }

    @Test
    void nullTextRejected() {
        AstNode root = AstNode.of(AstNodeKind.COMPILATION_UNIT, TextRange.of(0, 5), List.of());
        assertThrows(IllegalArgumentException.class,
                () -> new ParserSnapshot(1, null, root));
    }

    @Test
    void nullAstRootRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ParserSnapshot(1, "x", null));
    }

    @Test
    void ofFactoryProducesSameContents() {
        DocumentSnapshot doc = new DocumentSnapshot(7, "class A {}", null, null, "s1");
        AstNode root = parseAst("class A {}");
        ParserSnapshot snapshot = ParserSnapshot.of(doc, root);
        assertEquals(7, snapshot.version());
        assertEquals("class A {}", snapshot.text());
        assertSame(root, snapshot.astRoot());
    }

    @Test
    void equalWhenVersionAndTextAndAstMatch() {
        AstNode root = parseAst("class A {}");
        ParserSnapshot a = new ParserSnapshot(1, "class A {}", root);
        ParserSnapshot b = new ParserSnapshot(1, "class A {}", root);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void notEqualWhenVersionDiffers() {
        AstNode root = parseAst("class A {}");
        ParserSnapshot a = new ParserSnapshot(1, "class A {}", root);
        ParserSnapshot b = new ParserSnapshot(2, "class A {}", root);
        assertNotEquals(a, b);
    }

    @Test
    void notEqualWhenTextDiffers() {
        AstNode root = parseAst("class A {}");
        ParserSnapshot a = new ParserSnapshot(1, "class A {}", root);
        ParserSnapshot b = new ParserSnapshot(1, "class B {}", root);
        assertNotEquals(a, b);
    }

    @Test
    void structurallyDifferentAstMakesSnapshotUnequal() {
        AstNode rootA = parseAst("class A { void m() { int x = 1; } }");
        AstNode rootB = parseAst("class A { void m() { int x = 2; } }");
        ParserSnapshot a = new ParserSnapshot(1, "class A { void m() { int x = 1; } }", rootA);
        ParserSnapshot b = new ParserSnapshot(1, "class A { void m() { int x = 2; } }", rootB);
        assertNotEquals(a, b);
    }

    @Test
    void structuralEqualityHoldsForFreshlyRebuiltTrees() {
        String source = "class A { void m() { int x = 1; } }";
        AstNode rootA = parseAst(source);
        AstNode rootB = parseAst(source);
        ParserSnapshot a = new ParserSnapshot(1, source, rootA);
        ParserSnapshot b = new ParserSnapshot(1, source, rootB);
        assertEquals(a, b, "two independently-parsed trees are structurally equal");
    }

    @Test
    void astRootNeverNull() {
        AstNode root = AstNode.of(AstNodeKind.COMPILATION_UNIT, TextRange.of(0, 0), List.of());
        ParserSnapshot snapshot = new ParserSnapshot(0, "", root);
        assertNotNull(snapshot.astRoot());
    }

    @Test
    void parentsLinkedAfterParse() {
        AstNode root = parseAst("class A {}");
        assertNotNull(root.children().get(0).parent(),
                "JavaParser.parse() links parents before returning");
    }
}
