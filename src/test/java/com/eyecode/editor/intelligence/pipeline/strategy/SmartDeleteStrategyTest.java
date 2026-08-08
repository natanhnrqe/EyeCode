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

/**
 * Verifies {@link SmartDeleteStrategy} is conservative: Sprint 5.1d defines no
 * structural delete case, so plain DELETE always falls through to native
 * behavior — over plain text, at the end of a line, in an empty document and
 * next to delimiters. The strategy never modifies the document.
 */
class SmartDeleteStrategyTest {

    private static TypingPipeline pipeline() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(new SmartDeleteStrategy());
        return new TypingPipeline(registry);
    }

    private static EditorBuffer buffer(String text) {
        return new EditorBuffer(new EditorDocument(null, text));
    }

    private static void caretAt(EditorBuffer buffer, int offset) {
        buffer.moveCaret(buffer.getDocument().positionOf(offset));
    }

    private static EditorInputEvent delete(EditorBuffer buffer, int offset) {
        return EditorInputEvent.keyPressed("DELETE", offset,
                buffer.getDocument().currentVersion(), Set.of());
    }

    @Test
    void deleteOverPlainTextIsPassthrough() {
        EditorBuffer buffer = buffer("foo");
        caretAt(buffer, 0);
        SmartEditResult result = pipeline().process(delete(buffer, 0), new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals("foo", buffer.getDocument().getText());
    }

    @Test
    void deleteAtEndOfLineIsPassthrough() {
        EditorBuffer buffer = buffer("foo");
        caretAt(buffer, 3);
        SmartEditResult result = pipeline().process(delete(buffer, 3), new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals("foo", buffer.getDocument().getText());
    }

    @Test
    void deleteInEmptyDocumentIsPassthrough() {
        EditorBuffer buffer = buffer("");
        SmartEditResult result = pipeline().process(delete(buffer, 0), new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals("", buffer.getDocument().getText());
    }

    @Test
    void deleteNextToDelimitersIsPassthrough() {
        EditorBuffer buffer = buffer("(foo)");
        caretAt(buffer, 0);
        SmartEditResult result = pipeline().process(delete(buffer, 0), new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals("(foo)", buffer.getDocument().getText());
    }

    @Test
    void controlDeleteIsNotClaimed() {
        EditorBuffer buffer = buffer("foo");
        EditorInputEvent event = EditorInputEvent.keyPressed("DELETE", 0,
                buffer.getDocument().currentVersion(), Set.of(EditorModifier.CONTROL));
        SmartEditResult result = pipeline().process(event, new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
    }

    @Test
    void deleteCreatesNoUndoEntry() {
        EditorBuffer buffer = buffer("foo");
        caretAt(buffer, 0);
        pipeline().process(delete(buffer, 0), new EditorCommandContext(buffer));
        assertFalse(buffer.canUndo());
    }
}
