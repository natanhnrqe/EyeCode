package com.eyecode.language.java.parser.incremental;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.LineMap;
import com.eyecode.editor.intelligence.document.TextChange;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.ast.AstNodeKind;
import com.eyecode.language.java.JavaLexerService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncrementalParserTest {

    private static AstNode fullParse(String text) {
        JavaLexerService lexer = new JavaLexerService();
        var lex = lexer.lex(DocumentSnapshot.oneShot(text));
        JavaTokenStream stream = new JavaTokenStream(lex.tokens(), text);
        return new JavaParser(stream).parse().getAstRoot();
    }

    private static DocumentSnapshot session(String text, long version) {
        return new DocumentSnapshot(version, text, LineMap.of(text), null, "s1");
    }

    private static IncrementalParserStrategy.Result parse(String before, String after) {
        AstNode previous = fullParse(before);
        DocumentSnapshot previousDoc = session(before, 1);
        DocumentSnapshot currentDoc = session(after, 2);
        TextChange change = TextChange.between(previousDoc, currentDoc);
        return new IncrementalParserStrategy().parse(currentDoc, previousDoc, previous, change);
    }

    private static boolean structurallyEqual(AstNode a, AstNode b) {
        return AstEquivalence.equals(a, b);
    }

    private static String findNodeText(AstNode root, AstNodeKind kind) {
        if (root.kind() == kind) {
            return root.range().endOffset() > 0 ? "found" : "empty";
        }
        for (AstNode child : root.children()) {
            String r = findNodeText(child, kind);
            if (r != null) return r;
        }
        return null;
    }

    @Test
    void simpleEditInsideStatementMatchesFullReparse() {
        String before = "class A { void m() { int x = 1; } }";
        String after = "class A { void m() { int x = 12; } }";
        IncrementalParserStrategy.Result result = parse(before, after);
        AstNode fresh = fullParse(after);
        assertTrue(AstEquivalence.equals(result.astRoot(), fresh),
                "incremental AST must be structurally equal to fresh full parse");
    }

    @Test
    void simpleInsertInsideExpressionMatchesFullReparse() {
        String before = "class A { int x = 1 + 2; }";
        String after = "class A { int x = 1 + 22; }";
        IncrementalParserStrategy.Result result = parse(before, after);
        AstNode fresh = fullParse(after);
        if (!AstEquivalence.equals(result.astRoot(), fresh)) {
            System.out.println("=== INCREMENTAL ===");
            printAst(result.astRoot(), 0);
            System.out.println("=== FRESH ===");
            printAst(fresh, 0);
        }
        assertTrue(AstEquivalence.equals(result.astRoot(), fresh),
                "incremental AST must be structurally equal to fresh full parse");
    }

    private static void printAst(AstNode node, int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) sb.append("  ");
        sb.append(node.kind()).append(" ").append(node.range());
        if (node.token() != null) {
            sb.append(" tok=").append(node.token().text());
        }
        System.out.println(sb);
        for (AstNode child : node.children()) {
            printAst(child, depth + 1);
        }
    }

    @Test
    void simpleDeleteMatchesFullReparse() {
        String before = "class A { void m() { int x = 123; } }";
        String after = "class A { void m() { int x = 3; } }";
        AstNode previous = fullParse(before);
        System.out.println("=== OLD AST ===");
        printAst(previous, 0);
        IncrementalParserStrategy.Result result = parse(before, after);
        AstNode fresh = fullParse(after);
        if (!AstEquivalence.equals(result.astRoot(), fresh)) {
            System.out.println("=== INCREMENTAL ===");
            printAst(result.astRoot(), 0);
            System.out.println("=== FRESH ===");
            printAst(fresh, 0);
        }
        assertTrue(AstEquivalence.equals(result.astRoot(), fresh));
    }

    @Test
    void insertionOfBraceForcesFallback() {
        String before = "class A { void m() { int x = 1; } }";
        String after = "class A { void m() { { int x = 1; } } }";
        IncrementalParserStrategy.Result result = parse(before, after);
        assertTrue(result.fallbackUsed(), "brace insertion must fall back");
    }

    @Test
    void insertionOfSemicolonForcesFallback() {
        String before = "class A { void m() { int x = 1 } }";
        String after = "class A { void m() { int x = 1; } }";
        IncrementalParserStrategy.Result result = parse(before, after);
        assertTrue(result.fallbackUsed());
    }

    @Test
    void rangeRemainsValidAfterIncrementalEdit() {
        String before = "class A { void m() { int x = 1; } }";
        String after = "class A { void m() { int x = 12; } }";
        IncrementalParserStrategy.Result result = parse(before, after);
        assertTrue(result.astRoot().range().startOffset() >= 0);
        assertTrue(result.astRoot().range().endOffset() <= after.length());
    }

    @Test
    void parentLinksCorrectAfterIncrementalEdit() {
        String before = "class A { void m() { int x = 1; } }";
        String after = "class A { void m() { int x = 12; } }";
        IncrementalParserStrategy.Result result = parse(before, after);
        assertNotNull(result.astRoot().children().get(0).parent(),
                "root has no parent; first child must link back to root");
        AstNode clazz = result.astRoot().children().get(0);
        for (AstNode child : clazz.children()) {
            assertEquals(clazz, child.parent());
        }
    }

    @Test
    void sequentialEditsConvergeToFullParse() {
        String before = "class A { void m() { int x = 1; } }";
        String v2 = "class A { void m() { int x = 12; } }";
        String v3 = "class A { void m() { int x = 123; } }";
        AstNode prevRoot = fullParse(before);
        DocumentSnapshot prevDoc = session(before, 1);

        for (String[] step : new String[][]{{before, v2}, {v2, v3}}) {
            DocumentSnapshot nextDoc = session(step[1], prevDoc.version() + 1);
            TextChange change = TextChange.between(prevDoc, nextDoc);
            IncrementalParserStrategy.Result result =
                    new IncrementalParserStrategy().parse(nextDoc, prevDoc, prevRoot, change);
            AstNode fresh = fullParse(step[1]);
            assertTrue(structurallyEqual(result.astRoot(), fresh),
                    () -> "sequential edit divergence at " + step[1]);
            prevRoot = result.astRoot();
            prevDoc = nextDoc;
        }
    }

    @Test
    void chainedEditsAfterFallbackRecover() {
        String before = "class A { void m() { int x = 1; } }";
        String braceInsert = "class A { void m() { { int x = 1; } } }";
        String plain = "class A { void m() { int x = 99; } }";
        AstNode prevRoot = fullParse(before);
        DocumentSnapshot prevDoc = session(before, 1);

        DocumentSnapshot nextDoc = session(braceInsert, 2);
        TextChange change = TextChange.between(prevDoc, nextDoc);
        IncrementalParserStrategy.Result r1 = new IncrementalParserStrategy()
                .parse(nextDoc, prevDoc, prevRoot, change);
        assertTrue(r1.fallbackUsed());

        prevRoot = r1.astRoot();
        prevDoc = nextDoc;

        DocumentSnapshot finalDoc = session(plain, 3);
        TextChange change2 = TextChange.between(prevDoc, finalDoc);
        IncrementalParserStrategy.Result r2 = new IncrementalParserStrategy()
                .parse(finalDoc, prevDoc, prevRoot, change2);
        AstNode fresh = fullParse(plain);
        assertTrue(structurallyEqual(r2.astRoot(), fresh));
    }

    @Test
    void resultRecordsFallbackReason() {
        String before = "class A { void m() { int x = 1; } }";
        String after = "class A { void m() { { int x = 1; } } }";
        IncrementalParserStrategy.Result result = parse(before, after);
        assertTrue(result.fallbackUsed());
        assertNotNull(result.fallbackReason());
    }

    @Test
    void resultReportsNoFallbackForSafeEdits() {
        String before = "class A { void m() { int x = 1; } }";
        String after = "class A { void m() { int x = 12; } }";
        IncrementalParserStrategy.Result result = parse(before, after);
        assertFalse(result.fallbackUsed());
    }

    @Test
    void editAtStartOfClassBodyStillMatches() {
        String before = "class A { int x = 1; int y = 2; }";
        String after = "class A { int x = 11; int y = 2; }";
        IncrementalParserStrategy.Result result = parse(before, after);
        AstNode fresh = fullParse(after);
        assertTrue(structurallyEqual(result.astRoot(), fresh));
    }

    @Test
    void rangeInvariantAfterInsertion() {
        String before = "class A { void m() { int x = 1; } }";
        String after = "class A { void m() { int x = 12; } }";
        IncrementalParserStrategy.Result result = parse(before, after);
        AstEquivalence.equals(result.astRoot(), fullParse(after));
        for (AstNode child : result.astRoot().children()) {
            assertTrue(result.astRoot().range().contains(child.range()),
                    () -> "parent does not contain child " + child);
        }
    }
}
