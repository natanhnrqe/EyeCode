package com.eyecode.language;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.java.JavaLexer;
import com.eyecode.language.java.JavaTokenType;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaLexerTokenTest {

    private static List<Token> tokenize(String source) {
        return new JavaLexer().tokenize(source);
    }

    private static Token token(JavaTokenType type, int start, int end, String text) {
        return new Token(type, TextRange.of(start, end), text);
    }

    @Test
    void lexesSimpleClassDeclaration() {
        assertEquals(List.of(
                token(JavaTokenType.KEYWORD, 0, 5, "class"),
                token(JavaTokenType.WHITESPACE, 5, 6, " "),
                token(JavaTokenType.IDENTIFIER, 6, 11, "Hello"),
                token(JavaTokenType.EOF, 11, 11, "")
        ), tokenize("class Hello"));
    }

    @Test
    void lexesLineAndBlockComments() {
        assertEquals(List.of(
                token(JavaTokenType.COMMENT, 0, 7, "// line"),
                token(JavaTokenType.WHITESPACE, 7, 8, "\n"),
                token(JavaTokenType.COMMENT, 8, 19, "/* block */"),
                token(JavaTokenType.EOF, 19, 19, "")
        ), tokenize("// line\n/* block */"));
    }

    @Test
    void lexesConditionalStatement() {
        assertEquals(List.of(
                token(JavaTokenType.KEYWORD, 0, 2, "if"),
                token(JavaTokenType.WHITESPACE, 2, 3, " "),
                token(JavaTokenType.SEPARATOR, 3, 4, "("),
                token(JavaTokenType.IDENTIFIER, 4, 9, "value"),
                token(JavaTokenType.WHITESPACE, 9, 10, " "),
                token(JavaTokenType.OPERATOR, 10, 12, ">="),
                token(JavaTokenType.WHITESPACE, 12, 13, " "),
                token(JavaTokenType.NUMBER, 13, 15, "10"),
                token(JavaTokenType.SEPARATOR, 15, 16, ")"),
                token(JavaTokenType.WHITESPACE, 16, 17, " "),
                token(JavaTokenType.SEPARATOR, 17, 18, "{"),
                token(JavaTokenType.WHITESPACE, 18, 19, " "),
                token(JavaTokenType.IDENTIFIER, 19, 24, "value"),
                token(JavaTokenType.OPERATOR, 24, 26, "++"),
                token(JavaTokenType.SEPARATOR, 26, 27, ";"),
                token(JavaTokenType.WHITESPACE, 27, 28, " "),
                token(JavaTokenType.SEPARATOR, 28, 29, "}"),
                token(JavaTokenType.EOF, 29, 29, "")
        ), tokenize("if (value >= 10) { value++; }"));
    }

    @Test
    void lexesGenericTypeReference() {
        assertEquals(List.of(
                token(JavaTokenType.IDENTIFIER, 0, 4, "List"),
                token(JavaTokenType.OPERATOR, 4, 5, "<"),
                token(JavaTokenType.IDENTIFIER, 5, 11, "String"),
                token(JavaTokenType.OPERATOR, 11, 12, ">"),
                token(JavaTokenType.WHITESPACE, 12, 13, " "),
                token(JavaTokenType.IDENTIFIER, 13, 18, "names"),
                token(JavaTokenType.SEPARATOR, 18, 19, ";"),
                token(JavaTokenType.EOF, 19, 19, "")
        ), tokenize("List<String> names;"));
    }

    @Test
    void distinguishesKeywordPrefixes() {
        assertEquals(List.of(
                token(JavaTokenType.KEYWORD, 0, 3, "int"),
                token(JavaTokenType.WHITESPACE, 3, 4, " "),
                token(JavaTokenType.IDENTIFIER, 4, 11, "integer"),
                token(JavaTokenType.WHITESPACE, 11, 12, " "),
                token(JavaTokenType.KEYWORD, 12, 21, "interface"),
                token(JavaTokenType.WHITESPACE, 21, 22, " "),
                token(JavaTokenType.IDENTIFIER, 22, 35, "interfaceName"),
                token(JavaTokenType.EOF, 35, 35, "")
        ), tokenize("int integer interface interfaceName"));
    }

    @Test
    void lexesEmptySource() {
        assertEquals(List.of(
                token(JavaTokenType.EOF, 0, 0, "")
        ), tokenize(""));
    }

    @Test
    void lexesSingleCharacterSource() {
        assertEquals(List.of(
                token(JavaTokenType.IDENTIFIER, 0, 1, "a"),
                token(JavaTokenType.EOF, 1, 1, "")
        ), tokenize("a"));
    }

    @Test
    void lexesTrailingNewline() {
        assertEquals(List.of(
                token(JavaTokenType.IDENTIFIER, 0, 1, "a"),
                token(JavaTokenType.WHITESPACE, 1, 2, "\n"),
                token(JavaTokenType.EOF, 2, 2, "")
        ), tokenize("a\n"));
    }

    @Test
    void lexesCrlfAndLfIdentically() {
        assertEquals(List.of(
                token(JavaTokenType.IDENTIFIER, 0, 1, "a"),
                token(JavaTokenType.WHITESPACE, 1, 3, "\r\n"),
                token(JavaTokenType.IDENTIFIER, 3, 4, "b"),
                token(JavaTokenType.EOF, 4, 4, "")
        ), tokenize("a\r\nb"));

        assertEquals(List.of(
                token(JavaTokenType.IDENTIFIER, 0, 1, "a"),
                token(JavaTokenType.WHITESPACE, 1, 2, "\n"),
                token(JavaTokenType.IDENTIFIER, 2, 3, "b"),
                token(JavaTokenType.EOF, 3, 3, "")
        ), tokenize("a\nb"));
    }

    @Test
    void lexesUnicodeIdentifiers() {
        assertEquals(List.of(
                token(JavaTokenType.KEYWORD, 0, 3, "int"),
                token(JavaTokenType.WHITESPACE, 3, 4, " "),
                token(JavaTokenType.IDENTIFIER, 4, 8, "café"),
                token(JavaTokenType.WHITESPACE, 8, 9, " "),
                token(JavaTokenType.OPERATOR, 9, 10, "="),
                token(JavaTokenType.WHITESPACE, 10, 11, " "),
                token(JavaTokenType.NUMBER, 11, 12, "1"),
                token(JavaTokenType.SEPARATOR, 12, 13, ";"),
                token(JavaTokenType.EOF, 13, 13, "")
        ), tokenize("int café = 1;"));
    }

    @Test
    void toleratesUnterminatedString() {
        assertEquals(List.of(
                token(JavaTokenType.STRING, 0, 4, "\"abc"),
                token(JavaTokenType.EOF, 4, 4, "")
        ), tokenize("\"abc"));
    }

    @Test
    void toleratesUnterminatedBlockComment() {
        assertEquals(List.of(
                token(JavaTokenType.COMMENT, 0, 6, "/* abc"),
                token(JavaTokenType.EOF, 6, 6, "")
        ), tokenize("/* abc"));
    }

    @Test
    void toleratesIncompleteOperator() {
        assertEquals(List.of(
                token(JavaTokenType.NUMBER, 0, 1, "1"),
                token(JavaTokenType.WHITESPACE, 1, 2, " "),
                token(JavaTokenType.OPERATOR, 2, 3, "+"),
                token(JavaTokenType.EOF, 3, 3, "")
        ), tokenize("1 +"));
    }

    @Test
    void toleratesIncompleteDeclaration() {
        assertEquals(List.of(
                token(JavaTokenType.KEYWORD, 0, 3, "int"),
                token(JavaTokenType.WHITESPACE, 3, 4, " "),
                token(JavaTokenType.IDENTIFIER, 4, 5, "x"),
                token(JavaTokenType.EOF, 5, 5, "")
        ), tokenize("int x"));
    }

    @Test
    void lexesAnnotationAsAtFollowedByIdentifier() {
        assertEquals(List.of(
                token(JavaTokenType.AT, 0, 1, "@"),
                token(JavaTokenType.IDENTIFIER, 1, 9, "Override"),
                token(JavaTokenType.WHITESPACE, 9, 10, "\n"),
                token(JavaTokenType.KEYWORD, 10, 16, "public"),
                token(JavaTokenType.WHITESPACE, 16, 17, " "),
                token(JavaTokenType.KEYWORD, 17, 21, "void"),
                token(JavaTokenType.WHITESPACE, 21, 22, " "),
                token(JavaTokenType.IDENTIFIER, 22, 26, "test"),
                token(JavaTokenType.SEPARATOR, 26, 27, "("),
                token(JavaTokenType.SEPARATOR, 27, 28, ")"),
                token(JavaTokenType.WHITESPACE, 28, 29, " "),
                token(JavaTokenType.SEPARATOR, 29, 30, "{"),
                token(JavaTokenType.SEPARATOR, 30, 31, "}"),
                token(JavaTokenType.EOF, 31, 31, "")
        ), tokenize("@Override\npublic void test() {}"));
    }

    @Test
    void lexesBooleanLiterals() {
        assertEquals(List.of(
                token(JavaTokenType.KEYWORD, 0, 7, "boolean"),
                token(JavaTokenType.WHITESPACE, 7, 8, " "),
                token(JavaTokenType.IDENTIFIER, 8, 15, "enabled"),
                token(JavaTokenType.WHITESPACE, 15, 16, " "),
                token(JavaTokenType.OPERATOR, 16, 17, "="),
                token(JavaTokenType.WHITESPACE, 17, 18, " "),
                token(JavaTokenType.BOOLEAN_LITERAL, 18, 22, "true"),
                token(JavaTokenType.SEPARATOR, 22, 23, ";"),
                token(JavaTokenType.EOF, 23, 23, "")
        ), tokenize("boolean enabled = true;"));

        assertEquals(List.of(
                token(JavaTokenType.KEYWORD, 0, 7, "boolean"),
                token(JavaTokenType.WHITESPACE, 7, 8, " "),
                token(JavaTokenType.IDENTIFIER, 8, 16, "disabled"),
                token(JavaTokenType.WHITESPACE, 16, 17, " "),
                token(JavaTokenType.OPERATOR, 17, 18, "="),
                token(JavaTokenType.WHITESPACE, 18, 19, " "),
                token(JavaTokenType.BOOLEAN_LITERAL, 19, 24, "false"),
                token(JavaTokenType.SEPARATOR, 24, 25, ";"),
                token(JavaTokenType.EOF, 25, 25, "")
        ), tokenize("boolean disabled = false;"));
    }

    @Test
    void lexesNullLiteral() {
        assertEquals(List.of(
                token(JavaTokenType.IDENTIFIER, 0, 6, "Object"),
                token(JavaTokenType.WHITESPACE, 6, 7, " "),
                token(JavaTokenType.IDENTIFIER, 7, 12, "value"),
                token(JavaTokenType.WHITESPACE, 12, 13, " "),
                token(JavaTokenType.OPERATOR, 13, 14, "="),
                token(JavaTokenType.WHITESPACE, 14, 15, " "),
                token(JavaTokenType.NULL_LITERAL, 15, 19, "null"),
                token(JavaTokenType.SEPARATOR, 19, 20, ";"),
                token(JavaTokenType.EOF, 20, 20, "")
        ), tokenize("Object value = null;"));
    }

    @Test
    void literalKeywordsAreExactMatches() {
        assertEquals(List.of(
                token(JavaTokenType.IDENTIFIER, 0, 7, "trueish"),
                token(JavaTokenType.WHITESPACE, 7, 8, " "),
                token(JavaTokenType.IDENTIFIER, 8, 15, "nullify"),
                token(JavaTokenType.EOF, 15, 15, "")
        ), tokenize("trueish nullify"));
    }

    @Test
    void lexesNumberVariants() {
        assertEquals(List.of(
                token(JavaTokenType.NUMBER, 0, 7, "12.5e-3"),
                token(JavaTokenType.EOF, 7, 7, "")
        ), tokenize("12.5e-3"));

        assertEquals(List.of(
                token(JavaTokenType.NUMBER, 0, 4, "0x1F"),
                token(JavaTokenType.EOF, 4, 4, "")
        ), tokenize("0x1F"));

        assertEquals(List.of(
                token(JavaTokenType.NUMBER, 0, 5, "0b101"),
                token(JavaTokenType.EOF, 5, 5, "")
        ), tokenize("0b101"));

        assertEquals(List.of(
                token(JavaTokenType.NUMBER, 0, 3, "42L"),
                token(JavaTokenType.EOF, 3, 3, "")
        ), tokenize("42L"));
    }

    @Test
    void lexesCharacterLiteral() {
        assertEquals(List.of(
                token(JavaTokenType.CHARACTER, 0, 3, "'x'"),
                token(JavaTokenType.EOF, 3, 3, "")
        ), tokenize("'x'"));
    }
}
