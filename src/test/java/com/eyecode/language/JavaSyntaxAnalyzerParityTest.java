package com.eyecode.language;

import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.syntax.JavaSyntaxAnalyzer;
import com.eyecode.editor.v2.syntax.SyntaxToken;
import com.eyecode.editor.v2.syntax.TokenType;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaSyntaxAnalyzerParityTest {

    private static final Path SOURCE = Path.of("Test.java");

    private static List<SyntaxToken> analyze(String source) {
        EditorDocument document = new EditorDocument(SOURCE, source);
        return new JavaSyntaxAnalyzer().analyze(document).getTokens();
    }

    private static SyntaxToken token(TokenType type, int start, int end, String text) {
        return new SyntaxToken(type, start, end, text);
    }

    @Test
    void classDeclarationSnapshot() {
        assertEquals(List.of(
                token(TokenType.KEYWORD, 0, 5, "class"),
                token(TokenType.WHITESPACE, 5, 6, " "),
                token(TokenType.IDENTIFIER, 6, 11, "Hello"),
                token(TokenType.WHITESPACE, 11, 12, " "),
                token(TokenType.SEPARATOR, 12, 13, "{"),
                token(TokenType.WHITESPACE, 13, 14, " "),
                token(TokenType.KEYWORD, 14, 17, "int"),
                token(TokenType.WHITESPACE, 17, 18, " "),
                token(TokenType.IDENTIFIER, 18, 23, "value"),
                token(TokenType.WHITESPACE, 23, 24, " "),
                token(TokenType.OPERATOR, 24, 25, "="),
                token(TokenType.WHITESPACE, 25, 26, " "),
                token(TokenType.NUMBER, 26, 28, "42"),
                token(TokenType.SEPARATOR, 28, 29, ";"),
                token(TokenType.WHITESPACE, 29, 30, " "),
                token(TokenType.SEPARATOR, 30, 31, "}")
        ), analyze("class Hello { int value = 42; }"));
    }

    @Test
    void conditionalSnapshot() {
        assertEquals(List.of(
                token(TokenType.KEYWORD, 0, 2, "if"),
                token(TokenType.WHITESPACE, 2, 3, " "),
                token(TokenType.SEPARATOR, 3, 4, "("),
                token(TokenType.IDENTIFIER, 4, 9, "value"),
                token(TokenType.WHITESPACE, 9, 10, " "),
                token(TokenType.OPERATOR, 10, 12, ">="),
                token(TokenType.WHITESPACE, 12, 13, " "),
                token(TokenType.NUMBER, 13, 15, "10"),
                token(TokenType.SEPARATOR, 15, 16, ")"),
                token(TokenType.WHITESPACE, 16, 17, " "),
                token(TokenType.SEPARATOR, 17, 18, "{"),
                token(TokenType.WHITESPACE, 18, 19, " "),
                token(TokenType.IDENTIFIER, 19, 24, "value"),
                token(TokenType.OPERATOR, 24, 26, "++"),
                token(TokenType.SEPARATOR, 26, 27, ";"),
                token(TokenType.WHITESPACE, 27, 28, " "),
                token(TokenType.SEPARATOR, 28, 29, "}")
        ), analyze("if (value >= 10) { value++; }"));
    }

    @Test
    void genericTypeSnapshot() {
        assertEquals(List.of(
                token(TokenType.IDENTIFIER, 0, 4, "List"),
                token(TokenType.OPERATOR, 4, 5, "<"),
                token(TokenType.IDENTIFIER, 5, 11, "String"),
                token(TokenType.OPERATOR, 11, 12, ">"),
                token(TokenType.WHITESPACE, 12, 13, " "),
                token(TokenType.IDENTIFIER, 13, 18, "names"),
                token(TokenType.SEPARATOR, 18, 19, ";")
        ), analyze("List<String> names;"));
    }

    @Test
    void commentsSnapshot() {
        assertEquals(List.of(
                token(TokenType.COMMENT, 0, 7, "// note"),
                token(TokenType.WHITESPACE, 7, 8, "\n"),
                token(TokenType.COMMENT, 8, 19, "/* block */")
        ), analyze("// note\n/* block */"));
    }

    @Test
    void annotationSnapshot() {
        assertEquals(List.of(
                token(TokenType.ANNOTATION, 0, 9, "@Override"),
                token(TokenType.WHITESPACE, 9, 10, "\n"),
                token(TokenType.KEYWORD, 10, 16, "public"),
                token(TokenType.WHITESPACE, 16, 17, " "),
                token(TokenType.KEYWORD, 17, 21, "void"),
                token(TokenType.WHITESPACE, 21, 22, " "),
                token(TokenType.IDENTIFIER, 22, 26, "test"),
                token(TokenType.SEPARATOR, 26, 27, "("),
                token(TokenType.SEPARATOR, 27, 28, ")"),
                token(TokenType.WHITESPACE, 28, 29, " "),
                token(TokenType.SEPARATOR, 29, 30, "{"),
                token(TokenType.SEPARATOR, 30, 31, "}")
        ), analyze("@Override\npublic void test() {}"));
    }

    @Test
    void charLiteralStyledAsString() {
        assertEquals(List.of(
                token(TokenType.KEYWORD, 0, 4, "char"),
                token(TokenType.WHITESPACE, 4, 5, " "),
                token(TokenType.IDENTIFIER, 5, 9, "mark"),
                token(TokenType.WHITESPACE, 9, 10, " "),
                token(TokenType.OPERATOR, 10, 11, "="),
                token(TokenType.WHITESPACE, 11, 12, " "),
                token(TokenType.STRING, 12, 15, "'x'"),
                token(TokenType.SEPARATOR, 15, 16, ";")
        ), analyze("char mark = 'x';"));
    }

    @Test
    void booleanLiteralsStyledAsKeyword() {
        assertEquals(List.of(
                token(TokenType.KEYWORD, 0, 7, "boolean"),
                token(TokenType.WHITESPACE, 7, 8, " "),
                token(TokenType.IDENTIFIER, 8, 15, "enabled"),
                token(TokenType.WHITESPACE, 15, 16, " "),
                token(TokenType.OPERATOR, 16, 17, "="),
                token(TokenType.WHITESPACE, 17, 18, " "),
                token(TokenType.KEYWORD, 18, 22, "true"),
                token(TokenType.SEPARATOR, 22, 23, ";")
        ), analyze("boolean enabled = true;"));
    }

    @Test
    void nullLiteralStyledAsKeyword() {
        assertEquals(List.of(
                token(TokenType.IDENTIFIER, 0, 6, "Object"),
                token(TokenType.WHITESPACE, 6, 7, " "),
                token(TokenType.IDENTIFIER, 7, 12, "value"),
                token(TokenType.WHITESPACE, 12, 13, " "),
                token(TokenType.OPERATOR, 13, 14, "="),
                token(TokenType.WHITESPACE, 14, 15, " "),
                token(TokenType.KEYWORD, 15, 19, "null"),
                token(TokenType.SEPARATOR, 19, 20, ";")
        ), analyze("Object value = null;"));
    }

    @Test
    void stringLiteralSnapshot() {
        assertEquals(List.of(
                token(TokenType.IDENTIFIER, 0, 6, "String"),
                token(TokenType.WHITESPACE, 6, 7, " "),
                token(TokenType.IDENTIFIER, 7, 8, "s"),
                token(TokenType.WHITESPACE, 8, 9, " "),
                token(TokenType.OPERATOR, 9, 10, "="),
                token(TokenType.WHITESPACE, 10, 11, " "),
                token(TokenType.STRING, 11, 15, "\"hi\""),
                token(TokenType.SEPARATOR, 15, 16, ";")
        ), analyze("String s = \"hi\";"));
    }

    @Test
    void hexNumberSnapshot() {
        assertEquals(List.of(
                token(TokenType.NUMBER, 0, 4, "0x1F")
        ), analyze("0x1F"));
    }

    @Test
    void keywordPrefixesSnapshot() {
        assertEquals(List.of(
                token(TokenType.KEYWORD, 0, 3, "int"),
                token(TokenType.WHITESPACE, 3, 4, " "),
                token(TokenType.IDENTIFIER, 4, 11, "integer"),
                token(TokenType.WHITESPACE, 11, 12, " "),
                token(TokenType.KEYWORD, 12, 21, "interface"),
                token(TokenType.WHITESPACE, 21, 22, " "),
                token(TokenType.IDENTIFIER, 22, 35, "interfaceName")
        ), analyze("int integer interface interfaceName"));
    }

    @Test
    void unterminatedStringSnapshot() {
        assertEquals(List.of(
                token(TokenType.STRING, 0, 4, "\"abc")
        ), analyze("\"abc"));
    }
}
