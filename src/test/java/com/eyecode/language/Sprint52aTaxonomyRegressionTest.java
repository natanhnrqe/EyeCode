package com.eyecode.language;

import com.eyecode.language.java.JavaLexer;
import com.eyecode.language.java.JavaTokenType;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class Sprint52aTaxonomyRegressionTest {

    private static List<Token> tokenize(String source) {
        return new JavaLexer().tokenize(source);
    }

    @Test
    void atSignIsNeverAnErrorToken() {
        List<Token> tokens = tokenize("@Override");

        assertEquals(JavaTokenType.AT, tokens.get(0).type());
        assertNotEquals(JavaTokenType.ERROR, tokens.get(0).type());
        assertEquals("@", tokens.get(0).text());
    }

    @Test
    void trueIsBooleanLiteralNotKeyword() {
        List<Token> tokens = tokenize("boolean enabled = true;");
        Token literal = tokens.stream()
                .filter(t -> t.text().equals("true"))
                .findFirst()
                .orElseThrow();

        assertEquals(JavaTokenType.BOOLEAN_LITERAL, literal.type());
        assertNotEquals(JavaTokenType.KEYWORD, literal.type());
        assertNotEquals(JavaTokenType.IDENTIFIER, literal.type());
    }

    @Test
    void falseIsBooleanLiteralNotKeyword() {
        List<Token> tokens = tokenize("boolean disabled = false;");
        Token literal = tokens.stream()
                .filter(t -> t.text().equals("false"))
                .findFirst()
                .orElseThrow();

        assertEquals(JavaTokenType.BOOLEAN_LITERAL, literal.type());
        assertNotEquals(JavaTokenType.KEYWORD, literal.type());
        assertNotEquals(JavaTokenType.IDENTIFIER, literal.type());
    }

    @Test
    void nullIsNullLiteralNotKeyword() {
        List<Token> tokens = tokenize("Object value = null;");
        Token literal = tokens.stream()
                .filter(t -> t.text().equals("null"))
                .findFirst()
                .orElseThrow();

        assertEquals(JavaTokenType.NULL_LITERAL, literal.type());
        assertNotEquals(JavaTokenType.KEYWORD, literal.type());
        assertNotEquals(JavaTokenType.IDENTIFIER, literal.type());
    }

    @Test
    void keywordsStillWorkAfterUnification() {
        List<Token> tokens = tokenize("public class Hello {}");

        assertEquals(JavaTokenType.KEYWORD, tokens.get(0).type());
        assertEquals(JavaTokenType.KEYWORD, tokens.get(2).type());
        assertEquals(JavaTokenType.IDENTIFIER, tokens.get(4).type());
    }
}
