package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent.EditorModifier;
import com.eyecode.editor.intelligence.pipeline.SmartEditingRegistry;
import com.eyecode.editor.intelligence.pipeline.SmartEditResult;
import com.eyecode.editor.intelligence.pipeline.TypingPipeline;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link SmartEndStrategy} in isolation: the logical end of the line
 * (before trailing whitespace) with a second press toggling to the absolute end,
 * blank lines, document end and multiple lines. No text is ever changed.
 */
class SmartEndStrategyTest {

    private static TypingPipeline pipeline() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(new SmartEndStrategy());
        return new TypingPipeline(registry);
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

    private static EditorInputEvent end(EditorBuffer buffer) {
        return EditorInputEvent.keyPressed("END", caretOffset(buffer),
                buffer.getDocument().currentVersion(), Set.of());
    }

    private static EditorInputEvent endWithModifier(EditorBuffer buffer, EditorModifier modifier) {
        return EditorInputEvent.keyPressed("END", caretOffset(buffer),
                buffer.getDocument().currentVersion(), Set.of(modifier));
    }

    @Test
    void normalLineMovesToLineEnd() {
        EditorBuffer buffer = buffer("foo");
        caretAt(buffer, 0);
        SmartEditResult result = pipeline().process(end(buffer), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals(3, caretOffset(buffer));
    }

    @Test
    void trailingWhitespaceMovesToLogicalEndFirst() {
        EditorBuffer buffer = buffer("foo   ");
        caretAt(buffer, 0);
        pipeline().process(end(buffer), new EditorCommandContext(buffer));
        assertEquals(3, caretOffset(buffer));
    }

    @Test
    void secondEndMovesToAbsoluteLineEnd() {
        EditorBuffer buffer = buffer("foo   ");
        caretAt(buffer, 3);
        pipeline().process(end(buffer), new EditorCommandContext(buffer));
        assertEquals(6, caretOffset(buffer));
    }

    @Test
    void emptyLineKeepsDeterministicPosition() {
        EditorBuffer buffer = buffer("foo\n\nbar");
        caretAt(buffer, 4);
        SmartEditResult result = pipeline().process(end(buffer), new EditorCommandContext(buffer));
        assertEquals(4, caretOffset(buffer));
        assertFalse(result.isHandled());
    }

    @Test
    void endOfDocumentMovesToLastLineEnd() {
        EditorBuffer buffer = buffer("foo");
        caretAt(buffer, 0);
        pipeline().process(end(buffer), new EditorCommandContext(buffer));
        assertEquals(3, caretOffset(buffer));
    }

    @Test
    void multipleLinesUseTheCurrentLine() {
        EditorBuffer buffer = buffer("abc\ndef");
        caretAt(buffer, 0);
        pipeline().process(end(buffer), new EditorCommandContext(buffer));
        assertEquals(3, caretOffset(buffer));
    }

    @Test
    void caretInsideTrailingWhitespaceMovesToAbsoluteEnd() {
        EditorBuffer buffer = buffer("foo   ");
        caretAt(buffer, 4);
        pipeline().process(end(buffer), new EditorCommandContext(buffer));
        assertEquals(6, caretOffset(buffer));
    }

    @Test
    void endDoesNotRemoveTrailingWhitespace() {
        EditorBuffer buffer = buffer("foo   ");
        caretAt(buffer, 0);
        pipeline().process(end(buffer), new EditorCommandContext(buffer));
        assertEquals("foo   ", buffer.getDocument().getText());
        assertFalse(buffer.canUndo());
    }

    @Test
    void shiftEndIsNotClaimed() {
        EditorBuffer buffer = buffer("foo");
        caretAt(buffer, 0);
        SmartEditResult result = pipeline().process(endWithModifier(buffer, EditorModifier.SHIFT),
                new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals(0, caretOffset(buffer));
    }

    @Test
    void controlEndIsNotClaimed() {
        EditorBuffer buffer = buffer("foo");
        caretAt(buffer, 0);
        SmartEditResult result = pipeline().process(endWithModifier(buffer, EditorModifier.CONTROL),
                new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals(0, caretOffset(buffer));
    }
}
