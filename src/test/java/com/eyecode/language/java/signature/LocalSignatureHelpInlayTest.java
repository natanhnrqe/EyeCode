package com.eyecode.language.java.signature;

import com.eyecode.language.inlay.InlayHint;
import com.eyecode.language.inlay.InlayHintMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalSignatureHelpInlayTest {

    private final LocalSignatureHelpResolver resolver = new LocalSignatureHelpResolver();

    @Test
    void unqualifiedCall_emitsParamNameHintsAtArgumentStarts() {
        String text = source("int result = add(1, 2);");

        List<InlayHint> hints = resolver.resolveInlayHints(text, 0, text.length(), InlayHintMode.NAME);

        assertEquals(2, hints.size());
        int callStart = text.indexOf("add(1, 2)");
        assertEquals(callStart + "add(".length(), hints.get(0).offset());
        assertEquals("left:", hints.get(0).label());
        assertEquals(text.indexOf(", 2)") + ", ".length(), hints.get(1).offset());
        assertEquals("right:", hints.get(1).label());
    }

    @Test
    void declarationParens_areNotHinted() {
        String text = source("int result = 0;");

        List<InlayHint> hints = resolver.resolveInlayHints(text, text.indexOf("int add(int left"),
                text.indexOf("void run()"), InlayHintMode.NAME);

        assertTrue(hints.isEmpty());
    }

    @Test
    void modeType_rendersTypeLabels() {
        String text = source("int result = add(1, 2);");

        List<InlayHint> hints = resolver.resolveInlayHints(text, 0, text.length(), InlayHintMode.TYPE);

        assertEquals(2, hints.size());
        assertEquals("int:", hints.get(0).label());
        assertEquals("int:", hints.get(1).label());
    }

    @Test
    void modeBoth_rendersNameAndTypeTogether() {
        String text = source("int result = add(1, 2);");

        List<InlayHint> hints = resolver.resolveInlayHints(text, 0, text.length(), InlayHintMode.BOTH);

        assertEquals(2, hints.size());
        assertEquals("left: int", hints.get(0).label());
        assertEquals("right: int", hints.get(1).label());
    }

    @Test
    void nullMode_defaultsToName() {
        String text = source("int result = add(1, 2);");

        List<InlayHint> hints = resolver.resolveInlayHints(text, 0, text.length(), null);

        assertEquals(2, hints.size());
        assertEquals("left:", hints.get(0).label());
    }

    @Test
    void rangeFilter_limitsHintsToRequestedWindow() {
        String text = source("int result = add(1, 2);");
        int firstArg = text.indexOf("add(1, 2)") + "add(".length();

        List<InlayHint> hints = resolver.resolveInlayHints(text, firstArg, firstArg + 1, InlayHintMode.NAME);

        assertEquals(1, hints.size());
        assertEquals(firstArg, hints.get(0).offset());
        assertEquals("left:", hints.get(0).label());
    }

    @Test
    void outsideRange_producesNoHints() {
        String text = source("int result = add(1, 2);");
        int callEnd = text.indexOf("add(1, 2)") + "add(1, 2)".length();

        List<InlayHint> hints = resolver.resolveInlayHints(text, callEnd, text.length(), InlayHintMode.NAME);

        assertTrue(hints.isEmpty());
    }

    @Test
    void controlFlowKeywords_produceNoHints() {
        String text = source("if (result > 0) { result = 1; }");

        List<InlayHint> hints = resolver.resolveInlayHints(text, 0, text.length(), InlayHintMode.NAME);

        assertTrue(hints.isEmpty());
    }

    @Test
    void unknownMethod_withoutDatabaseEntry_producesNoHints() {
        String text = source("int result = mysteryFn(1, 2);");

        List<InlayHint> hints = resolver.resolveInlayHints(text, 0, text.length(), InlayHintMode.NAME);

        assertTrue(hints.isEmpty());
    }

    @Test
    void nestedCalls_hintEveryFrameSortedByOffset() {
        String text = source("int result = add(add(1, 2), 3);");

        List<InlayHint> hints = resolver.resolveInlayHints(text, 0, text.length(), InlayHintMode.NAME);

        assertEquals(4, hints.size());
        assertEquals("left:", hints.get(0).label());
        assertEquals(text.indexOf("add(add") + "add(".length(), hints.get(0).offset());
        assertEquals("left:", hints.get(1).label());
        assertEquals("right:", hints.get(2).label());
        assertEquals("right:", hints.get(3).label());
        assertEquals(text.indexOf(", 3)") + ", ".length(), hints.get(3).offset());
        for (int index = 1; index < hints.size(); index++) {
            assertTrue(hints.get(index - 1).offset() < hints.get(index).offset());
        }
    }

    @Test
    void genericArgument_commaInsideAngleBracketsDoesNotShiftParameters() {
        String text = source("int result = add(new HashMap<String, Integer>(), 2);");

        List<InlayHint> hints = resolver.resolveInlayHints(text, 0, text.length(), InlayHintMode.NAME);

        assertEquals(2, hints.size());
        int newOffset = text.indexOf("new HashMap");
        assertEquals(newOffset, hints.get(0).offset());
        assertEquals("left:", hints.get(0).label());
        assertEquals(text.indexOf(", 2)") + ", ".length(), hints.get(1).offset());
        assertEquals("right:", hints.get(1).label());
    }

    @Test
    void lambdaArgument_emitsArgStartAtLambdaParen() {
        String text = source("int result = add(() -> 1, 2);");

        List<InlayHint> hints = resolver.resolveInlayHints(text, 0, text.length(), InlayHintMode.NAME);

        assertEquals(2, hints.size());
        int lambdaParen = text.indexOf("add(()") + "add(".length();
        assertEquals(lambdaParen, hints.get(0).offset());
        assertEquals("left:", hints.get(0).label());
        assertEquals(text.indexOf(", 2)") + ", ".length(), hints.get(1).offset());
        assertEquals("right:", hints.get(1).label());
    }

    @Test
    void qualifiedJdkCall_typeMode_emitsTypeHints() {
        String text = source("text.substring(0, 3);");

        List<InlayHint> hints = resolver.resolveInlayHints(text, 0, text.length(), InlayHintMode.TYPE);

        assertFalse(hints.isEmpty());
        int firstArg = text.indexOf("text.substring(0, 3);") + "text.substring(".length();
        assertEquals(firstArg, hints.get(0).offset());
        assertEquals("int:", hints.get(0).label());
    }

    @Test
    void emptyOrNullSource_returnsEmpty() {
        assertTrue(resolver.resolveInlayHints(null, 0, 10, InlayHintMode.NAME).isEmpty());
        assertTrue(resolver.resolveInlayHints("", 0, 0, InlayHintMode.NAME).isEmpty());
    }

    @Test
    void invertedRange_isNormalized() {
        String text = source("int result = add(1, 2);");

        List<InlayHint> hints = resolver.resolveInlayHints(text, text.length(), 0, InlayHintMode.NAME);

        assertEquals(2, hints.size());
    }

    @Test
    void genericTypeInSource_doesNotBreakHintStack() {
        String text = source("HashMap<String, Integer> map = new HashMap<String, Integer>();");

        List<InlayHint> hints = resolver.resolveInlayHints(text, 0, text.length(), InlayHintMode.NAME);

        assertTrue(hints.isEmpty());
    }

    private static String source(String statement) {
        return "class Calculator {\n"
                + "    /** Adds two values together. */\n"
                + "    int add(int left, int right) {\n"
                + "        return left + right;\n"
                + "    }\n"
                + "\n"
                + "    void run() {\n"
                + "        String text = \"EyeCode\";\n"
                + "        int result = 1;\n"
                + "        " + statement + "\n"
                + "    }\n"
                + "}";
    }
}
