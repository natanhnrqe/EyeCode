package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent.EditorModifier;
import com.eyecode.editor.intelligence.pipeline.SmartEditingRegistry;
import com.eyecode.editor.intelligence.pipeline.SmartEditResult;
import com.eyecode.editor.intelligence.pipeline.TypingPipeline;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorPosition;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link AutoIndentStrategy} in isolation: plain Enter inserts a
 * newline plus the policy-computed indentation, dedents after closing braces,
 * and never claims modified key presses, Enter key-typed events or active
 * selections (those belong to {@link SmartEnterStrategy}).
 */
class AutoIndentStrategyTest {

    private static TypingPipeline pipeline() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(new AutoIndentStrategy());
        return new TypingPipeline(registry);
    }

    private static EditorBuffer buffer(String text) {
        return new EditorBuffer(new EditorDocument(null, text));
    }

    private static void caretAtEnd(EditorBuffer buffer) {
        buffer.moveCaret(buffer.getDocument().positionOf(buffer.getDocument().getText().length()));
    }

    private static void caretAt(EditorBuffer buffer, int offset) {
        buffer.moveCaret(buffer.getDocument().positionOf(offset));
    }

    private static int caretOffset(EditorBuffer buffer) {
        return buffer.getDocument().offsetOf(buffer.getCaret());
    }

    private static EditorInputEvent enter(EditorBuffer buffer) {
        return EditorInputEvent.keyPressed("ENTER", caretOffset(buffer),
                buffer.getDocument().currentVersion(), Set.of());
    }

    private static EditorInputEvent enterWithSelection(EditorBuffer buffer, TextRange selection) {
        return EditorInputEvent.keyPressed("ENTER", selection.endOffset(),
                buffer.getDocument().currentVersion(), Set.of(), selection);
    }

    private static EditorInputEvent enterWithModifier(EditorBuffer buffer, EditorModifier modifier) {
        return EditorInputEvent.keyPressed("ENTER", caretOffset(buffer),
                buffer.getDocument().currentVersion(), Set.of(modifier));
    }

    @Test
    void plainEnterInsertsNewlineWithCurrentIndent() {
        EditorBuffer buffer = buffer("    int x = 1;");
        caretAtEnd(buffer);
        SmartEditResult result = pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("    int x = 1;\n    ", buffer.getDocument().getText());
        assertEquals(19, caretOffset(buffer));
    }

    @Test
    void enterAfterOpenBraceIncreasesIndent() {
        EditorBuffer buffer = buffer("class A {");
        caretAtEnd(buffer);
        pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertEquals("class A {\n    ", buffer.getDocument().getText());
    }

    @Test
    void enterInsideNestedBlockKeepsLevel() {
        EditorBuffer buffer = buffer("class A {\n    void m() {\n        int x;");
        caretAtEnd(buffer);
        pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertEquals("class A {\n    void m() {\n        int x;\n        ", buffer.getDocument().getText());
    }

    @Test
    void enterAfterClosingBraceDedents() {
        EditorBuffer buffer = buffer("class A {\n    int x;\n}");
        caretAtEnd(buffer);
        pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertEquals("class A {\n    int x;\n}\n", buffer.getDocument().getText());
    }

    @Test
    void enterAfterSwitchLabelIncreasesIndent() {
        EditorBuffer buffer = buffer("    case 1:");
        caretAtEnd(buffer);
        pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertEquals("    case 1:\n    ", buffer.getDocument().getText());
    }

    @Test
    void enterInsideStringIsNotIndentedByBraces() {
        EditorBuffer buffer = buffer("String s = \"{\";");
        caretAtEnd(buffer);
        pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertEquals("String s = \"{\";\n", buffer.getDocument().getText());
    }

    @Test
    void enterAtDocumentStartProducesPlainNewline() {
        EditorBuffer buffer = buffer("");
        SmartEditResult result = pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("\n", buffer.getDocument().getText());
    }

    @Test
    void controlEnterIsNotClaimed() {
        EditorBuffer buffer = buffer("foo");
        caretAtEnd(buffer);
        SmartEditResult result = pipeline().process(enterWithModifier(buffer, EditorModifier.CONTROL),
                new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals("foo", buffer.getDocument().getText());
    }

    @Test
    void typedNewlineCharacterIsNotClaimed() {
        EditorBuffer buffer = buffer("foo");
        caretAtEnd(buffer);
        EditorInputEvent event = EditorInputEvent.characterTyped('\n', 3,
                buffer.getDocument().currentVersion(), Set.of());
        SmartEditResult result = pipeline().process(event, new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals("foo", buffer.getDocument().getText());
    }

    @Test
    void enterWithSelectionIsNotClaimed() {
        EditorBuffer buffer = buffer("foo");
        SmartEditResult result = pipeline().process(enterWithSelection(buffer, new TextRange(0, 3)),
                new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals("foo", buffer.getDocument().getText());
    }

    @Test
    void autoIndentIsSingleUndoStep() {
        EditorBuffer buffer = buffer("class A {");
        caretAtEnd(buffer);
        pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertEquals("class A {\n    ", buffer.getDocument().getText());
        buffer.undo();
        assertEquals("class A {", buffer.getDocument().getText());
        assertFalse(buffer.canUndo());
    }

    @Test
    void caretIsOnTheNewIndentedLine() {
        EditorBuffer buffer = buffer("void m() {");
        caretAtEnd(buffer);
        pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertEquals(new EditorPosition(1, 4), buffer.getCaret());
    }
}
