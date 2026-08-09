package com.eyecode.language;

import com.eyecode.language.java.LexerSnapshot;
import com.eyecode.language.java.incremental.FullRelexStrategy;
import com.eyecode.language.java.incremental.IncrementalJavaLexer;
import com.eyecode.language.java.incremental.IncrementalLexResult;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Frontier parity cases for the incremental lexer (Sprint 5.2e). Every
 * mutation is validated token-by-token — type, text, startOffset AND
 * endOffset — against a full re-lex. Token counts alone are never sufficient
 * proof of correctness.
 */
class IncrementalLexerFrontierTest {

    private final IncrementalJavaLexer incremental = new IncrementalJavaLexer();
    private final FullRelexStrategy full = new FullRelexStrategy();

    private void assertParity(long version, String oldText, String newText, String scenario) {
        LexerSnapshot previous = full.lex(oldText, version);
        IncrementalLexResult result = incremental.lex(oldText, previous, newText, version + 1);
        LexerSnapshot expected = full.lex(newText, version + 1);

        assertEquals(expected.version(), result.snapshot().version(), scenario + " version");
        List<Token> actual = result.snapshot().tokens();
        List<Token> want = expected.tokens();
        assertEquals(want.size(), actual.size(), scenario + " token count");
        for (int i = 0; i < want.size(); i++) {
            Token e = want.get(i);
            Token a = actual.get(i);
            assertEquals(e.type(), a.type(), scenario + " type at " + i);
            assertEquals(e.text(), a.text(), scenario + " text at " + i);
            assertEquals(e.startOffset(), a.startOffset(), scenario + " startOffset at " + i);
            assertEquals(e.endOffset(), a.endOffset(), scenario + " endOffset at " + i);
        }
    }

    @Test
    void insertionAtStart() {
        assertParity(1, "int x = 1;", "import x;\nint x = 1;", "insert-at-start");
    }

    @Test
    void insertionAtEnd() {
        assertParity(1, "int x = 1;", "int x = 1;\n// tail", "insert-at-end");
    }

    @Test
    void removalAtStart() {
        assertParity(1, "import x;\nint x = 1;", "int x = 1;", "remove-at-start");
    }

    @Test
    void removalAtEnd() {
        assertParity(1, "int x = 1;\n// tail", "int x = 1;", "remove-at-end");
    }

    @Test
    void editExactlyBeforeAToken() {
        assertParity(1, "int x = 1;", "int x = 1 ;", "before-token");
    }

    @Test
    void editExactlyAfterAToken() {
        assertParity(1, "int x = 1;", "int x = 1; ", "after-token");
    }

    @Test
    void editInsideAToken() {
        assertParity(1, "int counter = 1;", "int counter2 = 1;", "inside-token");
    }

    @Test
    void identifierGrowth() {
        assertParity(1, "class A {}", "class AB {}", "identifier-growth");
    }

    @Test
    void numberGrowth() {
        assertParity(1, "int x = 1;", "int x = 10;", "number-growth");
    }

    @Test
    void commentGrowth() {
        assertParity(1, "// ab\nint x;", "// abc\nint x;", "comment-growth");
    }

    @Test
    void stringGrowth() {
        assertParity(1, "String s = \"ab\";", "String s = \"abc\";", "string-growth");
    }

    @Test
    void incompleteStringGrowth() {
        assertParity(1, "String s = \"abc", "String s = \"abcd", "incomplete-string-growth");
    }

    @Test
    void commentBecomesCode() {
        assertParity(1, "// int x = 1;", "int x = 1;", "comment-to-code");
    }

    @Test
    void codeBecomesComment() {
        assertParity(1, "int x = 1;", "// int x = 1;", "code-to-comment");
    }

    @Test
    void blockOpenAndClose() {
        assertParity(1, "class A int x;", "class A { int x; }", "block-open-close");
    }

    @Test
    void multipleEditsOnTheSameToken() {
        String text = "String value = \"ab\";";
        String text2 = "String value = \"abc\";";
        String text3 = "String value = \"abcd\";";
        String text4 = "String value = \"abd\";";

        LexerSnapshot previous = full.lex(text, 1);
        LexerSnapshot v2 = incremental.lex(text, previous, text2, 2).snapshot();
        LexerSnapshot v3 = incremental.lex(text2, v2, text3, 3).snapshot();
        IncrementalLexResult finalStep = incremental.lex(text3, v3, text4, 4);

        assertEquals(full.lex(text4, 4), finalStep.snapshot(), "multiple-edits-same-token");
    }
}
