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
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link SmartBackspaceStrategy} in isolation: one Backspace on the
 * leading whitespace removes exactly one indentation unit (spaces, tabs,
 * partial and nested levels), while carets outside the whitespace, on column 0,
 * on empty lines or with an active selection keep native Backspace behavior.
 */
class SmartBackspaceStrategyTest {

    private static TypingPipeline pipeline() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(new SmartBackspaceStrategy());
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

    private static EditorInputEvent backspace(EditorBuffer buffer) {
        return EditorInputEvent.keyPressed("BACKSPACE", caretOffset(buffer),
                buffer.getDocument().currentVersion(), Set.of());
    }

    private static EditorInputEvent backspaceWithSelection(EditorBuffer buffer, TextRange selection) {
        return EditorInputEvent.keyPressed("BACKSPACE", selection.startOffset(),
                buffer.getDocument().currentVersion(), Set.of(), selection);
    }

    @Test
    void oneIndentationLevelIsRemoved() {
        EditorBuffer buffer = buffer("    foo");
        caretAt(buffer, 4);
        SmartEditResult result = pipeline().process(backspace(buffer), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("foo", buffer.getDocument().getText());
        assertEquals(0, caretOffset(buffer));
    }

    @Test
    void specExampleEightSpacesCollapseToOneLevel() {
        EditorBuffer buffer = buffer("        ");
        caretAt(buffer, 8);
        pipeline().process(backspace(buffer), new EditorCommandContext(buffer));
        assertEquals("    ", buffer.getDocument().getText());
        assertEquals(4, caretOffset(buffer));
    }

    @Test
    void multipleNestedLevelsRemoveOneUnitPerPress() {
        EditorBuffer buffer = buffer("            foo");
        caretAt(buffer, 12);
        pipeline().process(backspace(buffer), new EditorCommandContext(buffer));
        assertEquals("        foo", buffer.getDocument().getText());
        assertEquals(8, caretOffset(buffer));
    }

    @Test
    void leadingTabCountsAsOneUnit() {
        EditorBuffer buffer = buffer("\tfoo");
        caretAt(buffer, 1);
        pipeline().process(backspace(buffer), new EditorCommandContext(buffer));
        assertEquals("foo", buffer.getDocument().getText());
        assertEquals(0, caretOffset(buffer));
    }

    @Test
    void partialIndentationIsCollapsed() {
        EditorBuffer buffer = buffer("   foo");
        caretAt(buffer, 3);
        pipeline().process(backspace(buffer), new EditorCommandContext(buffer));
        assertEquals("foo", buffer.getDocument().getText());
        assertEquals(0, caretOffset(buffer));
    }

    @Test
    void caretInsidePartialLeadingWhitespaceSnapsToPreviousIndentBoundary() {
        EditorBuffer buffer = buffer("      foo");
        caretAt(buffer, 6);

        pipeline().process(backspace(buffer), new EditorCommandContext(buffer));

        assertEquals("    foo", buffer.getDocument().getText());
        assertEquals(4, caretOffset(buffer));
    }

    @Test
    void caretAtLineStartIsNotClaimed() {
        EditorBuffer buffer = buffer("foo");
        caretAt(buffer, 0);
        SmartEditResult result = pipeline().process(backspace(buffer), new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals("foo", buffer.getDocument().getText());
    }

    @Test
    void caretOutsideIndentationIsNotClaimed() {
        EditorBuffer buffer = buffer("    foo");
        caretAt(buffer, 7);
        SmartEditResult result = pipeline().process(backspace(buffer), new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals("    foo", buffer.getDocument().getText());
        assertEquals(7, caretOffset(buffer));
    }

    @Test
    void caretRightBeforeContentIsClaimed() {
        EditorBuffer buffer = buffer("    foo");
        caretAt(buffer, 4);
        pipeline().process(backspace(buffer), new EditorCommandContext(buffer));
        assertEquals("foo", buffer.getDocument().getText());
        assertEquals(0, caretOffset(buffer));
    }

    @Test
    void activeSelectionIsNotClaimed() {
        EditorBuffer buffer = buffer("    foo");
        SmartEditResult result = pipeline().process(backspaceWithSelection(buffer, new TextRange(0, 4)),
                new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals("    foo", buffer.getDocument().getText());
    }

    @Test
    void emptyLineIsNotClaimed() {
        EditorBuffer buffer = buffer("foo\n\nbar");
        caretAt(buffer, 4);
        SmartEditResult result = pipeline().process(backspace(buffer), new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals("foo\n\nbar", buffer.getDocument().getText());
    }

    @Test
    void whitespaceOnlyLineIsDedented() {
        EditorBuffer buffer = buffer("foo\n    \nbar");
        caretAt(buffer, 8);
        pipeline().process(backspace(buffer), new EditorCommandContext(buffer));
        assertEquals("foo\n\nbar", buffer.getDocument().getText());
        assertEquals(4, caretOffset(buffer));
    }

    @Test
    void emptyDocumentIsNotClaimed() {
        EditorBuffer buffer = buffer("");
        SmartEditResult result = pipeline().process(backspace(buffer), new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals("", buffer.getDocument().getText());
    }

    @Test
    void dedentIsSingleUndoStep() {
        EditorBuffer buffer = buffer("        foo");
        caretAt(buffer, 8);
        pipeline().process(backspace(buffer), new EditorCommandContext(buffer));
        assertEquals("    foo", buffer.getDocument().getText());
        buffer.undo();
        assertEquals("        foo", buffer.getDocument().getText());
        assertFalse(buffer.canUndo());
    }

    @Test
    void controlBackspaceIsNotClaimed() {
        EditorBuffer buffer = buffer("    foo");
        caretAt(buffer, 4);
        EditorInputEvent event = EditorInputEvent.keyPressed("BACKSPACE", 4,
                buffer.getDocument().currentVersion(), Set.of(EditorModifier.CONTROL));
        SmartEditResult result = pipeline().process(event, new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals("    foo", buffer.getDocument().getText());
    }
}
