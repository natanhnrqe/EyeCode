package com.eyecode.editor.intelligence.indent;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link JavaIndentPolicy}: canonical next-line levels derived from
 * bracket nesting, continuation rules, switch labels and dedent detection.
 */
class JavaIndentPolicyTest {

    private final JavaIndentPolicy policy = JavaIndentPolicy.INSTANCE;

    private static DocumentSnapshot snapshot(String text) {
        return new DocumentSnapshot(1, text, null, null);
    }

    @Test
    void indentationForUsesFourSpaceUnits() {
        assertEquals("", policy.indentationFor(0));
        assertEquals("    ", policy.indentationFor(1));
        assertEquals("        ", policy.indentationFor(2));
        assertEquals("", policy.indentationFor(-1));
    }

    @Test
    void indentationLevelCountsSpacesAndTabs() {
        assertEquals(0, policy.indentationLevel("foo();", 0));
        assertEquals(1, policy.indentationLevel("    foo();", 0));
        assertEquals(2, policy.indentationLevel("        foo();", 0));
        assertEquals(0, policy.indentationLevel("  foo();", 0));
        assertEquals(1, policy.indentationLevel("\tfoo();", 0));
        assertEquals(2, policy.indentationLevel("        foo();", 0));
        assertEquals(0, policy.indentationLevel("x\nfoo();", 1));
    }

    @Test
    void currentLineIndentLevelReadsSnapshot() {
        DocumentSnapshot snapshot = snapshot("    int x;\n");
        assertEquals(1, policy.currentLineIndentLevel(snapshot, 0));
        assertEquals(0, policy.currentLineIndentLevel(snapshot, 1));
    }

    @Test
    void nextLineLevelAfterOpenBraceIncreases() {
        DocumentSnapshot snapshot = snapshot("class A {\n");
        assertEquals(1, policy.nextLineIndentLevel(snapshot, 0));
    }

    @Test
    void nextLineLevelInsideNestedBlocks() {
        DocumentSnapshot snapshot = snapshot(
                "class A {\n"
                        + "    void m() {\n"
                        + "        int x;\n"
                        + "    }\n"
                        + "}\n");
        assertEquals(1, policy.nextLineIndentLevel(snapshot, 0));
        assertEquals(2, policy.nextLineIndentLevel(snapshot, 1));
        assertEquals(2, policy.nextLineIndentLevel(snapshot, 2));
        assertEquals(1, policy.nextLineIndentLevel(snapshot, 3));
        assertEquals(0, policy.nextLineIndentLevel(snapshot, 4));
    }

    @Test
    void closingBraceLineDedents() {
        DocumentSnapshot snapshot = snapshot(
                "void m() {\n"
                        + "    if (x) {\n"
                        + "        y();\n"
                        + "    }\n");
        assertEquals(1, policy.nextLineIndentLevel(snapshot, 3));
    }

    @Test
    void elseAfterClosingBraceKeepsLevel() {
        DocumentSnapshot snapshot = snapshot(
                "if (x) {\n"
                        + "    y();\n"
                        + "} else {\n"
                        + "    z();\n"
                        + "}\n");
        assertEquals(1, policy.nextLineIndentLevel(snapshot, 2));
        assertEquals(1, policy.nextLineIndentLevel(snapshot, 3));
    }

    @Test
    void switchLabelsAddOneLevel() {
        DocumentSnapshot snapshot = snapshot(
                "switch (x) {\n"
                        + "    case 1:\n"
                        + "        break;\n"
                        + "    default:\n"
                        + "}\n");
        assertEquals(2, policy.nextLineIndentLevel(snapshot, 1));
        assertEquals(2, policy.nextLineIndentLevel(snapshot, 2));
        assertEquals(2, policy.nextLineIndentLevel(snapshot, 3));
    }

    @Test
    void bracesInsideStringsAreIgnored() {
        DocumentSnapshot snapshot = snapshot("String s = \"{\";\n");
        assertEquals(0, policy.nextLineIndentLevel(snapshot, 0));
    }

    @Test
    void openParenContinuationAddsLevel() {
        DocumentSnapshot snapshot = snapshot("foo(\n");
        assertEquals(1, policy.nextLineIndentLevel(snapshot, 0));
    }

    @Test
    void trailingCommaContinuationAddsLevel() {
        DocumentSnapshot snapshot = snapshot("    int[] x = {1, 2,\n");
        assertEquals(2, policy.nextLineIndentLevel(snapshot, 0));
    }

    @Test
    void completedCallReturnsToMethodLevel() {
        DocumentSnapshot snapshot = snapshot(
                "void m() {\n"
                        + "    foo(a,\n"
                        + "        b);\n"
                        + "}\n");
        assertEquals(2, policy.nextLineIndentLevel(snapshot, 1));
        assertEquals(1, policy.nextLineIndentLevel(snapshot, 2));
    }

    @Test
    void shouldDedentForClosingBraceLines() {
        DocumentSnapshot snapshot = snapshot(
                "{\n"
                        + "    x();\n"
                        + "}\n");
        assertFalse(policy.shouldDedent(snapshot, 0));
        assertTrue(policy.shouldDedent(snapshot, 2));
    }

    @Test
    void bareIndentedLineCarriesItsIndent() {
        DocumentSnapshot snapshot = snapshot("    int x = 1;\n");
        assertEquals(1, policy.nextLineIndentLevel(snapshot, 0));
    }

    @Test
    void unterminatedBraceAtLineEndCountsDespiteTrailingContent() {
        DocumentSnapshot snapshot = snapshot("void m() { foo();\n");
        assertEquals(1, policy.nextLineIndentLevel(snapshot, 0));
    }
}
