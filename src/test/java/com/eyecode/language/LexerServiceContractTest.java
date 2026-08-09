package com.eyecode.language;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.JavaTokenType;
import com.eyecode.language.java.LexerSnapshot;
import com.eyecode.language.java.LexerService;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the final {@link LexerService} contract (Sprint 5.2e): never null,
 * version-matched, immutable tokens, deterministic output, one-shot behavior,
 * stale input handling and full lexical coverage of edge inputs.
 */
class LexerServiceContractTest {

    private final LexerService service = new JavaLexerService();

    private static DocumentSnapshot snapshot(long version, String text) {
        return new DocumentSnapshot(version, text, null, null);
    }

    @Test
    void neverReturnsNull() {
        assertNotNull(service.lex(snapshot(1, "")));
        assertNotNull(service.lex(snapshot(1, "class A {}")));
        assertNotNull(service.lex(new DocumentSnapshot(1, null, null, null)));
        assertNotNull(service.lex(DocumentSnapshot.oneShot("class A {}")));
    }

    @Test
    void returnedVersionAlwaysMatchesInputVersion() {
        for (long version : new long[]{0, 1, 2, 7, 42}) {
            LexerSnapshot result = service.lex(snapshot(version, "class A {}"));
            assertEquals(version, result.version());
        }
    }

    @Test
    void snapshotVersionMatchesDocumentVersionInLiveDocument() {
        com.eyecode.editor.v2.EditorDocument document =
                new com.eyecode.editor.v2.EditorDocument(null, "class A {}");
        for (int i = 0; i < 4; i++) {
            document.insert(document.length(), "\n");
            LexerSnapshot result = service.lex(document.snapshot());
            assertEquals(document.currentVersion(), result.version());
        }
    }

    @Test
    void tokensListIsUnmodifiable() {
        LexerSnapshot result = service.lex(snapshot(1, "class A {}"));

        assertThrows(UnsupportedOperationException.class, () -> result.tokens().add(result.tokens().get(0)));
        assertThrows(UnsupportedOperationException.class, () -> result.tokens().remove(0));
    }

    @Test
    void tokenInstancesAreImmutableRecords() {
        LexerSnapshot result = service.lex(snapshot(1, "class A {}"));
        Token token = result.tokens().get(0);
        assertTrue(token.type() instanceof com.eyecode.language.TokenType);
        assertNotNull(token.range());
        assertNotNull(token.text());
    }

    @Test
    void repeatedLexingIsDeterministic() {
        String text = "class A { int x = 42; }";
        LexerSnapshot first = service.lex(snapshot(3, text));
        LexerSnapshot second = service.lex(snapshot(3, text));

        assertEquals(first, second);
        assertEquals(first.tokens(), second.tokens());
    }

    @Test
    void staleAnonymousSnapshotIsFullLexedAndVersionBound() {
        LexerSnapshot v1 = service.lex(DocumentSnapshot.oneShot("class A {}"));
        LexerSnapshot v2 = service.lex(DocumentSnapshot.oneShot("class AB {}"));
        LexerSnapshot v1Again = service.lex(DocumentSnapshot.oneShot("class A {}"));

        assertEquals(0, v1.version());
        assertEquals(0, v2.version());
        assertEquals(v1, v1Again);
    }

    @Test
    void emptyDocumentYieldsEofOnly() {
        LexerSnapshot result = service.lex(snapshot(1, ""));

        assertEquals(1, result.tokens().size());
        assertEquals(JavaTokenType.EOF, result.tokens().get(0).type());
        assertEquals(0, result.tokens().get(0).startOffset());
        assertEquals(0, result.tokens().get(0).endOffset());
    }

    @Test
    void incompleteSourceIsTolerated() {
        LexerSnapshot result = service.lex(snapshot(1, "class A { String s = \"unterminated; // x"));

        List<Token> tokens = result.tokens();
        assertFalse(tokens.isEmpty());
        assertEquals(JavaTokenType.EOF, tokens.get(tokens.size() - 1).type());
    }

    @Test
    void unicodeTextIsTokenized() {
        String text = "class Café { String s = \"olá — mundo\"; }";
        LexerSnapshot result = service.lex(snapshot(1, text));

        assertEquals(text.length(), result.tokens().get(result.tokens().size() - 1).endOffset());
        assertTrue(result.tokens().stream().anyMatch(t -> t.text().contains("Café")));
        assertTrue(result.tokens().stream().anyMatch(t -> t.type() == JavaTokenType.STRING));
    }

    @Test
    void crlfLineEndingsAreTokenized() {
        LexerSnapshot result = service.lex(snapshot(1, "int a;\r\nint b;\r\n"));

        assertTrue(result.tokens().stream().anyMatch(t -> t.text().equals("\r\n")));
        assertEquals(JavaTokenType.EOF, result.tokens().get(result.tokens().size() - 1).type());
    }

    @Test
    void annotationIsAtPlusIdentifier() {
        LexerSnapshot result = service.lex(snapshot(1, "@Override public void run() {}"));

        assertEquals(JavaTokenType.AT, result.tokens().get(0).type());
        assertEquals(JavaTokenType.IDENTIFIER, result.tokens().get(1).type());
        assertEquals("Override", result.tokens().get(1).text());
    }

    @Test
    void booleanAndNullAreLiteralsNotKeywords() {
        LexerSnapshot result = service.lex(snapshot(1, "boolean b = true; Object o = null;"));

        assertTrue(result.tokens().stream().anyMatch(t ->
                t.type() == JavaTokenType.BOOLEAN_LITERAL && t.text().equals("true")));
        assertTrue(result.tokens().stream().anyMatch(t ->
                t.type() == JavaTokenType.NULL_LITERAL && t.text().equals("null")));
        assertTrue(result.tokens().stream().noneMatch(t ->
                t.type() == JavaTokenType.KEYWORD && (t.text().equals("true") || t.text().equals("null"))));
    }

    @Test
    void oneShotSnapshotsNeverTouchTheCache() {
        LexerSnapshot v1 = service.lex(DocumentSnapshot.oneShot("class A {}"));
        LexerSnapshot v1Again = service.lex(DocumentSnapshot.oneShot("class A {}"));

        assertNotSame(v1, v1Again, "one-shot lexes must not be reused from cache");
        assertEquals(v1, v1Again);
    }

    @Test
    void rejectsNullDocument() {
        assertThrows(IllegalArgumentException.class, () -> service.lex(null));
    }
}
