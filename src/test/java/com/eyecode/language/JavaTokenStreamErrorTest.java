package com.eyecode.language;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.parser.ParserException;
import com.eyecode.language.java.JavaTokenType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaTokenStreamErrorTest {

    private static JavaTokenStream stream(String source) {
        JavaLexerService service = new JavaLexerService();
        return new JavaTokenStream(
                service.lex(DocumentSnapshot.oneShot(source)).tokens(), source);
    }

    @Test
    void expectReportsLineAndColumnForLfSource() {
        JavaTokenStream stream = stream("int x = 1;\npublic");
        stream.expect(JavaTokenType.KEYWORD, "int");
        stream.expect(JavaTokenType.WHITESPACE);
        stream.expect(JavaTokenType.IDENTIFIER, "x");
        stream.expect(JavaTokenType.WHITESPACE);
        stream.expect(JavaTokenType.OPERATOR, "=");
        stream.expect(JavaTokenType.WHITESPACE);
        stream.expect(JavaTokenType.NUMBER, "1");
        stream.expect(JavaTokenType.SEPARATOR, ";");
        stream.expect(JavaTokenType.WHITESPACE);

        ParserException ex = assertThrows(ParserException.class,
                () -> stream.expect(JavaTokenType.IDENTIFIER));

        assertEquals(1, ex.getLine());
        assertEquals(0, ex.getColumn());
        assertEquals("public", ex.getFoundToken());
        assertEquals("IDENTIFIER", ex.getExpectedToken());
    }

    @Test
    void expectReportsLineAndColumnForCrlfSource() {
        JavaTokenStream stream = stream("int x;\r\npublic");
        stream.expect(JavaTokenType.KEYWORD, "int");
        stream.expect(JavaTokenType.WHITESPACE);
        stream.expect(JavaTokenType.IDENTIFIER, "x");
        stream.expect(JavaTokenType.SEPARATOR, ";");
        stream.expect(JavaTokenType.WHITESPACE);

        ParserException ex = assertThrows(ParserException.class,
                () -> stream.expect(JavaTokenType.IDENTIFIER));

        assertEquals(1, ex.getLine());
        assertEquals(0, ex.getColumn());
        assertEquals("public", ex.getFoundToken());
    }

    @Test
    void expectReportsDeepLinePosition() {
        JavaTokenStream stream = stream("line1\nline2\nclass");
        stream.expect(JavaTokenType.IDENTIFIER);
        stream.expect(JavaTokenType.WHITESPACE);
        stream.expect(JavaTokenType.IDENTIFIER);
        stream.expect(JavaTokenType.WHITESPACE);

        ParserException ex = assertThrows(ParserException.class,
                () -> stream.expect(JavaTokenType.SEPARATOR, ";"));

        assertEquals(2, ex.getLine());
        assertEquals(0, ex.getColumn());
        assertEquals("class", ex.getFoundToken());
        assertEquals("SEPARATOR \";\"", ex.getExpectedToken());
    }

    @Test
    void expectFailureAtStartOfSource() {
        JavaTokenStream stream = stream("class Foo {}");

        ParserException ex = assertThrows(ParserException.class,
                () -> stream.expect(JavaTokenType.SEPARATOR, ","));

        assertEquals(0, ex.getLine());
        assertEquals(0, ex.getColumn());
        assertEquals("class", ex.getFoundToken());
        assertEquals("SEPARATOR \",\"", ex.getExpectedToken());
    }

    @Test
    void expectFailureMidSource() {
        JavaTokenStream stream = stream("class Foo {}");
        stream.expect(JavaTokenType.KEYWORD, "class");
        stream.expect(JavaTokenType.WHITESPACE);
        stream.expect(JavaTokenType.IDENTIFIER, "Foo");
        stream.expect(JavaTokenType.WHITESPACE);

        ParserException ex = assertThrows(ParserException.class,
                () -> stream.expect(JavaTokenType.SEPARATOR, ","));

        assertEquals(0, ex.getLine());
        assertEquals(10, ex.getColumn());
        assertEquals("{", ex.getFoundToken());
        assertEquals("SEPARATOR \",\"", ex.getExpectedToken());
    }

    @Test
    void messageIncludesPositionDetails() {
        JavaTokenStream stream = stream("int x = 1;\npublic");
        stream.expect(JavaTokenType.KEYWORD, "int");
        stream.expect(JavaTokenType.WHITESPACE);
        stream.expect(JavaTokenType.IDENTIFIER, "x");
        stream.expect(JavaTokenType.WHITESPACE);
        stream.expect(JavaTokenType.OPERATOR, "=");
        stream.expect(JavaTokenType.WHITESPACE);
        stream.expect(JavaTokenType.NUMBER, "1");
        stream.expect(JavaTokenType.SEPARATOR, ";");
        stream.expect(JavaTokenType.WHITESPACE);

        ParserException ex = assertThrows(ParserException.class,
                () -> stream.expect(JavaTokenType.IDENTIFIER));

        assertTrue(ex.getMessage().contains("line=1"));
        assertTrue(ex.getMessage().contains("column=0"));
        assertTrue(ex.getMessage().contains("expected=IDENTIFIER"));
        assertTrue(ex.getMessage().contains("found=public"));
    }

    @Test
    void matchAndConsumeWorkOnCanonicalTokens() {
        JavaTokenStream stream = stream("class Foo {}");

        assertEquals(JavaTokenType.KEYWORD, stream.peek().type());
        assertTrue(stream.match(JavaTokenType.KEYWORD, "class"));
        assertEquals(" ", stream.consume().text());
        assertEquals("Foo", stream.consume().text());
        assertEquals(" ", stream.consume().text());
        assertTrue(stream.match(JavaTokenType.SEPARATOR, "{"));
        assertTrue(stream.match(JavaTokenType.SEPARATOR, "}"));
        assertTrue(stream.isEOF());
    }
}
