package com.eyecode.language;

import com.eyecode.language.java.JavaTokenType;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaTokenTypeTest {

    @Test
    void implementsCanonicalTokenType() {
        assertTrue(TokenType.class.isAssignableFrom(JavaTokenType.class));
        assertTrue(JavaTokenType.KEYWORD instanceof TokenType);
    }

    @Test
    void preservesLegacyElevenConstantsInOrder() {
        List<String> legacy = List.of(
                "EOF", "ERROR", "WHITESPACE", "COMMENT", "KEYWORD", "IDENTIFIER",
                "NUMBER", "STRING", "CHARACTER", "OPERATOR", "SEPARATOR"
        );
        List<String> names = Arrays.stream(JavaTokenType.values()).map(Enum::name).toList();

        assertEquals(legacy, names.subList(0, legacy.size()));
    }

    @Test
    void addsCanonicalConstants() {
        List<String> names = Arrays.stream(JavaTokenType.values()).map(Enum::name).toList();

        assertTrue(names.contains("AT"));
        assertTrue(names.contains("BOOLEAN_LITERAL"));
        assertTrue(names.contains("NULL_LITERAL"));
    }

    @Test
    void hasFourteenConstants() {
        assertEquals(14, JavaTokenType.values().length);
    }

    @Test
    void newConstantsAreDistinctFromLegacyOnes() {
        assertTrue(JavaTokenType.AT != JavaTokenType.ERROR);
        assertTrue(JavaTokenType.BOOLEAN_LITERAL != JavaTokenType.KEYWORD);
        assertTrue(JavaTokenType.BOOLEAN_LITERAL != JavaTokenType.IDENTIFIER);
        assertTrue(JavaTokenType.NULL_LITERAL != JavaTokenType.KEYWORD);
        assertTrue(JavaTokenType.NULL_LITERAL != JavaTokenType.IDENTIFIER);
    }
}
