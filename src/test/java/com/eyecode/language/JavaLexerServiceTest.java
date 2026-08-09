package com.eyecode.language;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.language.java.JavaLexer;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.JavaTokenType;
import com.eyecode.language.java.LexerSnapshot;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaLexerServiceTest {

    private final JavaLexerService service = new JavaLexerService();

    private static DocumentSnapshot snapshot(long version, String text) {
        return new DocumentSnapshot(version, text, null, null);
    }

    private static List<Token> directTokenize(String source) {
        return new JavaLexer().tokenize(source);
    }

    private static void assertSameTokens(List<Token> expected, List<Token> actual) {
        assertEquals(expected.size(), actual.size(), "token count");
        for (int i = 0; i < expected.size(); i++) {
            Token e = expected.get(i);
            Token a = actual.get(i);
            assertEquals(e.type(), a.type(), "type at index " + i);
            assertEquals(e.range(), a.range(), "range at index " + i);
            assertEquals(e.text(), a.text(), "text at index " + i);
        }
    }

    @Test
    void simpleClassSnapshotV1() {
        LexerSnapshot result = service.lex(snapshot(1, "class Test {}"));

        assertEquals(1, result.version());
        assertEquals(directTokenize("class Test {}").size(), result.tokens().size());
    }

    @Test
    void sameTextInTwoVersionsProducesTwoSnapshots() {
        LexerSnapshot v1 = service.lex(snapshot(1, "class Test {}"));
        LexerSnapshot v2 = service.lex(snapshot(2, "class Test {}"));

        assertEquals(1, v1.version());
        assertEquals(2, v2.version());
        assertEquals(v1.tokens(), v2.tokens());
    }

    @Test
    void changedTextProducesNewVersionSnapshot() {
        LexerSnapshot v1 = service.lex(snapshot(1, "class Test {}"));
        LexerSnapshot v2 = service.lex(snapshot(2, "class Test {\n}"));

        assertEquals(1, v1.version());
        assertEquals(2, v2.version());
        assertEquals(directTokenize("class Test {\n}"), v2.tokens());
        assertTrue(v2.tokens().size() > v1.tokens().size());
    }

    @Test
    void emptyDocumentProducesValidSnapshot() {
        LexerSnapshot result = service.lex(snapshot(1, ""));

        assertEquals(1, result.version());
        List<Token> tokens = result.tokens();
        assertTrue(tokens.size() >= 1);
        assertEquals(JavaTokenType.EOF, tokens.get(tokens.size() - 1).type());
    }

    @Test
    void rejectsNullDocument() {
        assertThrows(IllegalArgumentException.class, () -> service.lex(null));
    }

    @Test
    void versionParityAcrossMultipleVersions() {
        LexerSnapshot v1 = service.lex(snapshot(1, "class A {}"));
        LexerSnapshot v2 = service.lex(snapshot(2, "class A {\n}"));
        LexerSnapshot v3 = service.lex(snapshot(3, "class B { int x; }"));
        LexerSnapshot v4 = service.lex(snapshot(4, "class B { int x; }\n"));

        assertEquals(1, v1.version());
        assertEquals(2, v2.version());
        assertEquals(3, v3.version());
        assertEquals(4, v4.version());
    }

    @Test
    void tokenParityWithJavaLexer() {
        String[] samples = {
                "class Test {}",
                "import java.util.List;\nimport java.io.*;",
                "@Override public void run() {}",
                "",
                "int x = 42; // comment\nString s = \"hi\";",
                "if (value >= 10) { value++; } else { value--; }",
                "public final record Point(int x, int y) {}",
                "var list = List.of(1, 2, 3);"
        };
        for (String sample : samples) {
            LexerSnapshot result = service.lex(snapshot(5, sample));
            assertSameTokens(directTokenize(sample), result.tokens());
        }
    }

    @Test
    void staleSnapshotKeepsItsOwnVersionAndTokens() {
        EditorDocument document = new EditorDocument(null, "class A {}");
        LexerSnapshot v1 = service.lex(document.snapshot());
        assertEquals(1, v1.version());

        document.insert(10, "\n");
        LexerSnapshot v2 = service.lex(document.snapshot());
        assertEquals(2, v2.version());

        assertEquals(1, v1.version());
        assertEquals(directTokenize("class A {}"), v1.tokens());
        assertEquals(directTokenize(document.getText()), v2.tokens());
    }

    @Test
    void incrementalPathMatchesFullRelexAcrossDocumentMutations() {
        EditorDocument document = new EditorDocument(null, "class A {}");
        LexerSnapshot v1 = service.lex(document.snapshot());
        String t1 = document.getText();
        document.insert(t1.length(), "\n");
        LexerSnapshot v2 = service.lex(document.snapshot());
        String t2 = document.getText();
        document.insert(0, "import java.util.List;\n");
        LexerSnapshot v3 = service.lex(document.snapshot());
        String t3 = document.getText();
        document.insert(t3.length() - 1, "\n\n");
        LexerSnapshot v4 = service.lex(document.snapshot());

        assertEquals(1, v1.version());
        assertEquals(2, v2.version());
        assertEquals(3, v3.version());
        assertEquals(4, v4.version());
        assertSameTokens(directTokenize(t1), v1.tokens());
        assertSameTokens(directTokenize(t2), v2.tokens());
        assertSameTokens(directTokenize(t3), v3.tokens());
        assertSameTokens(directTokenize(document.getText()), v4.tokens());
    }
}
