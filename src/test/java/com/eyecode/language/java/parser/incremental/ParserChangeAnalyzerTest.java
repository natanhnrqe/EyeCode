package com.eyecode.language.java.parser.incremental;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.LineMap;
import com.eyecode.editor.intelligence.document.TextChange;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.java.JavaLexerService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserChangeAnalyzerTest {

    private static AstNode parseAst(String text) {
        JavaLexerService lexer = new JavaLexerService();
        var lex = lexer.lex(DocumentSnapshot.oneShot(text));
        JavaTokenStream stream = new JavaTokenStream(lex.tokens(), text);
        return new JavaParser(stream).parse().getAstRoot();
    }

    private static DocumentSnapshot session(String text, long version) {
        return new DocumentSnapshot(version, text, LineMap.of(text), null, "s1");
    }

    private static TextChange change(String before, String after) {
        return TextChange.between(session(before, 1), session(after, 2));
    }

    @Test
    void missingPreviousAstForcesFallback() {
        TextChange c = new TextChange(1, TextRange.of(0, 0), "x", TextRange.of(0, 1));
        ParserChangeRegion region = new ParserChangeAnalyzer().analyze(c, null, null);
        assertTrue(region.fallbackRequired());
        assertNotNull(region.fallbackReason());
    }

    @Test
    void emptyChangeForcesFallback() {
        String source = "class A {}";
        AstNode root = parseAst(source);
        TextChange c = new TextChange(1, TextRange.of(0, 0), "", TextRange.of(0, 0));
        ParserChangeRegion region = new ParserChangeAnalyzer().analyze(c, session(source, 1), root);
        assertTrue(region.fallbackRequired());
    }

    @Test
    void insertionInsideStatementIsIncremental() {
        String before = "class A { void m() { int x = 1; } }";
        String after = "class A { void m() { int x = 12; } }";
        AstNode root = parseAst(before);
        TextChange c = change(before, after);
        ParserChangeRegion region = new ParserChangeAnalyzer().analyze(c, session(before, 1), root);
        assertFalse(region.fallbackRequired(), () -> "fallback reason: " + region.fallbackReason());
        assertNotNull(region.affectedNode());
    }

    @Test
    void removalInsideStatementIsIncremental() {
        String before = "class A { void m() { int x = 123; } }";
        String after = "class A { void m() { int x = 3; } }";
        AstNode root = parseAst(before);
        TextChange c = change(before, after);
        ParserChangeRegion region = new ParserChangeAnalyzer().analyze(c, session(before, 1), root);
        assertFalse(region.fallbackRequired(), () -> "fallback reason: " + region.fallbackReason());
    }

    @Test
    void insertionOfBraceForcesFallback() {
        String before = "class A { void m() { int x = 1; } }";
        String after = "class A { void m() { { int x = 1; } } }";
        AstNode root = parseAst(before);
        TextChange c = change(before, after);
        ParserChangeRegion region = new ParserChangeAnalyzer().analyze(c, session(before, 1), root);
        assertTrue(region.fallbackRequired());
    }

    @Test
    void removalOfSemicolonForcesFallback() {
        String before = "class A { void m() { int x = 1; } }";
        String after = "class A { void m() { int x = 1 } }";
        AstNode root = parseAst(before);
        TextChange c = change(before, after);
        ParserChangeRegion region = new ParserChangeAnalyzer().analyze(c, session(before, 1), root);
        assertTrue(region.fallbackRequired());
    }

    @Test
    void insertionOfSemicolonForcesFallback() {
        String before = "class A { void m() { int x = 1 } }";
        String after = "class A { void m() { int x = 1; } }";
        AstNode root = parseAst(before);
        TextChange c = change(before, after);
        ParserChangeRegion region = new ParserChangeAnalyzer().analyze(c, session(before, 1), root);
        assertTrue(region.fallbackRequired());
    }

    @Test
    void removalOfParenForcesFallback() {
        String before = "class A { void m(int x) { } }";
        String after = "class A { void mint x) { } }";
        AstNode root = parseAst(before);
        TextChange c = change(before, after);
        ParserChangeRegion region = new ParserChangeAnalyzer().analyze(c, session(before, 1), root);
        assertTrue(region.fallbackRequired());
    }

    @Test
    void changeOutsideAstForcesFallback() {
        String before = "class A {}";
        AstNode root = parseAst(before);
        TextChange c = new TextChange(2, TextRange.of(100, 100), "x", TextRange.of(100, 101));
        ParserChangeRegion region = new ParserChangeAnalyzer().analyze(c, session(before, 1), root);
        assertTrue(region.fallbackRequired());
    }

    @Test
    void insertionInsideClassBodyForcesFallbackBecauseOfSemicolon() {
        String before = "class A { int a; }";
        String after = "class A { int a; int b; }";
        AstNode root = parseAst(before);
        TextChange c = change(before, after);
        ParserChangeRegion region = new ParserChangeAnalyzer().analyze(c, session(before, 1), root);
        assertTrue(region.fallbackRequired(),
                "inserted semicolon always forces full reparse");
    }

    @Test
    void editInsideExpressionIsIncremental() {
        String before = "class A { int x = 1 + 2; }";
        String after = "class A { int x = 1 + 5; }";
        AstNode root = parseAst(before);
        TextChange c = change(before, after);
        ParserChangeRegion region = new ParserChangeAnalyzer().analyze(c, session(before, 1), root);
        assertFalse(region.fallbackRequired(), () -> "fallback: " + region.fallbackReason());
    }

    @Test
    void editInTwoStatementsAtOnceForcesFallback() {
        String before = "class A { void m() { int a = 1; int b = 2; } }";
        String after = "class A { void m() { int a = 3; int b = 9; } }";
        AstNode root = parseAst(before);
        TextChange c = change(before, after);
        ParserChangeRegion region = new ParserChangeAnalyzer().analyze(c, session(before, 1), root);
        assertTrue(region.fallbackRequired(),
                "region containing multiple statements must not be incrementally reparsed");
    }

    @Test
    void reparsableRangeContainsAffectedNode() {
        String before = "class A { void m() { int x = 1; } }";
        String after = "class A { void m() { int x = 12; } }";
        AstNode root = parseAst(before);
        TextChange c = change(before, after);
        ParserChangeRegion region = new ParserChangeAnalyzer().analyze(c, session(before, 1), root);
        assertFalse(region.fallbackRequired());
        assertNotNull(region.affectedNode());
        TextRange reparsable = region.reparsableRange();
        TextRange affected = region.affectedNode().range();
        assertTrue(reparsable.contains(affected.startOffset()),
                () -> "reparsable=" + reparsable + " affected=" + affected);
        assertTrue(reparsable.contains(affected.endOffset()),
                () -> "reparsable=" + reparsable + " affected=" + affected);
    }

    @Test
    void findDeepestContainingReturnsNullForOutsideRange() {
        AstNode root = parseAst("class A {}");
        AstNode result = ParserChangeAnalyzer.findDeepestContaining(root, TextRange.of(100, 110));
        assertNull(result);
    }

    @Test
    void findReparsableAncestorReturnsBlockForInnerStatement() {
        AstNode root = parseAst("class A { void m() { int x = 1; } }");
        AstNode method = root.children().get(0).children().stream()
                .filter(c -> "METHOD_DECLARATION".equals(c.kind().name()))
                .findFirst().orElseThrow();
        AstNode block = method.children().stream()
                .filter(c -> "BLOCK".equals(c.kind().name()))
                .findFirst().orElseThrow();
        AstNode statement = block.children().get(0);
        AstNode reparsable = ParserChangeAnalyzer.findReparsableAncestor(statement);
        assertEquals(block, reparsable);
    }
}
