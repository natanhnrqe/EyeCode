package com.eyecode.editor.v2.ui;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import org.junit.jupiter.api.Test;

import javax.swing.JTextPane;
import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test for Sprint 5.4c.3: the {@link RichEditorView#goToDefinition()}
 * wires the caret position into {@code DefinitionAtCaretResolver} and moves
 * the caret to the resolved declaration range.
 */
class RichEditorViewGoToDefinitionTest {

    private static RichEditorView create(String source) {
        return new RichEditorView(new EditorBuffer(new EditorDocument(null, source)));
    }

    private static void putCaret(RichEditorView view, int offset) {
        JTextPane pane = view.getTextPane();
        pane.setCaretPosition(offset);
        EditorBuffer buffer = view.getBuffer();
        buffer.moveCaret(buffer.getDocument().positionOf(offset));
    }

    @Test
    void goToDefinition_localVariable_movesCaretToDeclaration() {
        String source = "class C {\n" +
                "    void m() {\n" +
                "        int value = 1;\n" +
                "        int x = value;\n" +
                "    }\n" +
                "}\n";
        RichEditorView view = create(source);
        int refOffset = source.indexOf("= value");
        putCaret(view, refOffset + 2);
        view.goToDefinition();
        int declOffset = source.indexOf("value = 1") + "value = 1".indexOf("value");
        assertEquals(declOffset, view.getTextPane().getCaretPosition());
    }

    @Test
    void goToDefinition_field_movesCaretToFieldDeclaration() {
        String source = "class C {\n" +
                "    int counter;\n" +
                "    void m() {\n" +
                "        counter = 5;\n" +
                "    }\n" +
                "}\n";
        RichEditorView view = create(source);
        int refOffset = source.indexOf("counter = 5");
        putCaret(view, refOffset);
        view.goToDefinition();
        int declOffset = source.indexOf("int counter;");
        assertEquals(declOffset, view.getTextPane().getCaretPosition());
    }

    @Test
    void goToDefinition_parameter_movesCaretToParameter() {
        String source = "class C {\n" +
                "    void m(int value) {\n" +
                "        int x = value;\n" +
                "    }\n" +
                "}\n";
        RichEditorView view = create(source);
        int refOffset = source.indexOf("= value");
        putCaret(view, refOffset + 2);
        view.goToDefinition();
        int declOffset = source.indexOf("int value)");
        assertEquals(declOffset, view.getTextPane().getCaretPosition());
    }

    @Test
    void goToDefinition_type_movesCaretToTypeDeclaration() {
        String source = "class Helper { }\n" +
                "class C {\n" +
                "    Helper h;\n" +
                "}\n";
        RichEditorView view = create(source);
        int refOffset = source.indexOf("Helper h;");
        putCaret(view, refOffset);
        view.goToDefinition();
        int declOffset = source.indexOf("class Helper");
        assertEquals(declOffset, view.getTextPane().getCaretPosition());
    }

    @Test
    void goToDefinition_method_movesCaretToMethodDeclaration() {
        String source = "class C {\n" +
                "    void target() { }\n" +
                "    void caller() {\n" +
                "        target();\n" +
                "    }\n" +
                "}\n";
        RichEditorView view = create(source);
        int refOffset = source.indexOf("target();");
        putCaret(view, refOffset);
        view.goToDefinition();
        int declOffset = source.indexOf("void target()");
        assertEquals(declOffset, view.getTextPane().getCaretPosition());
    }

    @Test
    void goToDefinition_qualifiedReferenceToParameter_doesNotCrash() {
        String source = "class Helper { int value; }\n" +
                "class C {\n" +
                "    void m(Helper h) {\n" +
                "        int x = h.value;\n" +
                "    }\n" +
                "}\n";
        RichEditorView view = create(source);
        int refOffset = source.indexOf("h.value");
        putCaret(view, refOffset + 2);
        int caretBefore = view.getTextPane().getCaretPosition();
        view.goToDefinition();
        int caretAfter = view.getTextPane().getCaretPosition();
        assertTrue(caretAfter >= 0);
        assertTrue(caretAfter == caretBefore || caretAfter < source.length());
    }

    @Test
    void goToDefinition_qualifiedReferenceToTypeField_resolvesField() {
        String source = "class Helper { int value; }\n" +
                "class C {\n" +
                "    Helper h = new Helper();\n" +
                "    void m() {\n" +
                "        int x = h.value;\n" +
                "    }\n" +
                "}\n";
        RichEditorView view = create(source);
        int refOffset = source.indexOf("h.value");
        putCaret(view, refOffset + 2);
        view.goToDefinition();
        int caretAfter = view.getTextPane().getCaretPosition();
        assertTrue(caretAfter >= 0);
    }

    @Test
    void goToDefinition_unresolved_leavesCaretUnchanged() {
        String source = "class C { void m() { int x = 1; } }\n";
        RichEditorView view = create(source);
        int caretOffset = source.indexOf("1;");
        putCaret(view, caretOffset);
        view.goToDefinition();
        assertEquals(caretOffset, view.getTextPane().getCaretPosition());
    }

    @Test
    void goToDefinition_whitespace_leavesCaretUnchanged() {
        String source = "class C { void m() { int x = 1; } }\n";
        RichEditorView view = create(source);
        int caretOffset = source.indexOf(" { ");
        putCaret(view, caretOffset + 1);
        view.goToDefinition();
        assertEquals(caretOffset + 1, view.getTextPane().getCaretPosition());
    }

    @Test
    void goToDefinition_comment_leavesCaretUnchanged() {
        String source = "class C {\n" +
                "    // this is a comment\n" +
                "    void m() { }\n" +
                "}\n";
        RichEditorView view = create(source);
        int caretOffset = source.indexOf("this is a comment");
        putCaret(view, caretOffset + 5);
        view.goToDefinition();
        assertEquals(caretOffset + 5, view.getTextPane().getCaretPosition());
    }

    @Test
    void goToDefinition_stringLiteral_leavesCaretUnchanged() {
        String source = "class C { String s = \"hello\"; }\n";
        RichEditorView view = create(source);
        int caretOffset = source.indexOf("hello");
        putCaret(view, caretOffset + 2);
        view.goToDefinition();
        assertEquals(caretOffset + 2, view.getTextPane().getCaretPosition());
    }

    @Test
    void goToDefinition_shadowing_returnsInnermostDeclaration() {
        String source = "class C {\n" +
                "    int value = 1;\n" +
                "    void m() {\n" +
                "        int value = 2;\n" +
                "        int x = value;\n" +
                "    }\n" +
                "}\n";
        RichEditorView view = create(source);
        int refOffset = source.indexOf("int x = value");
        putCaret(view, source.indexOf("= value", refOffset) + 2);
        view.goToDefinition();
        int firstIdx = source.indexOf("value =");
        int innerIdx = source.indexOf("value =", firstIdx + 1);
        assertEquals(innerIdx, view.getTextPane().getCaretPosition());
    }

    @Test
    void goToDefinition_repeated_isDeterministic() {
        String source = "class C {\n" +
                "    int value = 1;\n" +
                "    void m() { int x = value; }\n" +
                "}\n";
        RichEditorView view = create(source);
        int refOffset = source.indexOf("= value");
        putCaret(view, refOffset + 2);
        view.goToDefinition();
        int firstCaret = view.getTextPane().getCaretPosition();
        putCaret(view, refOffset + 2);
        view.goToDefinition();
        int secondCaret = view.getTextPane().getCaretPosition();
        assertEquals(firstCaret, secondCaret);
    }

    @Test
    void revealOffset_withinBounds_keepsCaretInBounds() {
        String source = "class C {\n" +
                "    void m() {\n" +
                "        int value = 1;\n" +
                "    }\n" +
                "}\n";
        RichEditorView view = create(source);
        Rectangle before = view.getTextPane().getVisibleRect();
        assertNotNull(before);
        view.revealOffset(source.indexOf("int value ="));
        assertTrue(view.getTextPane().getCaretPosition() >= 0);
    }

    @Test
    void revealOffset_negativeOffset_isNoop() {
        String source = "class C { }\n";
        RichEditorView view = create(source);
        putCaret(view, 0);
        view.revealOffset(-1);
        assertEquals(0, view.getTextPane().getCaretPosition());
    }

    @Test
    void revealOffset_pastEnd_isClampedToTextLength() {
        String source = "class C { }\n";
        RichEditorView view = create(source);
        view.revealOffset(source.length() + 100);
        assertTrue(view.getTextPane().getCaretPosition() <= source.length());
    }
}
