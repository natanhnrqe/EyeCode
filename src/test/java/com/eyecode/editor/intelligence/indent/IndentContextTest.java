package com.eyecode.editor.intelligence.indent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the {@link IndentContext} lexical scan: bracket nesting must count
 * braces only outside string literals, character literals, line comments and
 * block comments, and per-line significant-character facts must be reported.
 */
class IndentContextTest {

    @Test
    void tracksSimpleBlockNesting() {
        IndentContext context = IndentContext.of("{\n    x;\n}\n");
        assertEquals(0, context.blockDepthAtLineStart(0));
        assertEquals(1, context.blockDepthAtLineStart(1));
        assertEquals(1, context.blockDepthAtLineStart(2));
        assertEquals(0, context.blockDepthAtLineStart(3));
        assertEquals(1, context.lineNetBraceDelta(0));
        assertEquals(-1, context.lineNetBraceDelta(2));
    }

    @Test
    void ignoresBracesInsideDoubleQuotedStrings() {
        IndentContext context = IndentContext.of("String s = \"{\";\n");
        assertEquals(0, context.blockDepthAtLineStart(1));
        assertEquals(0, context.lineNetBraceDelta(0));
    }

    @Test
    void ignoresBracesInsideSingleQuotedChars() {
        IndentContext context = IndentContext.of("char c = '}';\n");
        assertEquals(0, context.lineNetBraceDelta(0));
        assertFalse(context.lineEndsWithOpenBrace(0));
    }

    @Test
    void ignoresEscapedQuotes() {
        IndentContext context = IndentContext.of("String s = \"a\\\"{b\";\n");
        assertEquals(0, context.lineNetBraceDelta(0));
    }

    @Test
    void ignoresBracesInLineComments() {
        IndentContext context = IndentContext.of("// { not a block\n{\n");
        assertEquals(0, context.lineNetBraceDelta(0));
        assertEquals(1, context.lineNetBraceDelta(1));
        assertFalse(context.lineEndsWithOpenBrace(0));
    }

    @Test
    void ignoresBracesInBlockCommentsAcrossLines() {
        IndentContext context = IndentContext.of("/* {\n} */\n{\n");
        assertEquals(0, context.lineNetBraceDelta(0));
        assertEquals(0, context.lineNetBraceDelta(1));
        assertEquals(1, context.lineNetBraceDelta(2));
        assertTrue(context.inBlockComment(1));
        assertFalse(context.inBlockComment(2));
    }

    @Test
    void closingCommentRestoresNormalState() {
        IndentContext context = IndentContext.of("/* x */ {\n");
        assertEquals(1, context.lineNetBraceDelta(0));
        assertFalse(context.inBlockComment(0));
    }

    @Test
    void lineEndsWithOpenBrace() {
        IndentContext context = IndentContext.of("if (x) {\n    foo(\n");
        assertTrue(context.lineEndsWithOpenBrace(0));
        assertFalse(context.lineEndsWithOpenBrace(1));
        assertTrue(context.lineEndsWithOpenParen(1));
        assertFalse(context.lineEndsWithOpenParen(0));
    }

    @Test
    void lineEndsWithComma() {
        IndentContext context = IndentContext.of("foo(a,\n    b);\n");
        assertTrue(context.lineEndsWithComma(0));
        assertFalse(context.lineEndsWithComma(1));
    }

    @Test
    void lineStartsWithClosingBrace() {
        IndentContext context = IndentContext.of("{\n    }\n");
        assertTrue(context.lineStartsWithClosingBrace(1));
        assertFalse(context.lineStartsWithClosingBrace(0));
    }

    @Test
    void blankLineDetection() {
        IndentContext context = IndentContext.of("    \nfoo\n");
        assertTrue(context.lineIsBlank(0));
        assertFalse(context.lineIsBlank(1));
    }

    @Test
    void parenDepthIsTrackedSeparately() {
        IndentContext context = IndentContext.of("foo(\n    x\n);\n");
        assertEquals(1, context.parenDepthAtLineStart(1));
        assertEquals(0, context.parenDepthAtLineStart(3));
        assertEquals(0, context.lineNetBraceDelta(1));
    }

    @Test
    void stringStateIsReportedPerLine() {
        IndentContext context = IndentContext.of("String a = \"unterminated\n");
        assertTrue(context.inString(1));
    }

    @Test
    void closedStringDoesNotLeakState() {
        IndentContext context = IndentContext.of("String a = \"x\";\nfoo\n");
        assertFalse(context.inString(1));
        assertEquals(0, context.lineNetBraceDelta(1));
    }

    @Test
    void crlfLineTerminatorsAreHandled() {
        IndentContext context = IndentContext.of("{\r\n    x;\r\n}\r\n");
        assertEquals(1, context.blockDepthAtLineStart(1));
        assertEquals(1, context.blockDepthAtLineStart(2));
        assertEquals(0, context.blockDepthAtLineStart(3));
    }

    @Test
    void emptyTextHasSingleBlankLine() {
        IndentContext context = IndentContext.of("");
        assertEquals(1, context.lineCount());
        assertTrue(context.lineIsBlank(0));
    }
}
