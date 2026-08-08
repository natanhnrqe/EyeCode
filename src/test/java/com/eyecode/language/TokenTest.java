package com.eyecode.language;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.java.JavaTokenType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenTest {

    @Test
    void exposesTypeRangeAndText() {
        Token token = new Token(JavaTokenType.KEYWORD, TextRange.of(3, 7), "void");

        assertEquals(JavaTokenType.KEYWORD, token.type());
        assertEquals(TextRange.of(3, 7), token.range());
        assertEquals("void", token.text());
    }

    @Test
    void delegatesOffsetsAndLengthToRange() {
        Token token = new Token(JavaTokenType.IDENTIFIER, TextRange.of(6, 11), "Hello");

        assertEquals(6, token.startOffset());
        assertEquals(11, token.endOffset());
        assertEquals(5, token.length());
    }

    @Test
    void recordEqualityCoversAllComponents() {
        Token a = new Token(JavaTokenType.KEYWORD, TextRange.of(0, 5), "class");
        Token b = new Token(JavaTokenType.KEYWORD, TextRange.of(0, 5), "class");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertTrue(a.range().equals(TextRange.of(0, 5)));
    }

    @Test
    void emptyTokenHasZeroLength() {
        Token token = new Token(JavaTokenType.EOF, TextRange.of(0, 0), "");

        assertEquals(0, token.length());
        assertTrue(token.range().isEmpty());
    }

    @Test
    void rejectsNullComponents() {
        assertThrows(NullPointerException.class,
                () -> new Token(null, TextRange.of(0, 1), "a"));
        assertThrows(NullPointerException.class,
                () -> new Token(JavaTokenType.IDENTIFIER, null, "a"));
        assertThrows(NullPointerException.class,
                () -> new Token(JavaTokenType.IDENTIFIER, TextRange.of(0, 1), null));
    }

    @Test
    void reusingSharedRangeKeepsTokenStable() {
        TextRange shared = TextRange.of(2, 8);
        Token first = new Token(JavaTokenType.STRING, shared, "\"hi\"");
        Token second = new Token(JavaTokenType.NUMBER, shared, "123456");

        assertEquals(TextRange.of(2, 8), first.range());
        assertEquals(TextRange.of(2, 8), second.range());
        assertEquals(6, first.length());
        assertNotNull(first.type());
        assertNotNull(first.text());
    }
}
