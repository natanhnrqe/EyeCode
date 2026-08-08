package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
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
 * Proves the full Sprint 5.1d chain with the default registry:
 * {@code EditorInputEvent → Dispatcher → Registry → Strategy → Command →
 * Transaction → EditorDocument} for smart Home/End/Backspace and the Delete
 * passthrough, plus the shared Enter-then-Backspace flow against
 * {@code JavaIndentPolicy}.
 */
class SmartNavigationIntegrationTest {

    private static TypingPipeline pipeline() {
        return new TypingPipeline(SmartEditingStrategies.defaultRegistry());
    }

    private static EditorBuffer buffer(String text) {
        return new EditorBuffer(new EditorDocument(null, text));
    }

    private static void caretAt(EditorBuffer buffer, int offset) {
        buffer.moveCaret(buffer.getDocument().positionOf(offset));
    }

    private static int caretOffset(EditorBuffer buffer) {
        return buffer.getDocument().offsetOf(buffer.getCaret());
    }

    private static EditorInputEvent key(EditorBuffer buffer, String key) {
        return EditorInputEvent.keyPressed(key, caretOffset(buffer),
                buffer.getDocument().currentVersion(), Set.of());
    }

    @Test
    void homeThroughFullChainMovesToFirstNonWhitespace() {
        EditorBuffer buffer = buffer("    int x;");
        caretAt(buffer, 12);
        SmartEditResult result = pipeline().process(key(buffer, "HOME"), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals(new EditorPosition(0, 4), buffer.getCaret());
        assertEquals("    int x;", buffer.getDocument().getText());
    }

    @Test
    void endThroughFullChainMovesToLogicalEnd() {
        EditorBuffer buffer = buffer("int x;   ");
        caretAt(buffer, 0);
        SmartEditResult result = pipeline().process(key(buffer, "END"), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals(new EditorPosition(0, 6), buffer.getCaret());
        assertEquals("int x;   ", buffer.getDocument().getText());
    }

    @Test
    void backspaceThroughFullChainDedents() {
        EditorBuffer buffer = buffer("        int x;");
        caretAt(buffer, 8);
        SmartEditResult result = pipeline().process(key(buffer, "BACKSPACE"), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("    int x;", buffer.getDocument().getText());
        assertEquals(4, caretOffset(buffer));
    }

    @Test
    void deleteThroughFullChainIsPassthrough() {
        EditorBuffer buffer = buffer("int x;");
        caretAt(buffer, 0);
        SmartEditResult result = pipeline().process(key(buffer, "DELETE"), new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals("int x;", buffer.getDocument().getText());
    }

    @Test
    void backspaceUndoRestoresPreviousState() {
        EditorBuffer buffer = buffer("        int x;");
        caretAt(buffer, 8);
        pipeline().process(key(buffer, "BACKSPACE"), new EditorCommandContext(buffer));
        assertEquals("    int x;", buffer.getDocument().getText());
        buffer.undo();
        assertEquals("        int x;", buffer.getDocument().getText());
        assertFalse(buffer.canUndo());
    }

    @Test
    void homeAndEndCreateNoUndoEntries() {
        EditorBuffer buffer = buffer("    foo   ");
        caretAt(buffer, 0);
        pipeline().process(key(buffer, "END"), new EditorCommandContext(buffer));
        pipeline().process(key(buffer, "HOME"), new EditorCommandContext(buffer));
        pipeline().process(key(buffer, "HOME"), new EditorCommandContext(buffer));
        assertEquals("    foo   ", buffer.getDocument().getText());
        assertFalse(buffer.canUndo());
    }

    @Test
    void enterThenBackspaceReturnsToPreviousIndentLevel() {
        EditorBuffer buffer = buffer("    if (true) {");
        caretAt(buffer, 15);
        pipeline().process(key(buffer, "ENTER"), new EditorCommandContext(buffer));
        assertEquals("    if (true) {\n    ", buffer.getDocument().getText());
        assertEquals(new EditorPosition(1, 4), buffer.getCaret());

        SmartEditResult result = pipeline().process(key(buffer, "BACKSPACE"), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("    if (true) {\n", buffer.getDocument().getText());
        assertEquals(new EditorPosition(1, 0), buffer.getCaret());
    }

    @Test
    void delimiterSkipOverStillWorksWithDefaultRegistry() {
        EditorBuffer buffer = buffer("()");
        caretAt(buffer, 1);
        SmartEditResult result = pipeline().process(EditorInputEvent.characterTyped(')', 1,
                buffer.getDocument().currentVersion(), Set.of()), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("()", buffer.getDocument().getText());
        assertEquals(2, caretOffset(buffer));
    }

    @Test
    void backspaceWithSelectionFallsThroughToNative() {
        EditorBuffer buffer = buffer("    foo");
        EditorInputEvent event = EditorInputEvent.keyPressed("BACKSPACE", 0,
                buffer.getDocument().currentVersion(), Set.of(), new TextRange(0, 4));
        SmartEditResult result = pipeline().process(event, new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals("    foo", buffer.getDocument().getText());
    }
}
