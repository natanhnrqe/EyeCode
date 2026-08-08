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
 * Verifies {@link SmartHomeStrategy} in isolation: toggling between the first
 * non-whitespace character and column 0, spaces/tabs, empty lines and the
 * no-undo property. Modified Home presses are never claimed.
 */
class SmartHomeStrategyTest {

    private static TypingPipeline pipeline() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(new SmartHomeStrategy());
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

    private static EditorInputEvent home(EditorBuffer buffer) {
        return EditorInputEvent.keyPressed("HOME", caretOffset(buffer),
                buffer.getDocument().currentVersion(), Set.of());
    }

    private static EditorInputEvent homeWithModifier(EditorBuffer buffer, EditorModifier modifier) {
        return EditorInputEvent.keyPressed("HOME", caretOffset(buffer),
                buffer.getDocument().currentVersion(), Set.of(modifier));
    }

    @Test
    void lineWithoutIndentationMovesToColumnZero() {
        EditorBuffer buffer = buffer("foo");
        caretAt(buffer, 3);
        SmartEditResult result = pipeline().process(home(buffer), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals(0, caretOffset(buffer));
    }

    @Test
    void lineWithSpacesMovesToFirstNonWhitespace() {
        EditorBuffer buffer = buffer("    foo");
        caretAt(buffer, 7);
        pipeline().process(home(buffer), new EditorCommandContext(buffer));
        assertEquals(4, caretOffset(buffer));
    }

    @Test
    void secondHomeMovesToColumnZero() {
        EditorBuffer buffer = buffer("    foo");
        caretAt(buffer, 4);
        pipeline().process(home(buffer), new EditorCommandContext(buffer));
        assertEquals(0, caretOffset(buffer));
    }

    @Test
    void lineWithTabsMovesToFirstNonWhitespace() {
        EditorBuffer buffer = buffer("\tfoo");
        caretAt(buffer, 4);
        pipeline().process(home(buffer), new EditorCommandContext(buffer));
        assertEquals(1, caretOffset(buffer));
    }

    @Test
    void caretBeforeIndentationMovesToFirstNonWhitespace() {
        EditorBuffer buffer = buffer("    foo");
        caretAt(buffer, 0);
        pipeline().process(home(buffer), new EditorCommandContext(buffer));
        assertEquals(4, caretOffset(buffer));
    }

    @Test
    void caretInsideIndentationMovesToFirstNonWhitespace() {
        EditorBuffer buffer = buffer("    foo");
        caretAt(buffer, 2);
        pipeline().process(home(buffer), new EditorCommandContext(buffer));
        assertEquals(4, caretOffset(buffer));
    }

    @Test
    void emptyLineKeepsDeterministicPosition() {
        EditorBuffer buffer = buffer("foo\n\nbar");
        caretAt(buffer, 4);
        SmartEditResult result = pipeline().process(home(buffer), new EditorCommandContext(buffer));
        assertEquals(4, caretOffset(buffer));
        assertFalse(result.isHandled());
    }

    @Test
    void whitespaceOnlyLineMovesToColumnZero() {
        EditorBuffer buffer = buffer("foo\n    \nbar");
        caretAt(buffer, 7);
        pipeline().process(home(buffer), new EditorCommandContext(buffer));
        assertEquals(4, caretOffset(buffer));
    }

    @Test
    void emptyDocumentStaysAtZero() {
        EditorBuffer buffer = buffer("");
        SmartEditResult result = pipeline().process(home(buffer), new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals(0, caretOffset(buffer));
    }

    @Test
    void multipleLinesUseTheCurrentLine() {
        EditorBuffer buffer = buffer("    a\n    foobar");
        caretAt(buffer, 16);
        pipeline().process(home(buffer), new EditorCommandContext(buffer));
        assertEquals(10, caretOffset(buffer));
    }

    @Test
    void homeDoesNotCreateUndoEntry() {
        EditorBuffer buffer = buffer("    foo");
        caretAt(buffer, 7);
        pipeline().process(home(buffer), new EditorCommandContext(buffer));
        assertEquals("    foo", buffer.getDocument().getText());
        assertFalse(buffer.canUndo());
    }

    @Test
    void shiftHomeIsNotClaimed() {
        EditorBuffer buffer = buffer("    foo");
        caretAt(buffer, 7);
        SmartEditResult result = pipeline().process(homeWithModifier(buffer, EditorModifier.SHIFT),
                new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals(7, caretOffset(buffer));
    }

    @Test
    void controlHomeIsNotClaimed() {
        EditorBuffer buffer = buffer("    foo");
        caretAt(buffer, 7);
        SmartEditResult result = pipeline().process(homeWithModifier(buffer, EditorModifier.CONTROL),
                new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals(7, caretOffset(buffer));
    }
}
