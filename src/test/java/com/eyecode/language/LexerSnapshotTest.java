package com.eyecode.language;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.java.JavaTokenType;
import com.eyecode.language.java.LexerSnapshot;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LexerSnapshotTest {

    private static Token token(JavaTokenType type, int start, int end, String text) {
        return new Token(type, TextRange.of(start, end), text);
    }

    @Test
    void emptySnapshotIsValid() {
        LexerSnapshot snapshot = new LexerSnapshot(0, List.of());

        assertEquals(0, snapshot.version());
        assertTrue(snapshot.isEmpty());
        assertEquals(List.of(), snapshot.tokens());
    }

    @Test
    void exposesVersionAndTokens() {
        List<Token> tokens = List.of(
                token(JavaTokenType.KEYWORD, 0, 5, "class"),
                token(JavaTokenType.IDENTIFIER, 6, 10, "Test"),
                token(JavaTokenType.EOF, 10, 10, "")
        );
        LexerSnapshot snapshot = new LexerSnapshot(7, tokens);

        assertEquals(7, snapshot.version());
        assertFalse(snapshot.isEmpty());
        assertEquals(3, snapshot.tokens().size());
        assertEquals(tokens, snapshot.tokens());
    }

    @Test
    void tokenListIsNotModifiable() {
        LexerSnapshot snapshot = new LexerSnapshot(1, List.of(
                token(JavaTokenType.IDENTIFIER, 0, 1, "a")
        ));

        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.tokens().add(token(JavaTokenType.KEYWORD, 1, 2, "b")));
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.tokens().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.tokens().remove(0));
    }

    @Test
    void sourceListMutationsDoNotLeakIntoSnapshot() {
        List<Token> source = new ArrayList<>();
        source.add(token(JavaTokenType.KEYWORD, 0, 5, "class"));
        LexerSnapshot snapshot = new LexerSnapshot(3, source);

        source.add(token(JavaTokenType.IDENTIFIER, 6, 10, "Test"));

        assertEquals(1, snapshot.tokens().size());
        assertEquals(token(JavaTokenType.KEYWORD, 0, 5, "class"), snapshot.tokens().get(0));
    }

    @Test
    void rejectsNullTokenList() {
        assertThrows(IllegalArgumentException.class, () -> new LexerSnapshot(1, null));
    }

    @Test
    void rejectsNullTokenElements() {
        List<Token> withNull = new ArrayList<>();
        withNull.add(token(JavaTokenType.KEYWORD, 0, 5, "class"));
        withNull.add(null);

        assertThrows(NullPointerException.class, () -> new LexerSnapshot(1, withNull));
    }

    @Test
    void sharedTokenInstancesAreSafeAcrossSnapshots() {
        Token shared = token(JavaTokenType.IDENTIFIER, 0, 4, "Test");
        LexerSnapshot first = new LexerSnapshot(1, List.of(shared));
        LexerSnapshot second = new LexerSnapshot(2, List.of(shared));

        assertEquals(shared, first.tokens().get(0));
        assertEquals(shared, second.tokens().get(0));
        assertEquals(TextRange.of(0, 4), second.tokens().get(0).range());
    }

    @Test
    void differentVersionsNeverGetConfused() {
        List<Token> tokens = List.of(token(JavaTokenType.KEYWORD, 0, 5, "class"));
        LexerSnapshot v1 = new LexerSnapshot(1, tokens);
        LexerSnapshot v2 = new LexerSnapshot(2, tokens);

        assertEquals(1, v1.version());
        assertEquals(2, v2.version());
        assertNotEquals(v1, v2);
        assertNotEquals(v1.hashCode(), v2.hashCode());
        assertEquals(tokens, v1.tokens());
        assertEquals(tokens, v2.tokens());
    }

    @Test
    void equalityCoversVersionAndTokens() {
        List<Token> tokens = List.of(token(JavaTokenType.KEYWORD, 0, 5, "class"));
        LexerSnapshot a = new LexerSnapshot(9, tokens);
        LexerSnapshot b = new LexerSnapshot(9, tokens);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
