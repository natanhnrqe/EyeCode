package com.eyecode.language.java.doc;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalJavadocResolverTest {

    private final LocalJavadocResolver resolver = new LocalJavadocResolver();

    @Test
    void methodUsage_resolvesDeclarationJavadoc() {
        String marked = source("int sum = ad|d(1, 2);");

        Optional<LocalJavadocResolver.JavadocAtCaret> result = resolver.atCaret(text(marked), caret(marked));

        assertTrue(result.isPresent());
        assertEquals("add", result.get().symbol());
        assertTrue(result.get().declared());
        assertFalse(result.get().variable());
        assertTrue(result.get().javadoc().contains("Adds two values together."));
        assertEquals("add", text(marked).substring(result.get().rangeStart(), result.get().rangeEnd()));
    }

    @Test
    void fieldUsage_resolvesDeclarationJavadoc() {
        String marked = source("int total = coun|ter;");

        Optional<LocalJavadocResolver.JavadocAtCaret> result = resolver.atCaret(text(marked), caret(marked));

        assertTrue(result.isPresent());
        assertEquals("counter", result.get().symbol());
        assertTrue(result.get().declared());
        assertTrue(result.get().javadoc().contains("Running total."));
    }

    @Test
    void localVariableUsage_isFlaggedAsVariable() {
        String marked = source("int outcome = res|ult;");

        Optional<LocalJavadocResolver.JavadocAtCaret> result = resolver.atCaret(text(marked), caret(marked));

        assertTrue(result.isPresent());
        assertEquals("result", result.get().symbol());
        assertFalse(result.get().declared());
        assertTrue(result.get().variable());
        assertTrue(result.get().javadoc().isBlank());
    }

    @Test
    void parameterUsage_isFlaggedAsVariable() {
        String source = "class Calculator {\n"
                + "    int add(int left, int right) {\n"
                + "        return le|ft + right;\n"
                + "    }\n"
                + "}";
        int offset = source.indexOf('|');

        Optional<LocalJavadocResolver.JavadocAtCaret> result = resolver.atCaret(source.replace("|", ""), offset);

        assertTrue(result.isPresent());
        assertEquals("left", result.get().symbol());
        assertTrue(result.get().variable());
    }

    @Test
    void ordinaryCommentBeforeDeclaration_isNotJavadoc() {
        String source = "class A {\n"
                + "    // just a note\n"
                + "    int value;\n"
                + "    void run() { int x = va|lue; }\n"
                + "}";
        int offset = source.indexOf('|');

        Optional<LocalJavadocResolver.JavadocAtCaret> result = resolver.atCaret(source.replace("|", ""), offset);

        assertTrue(result.isPresent());
        assertEquals("value", result.get().symbol());
        assertTrue(result.get().declared());
        assertTrue(result.get().javadoc().isBlank());
    }

    @Test
    void typeUsage_resolvesTypeJavadoc() {
        String marked = source("Ca|lculator instance = null;");

        Optional<LocalJavadocResolver.JavadocAtCaret> result = resolver.atCaret(text(marked), caret(marked));

        assertTrue(result.isPresent());
        assertEquals("Calculator", result.get().symbol());
        assertTrue(result.get().declared());
        assertTrue(result.get().javadoc().contains("Simple calculator."));
    }

    @Test
    void keywordAtCaret_isStillResolvedAsSymbol() {
        String marked = source("swi|tch (result) {");

        Optional<LocalJavadocResolver.JavadocAtCaret> result = resolver.atCaret(text(marked), caret(marked));

        assertTrue(result.isPresent());
        assertEquals("switch", result.get().symbol());
        assertFalse(result.get().declared());
        assertFalse(result.get().variable());
    }

    @Test
    void caretInWhitespace_returnsEmpty() {
        String marked = source("int sum = add(1, |2);");

        assertTrue(resolver.atCaret(text(marked), caret(marked)).isEmpty());
    }

    @Test
    void caretInComment_returnsEmpty() {
        String source = "class A {\n    // note her|e\n    void run() {}\n}";
        assertTrue(resolver.atCaret(source.replace("|", ""), source.indexOf('|')).isEmpty());
    }

    @Test
    void emptySource_returnsEmpty() {
        assertTrue(resolver.atCaret(null, 0).isEmpty());
        assertTrue(resolver.atCaret("", 0).isEmpty());
    }

    @Test
    void clean_stripsDecorationAndAsterisks() {
        assertEquals("Line one.\nLine two.",
                LocalJavadocResolver.clean("/**\n * Line one.\n * Line two.\n */"));
    }

    private static String source(String statement) {
        return "/** Simple calculator. */\n"
                + "class Calculator {\n"
                + "    /** Running total. */\n"
                + "    int counter;\n"
                + "\n"
                + "    /** Adds two values together. */\n"
                + "    int add(int left, int right) {\n"
                + "        int result = 1;\n"
                + "        int helperValue = 0;\n"
                + "        " + statement + "\n"
                + "        return helperValue;\n"
                + "    }\n"
                + "}";
    }

    private static String text(String marked) {
        return marked.replace("|", "");
    }

    private static int caret(String marked) {
        return marked.indexOf('|');
    }
}
