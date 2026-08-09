package com.eyecode.language;

import com.eyecode.language.java.LexerSnapshot;
import com.eyecode.language.java.incremental.FullRelexStrategy;
import com.eyecode.language.java.incremental.IncrementalJavaLexer;
import com.eyecode.language.java.incremental.IncrementalLexResult;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncrementalJavaLexerTest {

    private final IncrementalJavaLexer incremental = new IncrementalJavaLexer();
    private final FullRelexStrategy full = new FullRelexStrategy();

    private IncrementalLexResult lex(long version, String oldText, String newText) {
        LexerSnapshot previous = full.lex(oldText, version);
        return incremental.lex(oldText, previous, newText, version + 1);
    }

    private void assertEqualsFullRelex(long version, String oldText, String newText) {
        LexerSnapshot expected = full.lex(newText, version + 1);
        LexerSnapshot actual = lex(version, oldText, newText).snapshot();
        assertEquals(expected, actual);
    }

    @Test
    void simpleInsertionMatchesFullRelex() {
        assertEqualsFullRelex(1, "class Test {}", "class MyTest {}");
    }

    @Test
    void simpleDeletionMatchesFullRelex() {
        assertEqualsFullRelex(1, "class MyTest {}", "class Test {}");
    }

    @Test
    void replacementMatchesFullRelex() {
        assertEqualsFullRelex(1, "int value = 1;", "int value = 42;");
    }

    @Test
    void insertionAtStartMatchesFullRelex() {
        assertEqualsFullRelex(1, "class A {}", "// head\nclass A {}");
    }

    @Test
    void insertionAtEndMatchesFullRelex() {
        assertEqualsFullRelex(1, "class A {}", "class A {}\n// tail");
    }

    @Test
    void middleChangeMatchesFullRelex() {
        assertEqualsFullRelex(1, "int a = 1;\nint b = 2;\nint c = 3;",
                "int a = 1;\nint b = 20;\nint c = 3;");
    }

    @Test
    void multipleSequentialChangesMatchFullRelex() {
        String text = "class Test {}";
        long version = 0;
        LexerSnapshot previous = full.lex(text, version);
        for (String next : new String[]{
                "class Test { int x; }",
                "class Test { int y = 2; }",
                "class Test {}\n// done",
                "class Other {}"
        }) {
            version++;
            IncrementalLexResult result = incremental.lex(text, previous, next, version);
            assertEquals(full.lex(next, version), result.snapshot());
            text = next;
            previous = result.snapshot();
        }
    }

    @Test
    void emptyDocumentMatchesFullRelex() {
        assertEqualsFullRelex(1, "", "");
        assertEqualsFullRelex(1, "", "class A {}");
        assertEqualsFullRelex(1, "class A {}", "");
    }

    @Test
    void oneLineDocumentMatchesFullRelex() {
        assertEqualsFullRelex(1, "class A {}", "class A { int x; }");
    }

    @Test
    void trailingNewlineMatchesFullRelex() {
        assertEqualsFullRelex(1, "class A {}\n", "class A {}\n\n");
        assertEqualsFullRelex(1, "class A {}\n\n", "class A {}\n");
    }

    @Test
    void lfDocumentMatchesFullRelex() {
        assertEqualsFullRelex(1, "int a = 1;\nint b = 2;\n", "int a = 1;\nint b = 3;\nint c = 4;");
    }

    @Test
    void crlfDocumentMatchesFullRelex() {
        assertEqualsFullRelex(1, "int a = 1;\r\nint b = 2;\r\n", "int a = 10;\r\nint b = 2;\r\n");
        assertEqualsFullRelex(1, "int a = 1;\r\nint b = 2;", "int a = 1;\r\nint b = 22;\r\n");
    }

    @Test
    void stabilizationReusesTailAfterEdit() {
        String oldText = "int a = 1;\nint b = 2;\nint c = 3;\nint d = 4;\nint e = 5;";
        String newText = "int a = 1;\nint b = 20;\nint c = 3;\nint d = 4;\nint e = 5;";
        IncrementalLexResult result = lex(1, oldText, newText);

        assertEquals(full.lex(newText, 2), result.snapshot());
        assertTrue(result.reuseWindow().reusedTokenCount() > 5,
                "the tail after the edit should be reused");
        assertTrue(result.relexedTokenCount() < result.snapshot().tokens().size(),
                "not every token should be re-lexed, but was " + result.relexedTokenCount());
    }

    @Test
    void quoteRemovalForcesLexingToEof() {
        String oldText = "String a = \"hello\";\nint b = 2;";
        String newText = "String a = \"hello;\nint b = 2;";
        IncrementalLexResult result = lex(1, oldText, newText);

        assertEquals(full.lex(newText, 2), result.snapshot());
        assertEquals(full.lex(newText, 2).tokens(), result.snapshot().tokens());
    }

    @Test
    void quoteInsertionInsideStringForcesLexingThroughTheString() {
        String oldText = "String a = \"hello\";\nint b = 2;";
        String newText = "String a = \"he\"llo\";\nint b = 2;";
        IncrementalLexResult result = lex(1, oldText, newText);

        assertEquals(full.lex(newText, 2), result.snapshot());
    }

    @Test
    void blockCommentEditExtendsRegionToClosingMarker() {
        String oldText = "/* comment\n * text\n */\nclass A {}";
        String newText = "/* comment\n * text\n * more\n */\nclass A {}";
        IncrementalLexResult result = lex(1, oldText, newText);

        assertEquals(full.lex(newText, 2), result.snapshot());
        assertTrue(result.reuseWindow().reusedTokenCount() > 0,
                "the tail after the comment close should be reused");
    }

    @Test
    void editInsideOpenStringStartsRelexAtStringStart() {
        String oldText = "String s = \"hello world\";\nint x = 1;";
        String newText = "String s = \"hello world!\";\nint x = 1;";
        IncrementalLexResult result = lex(1, oldText, newText);

        assertEquals(full.lex(newText, 2), result.snapshot());
        assertTrue(result.reuseWindow().reusedTokenCount() > 0);
    }

    @Test
    void insertionContinuingIdentifierRelexesFromTokenStart() {
        assertEqualsFullRelex(1, "class Test { int value = 1; }", "class TestX { int value = 1; }");
    }

    @Test
    void insertionMergingWhitespaceRelexesFromPreviousToken() {
        assertEqualsFullRelex(1, "class A {}\nint x = 1;", "class A {}\n\nint x = 1;");
    }

    @Test
    void insertionContinuingANumberMergesTheNumberToken() {
        assertEqualsFullRelex(1, "int b = 2;", "int b = 20;");
    }

    @Test
    void taxonomyDecisionsSurviveIncrementalLexing() {
        String oldText = "@Override\npublic void run() { boolean b = true; Object o = null; }";
        String newText = "@Override\npublic void run() { boolean b = false; Object o = null; }";
        IncrementalLexResult result = lex(1, oldText, newText);

        assertEquals(full.lex(newText, 2), result.snapshot());
        LexerSnapshot snapshot = result.snapshot();
        assertTrue(snapshot.tokens().stream().noneMatch(
                t -> t.type() == com.eyecode.language.java.JavaTokenType.ERROR));
    }

    @Test
    void resultVersionIsTheNewDocumentVersion() {
        IncrementalLexResult result = lex(7, "class A {}", "class B {}");
        assertEquals(8, result.snapshot().version());
    }

    @Test
    void noPreviousSnapshotFallsBackToFullRelex() {
        IncrementalLexResult result = incremental.lex(null, null, "class A {}", 1);

        assertEquals(full.lex("class A {}", 1), result.snapshot());
        assertEquals(false, result.isIncremental());
    }

    @Test
    void identicalTextsReuseWholeSnapshot() {
        LexerSnapshot previous = full.lex("class A {}", 3);
        IncrementalLexResult result = incremental.lex("class A {}", previous, "class A {}", 4);

        assertEquals(full.lex("class A {}", 4), result.snapshot());
        assertTrue(result.prefixReusedTokenCount() > 0);
    }
}
