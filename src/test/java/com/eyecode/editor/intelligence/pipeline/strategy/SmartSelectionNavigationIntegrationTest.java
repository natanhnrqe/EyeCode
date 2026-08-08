package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent.EditorModifier;
import com.eyecode.editor.intelligence.pipeline.SmartEditResult;
import com.eyecode.editor.intelligence.pipeline.TypingPipeline;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorSelection;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the full Sprint 5.1e chain with the default registry:
 * {@code Ctrl+W / Ctrl+Shift+W → Dispatcher → ExtendSelectionStrategy /
 * ShrinkSelectionStrategy → SetSelectionCommand → EditorBuffer} with the
 * shared {@code SelectionHistory} walking R1 → R2 → R3 → R2. Selection
 * changes are caret-only: no text mutation and no undo entries.
 */
class SmartSelectionNavigationIntegrationTest {

    private static final String METHOD = """
            int foo(int x, int y) {
                int z = bar(x + 1);
                return z;
            }
            """;

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

    private static EditorInputEvent key(EditorBuffer buffer, Set<EditorModifier> modifiers) {
        EditorSelection selection = buffer.getSelection();
        int start = buffer.getDocument().offsetOf(selection.getStart());
        int end = buffer.getDocument().offsetOf(selection.getEnd());
        return EditorInputEvent.keyPressed("W", caretOffset(buffer),
                buffer.getDocument().currentVersion(), modifiers,
                new TextRange(Math.min(start, end), Math.max(start, end)));
    }

    private static EditorInputEvent key(EditorBuffer buffer, Set<EditorModifier> modifiers, TextRange selection) {
        return EditorInputEvent.keyPressed("W", caretOffset(buffer),
                buffer.getDocument().currentVersion(), modifiers, selection);
    }

    private static void assertSelected(EditorBuffer buffer, int start, int end) {
        EditorSelection selection = buffer.getSelection();
        int selStart = buffer.getDocument().offsetOf(selection.getStart());
        int selEnd = buffer.getDocument().offsetOf(selection.getEnd());
        assertEquals(new TextRange(start, end),
                new TextRange(Math.min(selStart, selEnd), Math.max(selStart, selEnd)), "selection");
        assertEquals(end, caretOffset(buffer), "caret");
    }

    @Test
    void expandWordThenShrinkRestoresCaretWithoutEditing() {
        TypingPipeline pipeline = pipeline();
        EditorBuffer buffer = buffer("int x;");
        caretAt(buffer, 4);

        SmartEditResult expanded = pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL)),
                new EditorCommandContext(buffer));
        assertTrue(expanded.isHandled());
        assertSelected(buffer, 4, 5);
        assertEquals("int x;", buffer.getDocument().getText());

        SmartEditResult shrunk = pipeline.process(
                key(buffer, Set.of(EditorModifier.CONTROL, EditorModifier.SHIFT)),
                new EditorCommandContext(buffer));
        assertTrue(shrunk.isHandled());
        assertEquals(4, caretOffset(buffer));
        assertTrue(buffer.getSelection().isEmpty());
        assertEquals("int x;", buffer.getDocument().getText());
        assertFalse(buffer.canUndo());
    }

    @Test
    void expandWalksTheLadderThenShrinkWalksBack() {
        TypingPipeline pipeline = pipeline();
        EditorBuffer buffer = buffer("int z = bar(x + 1);");
        caretAt(buffer, 12);

        pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL)), new EditorCommandContext(buffer));
        assertSelected(buffer, 12, 13);
        pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL)), new EditorCommandContext(buffer));
        assertSelected(buffer, 12, 17);
        pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL)), new EditorCommandContext(buffer));
        assertSelected(buffer, 11, 18);
        pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL)), new EditorCommandContext(buffer));
        assertSelected(buffer, 8, 18);
        pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL)), new EditorCommandContext(buffer));
        assertSelected(buffer, 0, 19);
        SmartEditResult capped = pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL)),
                new EditorCommandContext(buffer));
        assertFalse(capped.isHandled());

        pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL, EditorModifier.SHIFT)),
                new EditorCommandContext(buffer));
        assertSelected(buffer, 8, 18);
        pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL, EditorModifier.SHIFT)),
                new EditorCommandContext(buffer));
        assertSelected(buffer, 11, 18);
        pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL, EditorModifier.SHIFT)),
                new EditorCommandContext(buffer));
        assertSelected(buffer, 12, 17);
        pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL, EditorModifier.SHIFT)),
                new EditorCommandContext(buffer));
        assertSelected(buffer, 12, 13);
        pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL, EditorModifier.SHIFT)),
                new EditorCommandContext(buffer));
        assertTrue(buffer.getSelection().isEmpty());
        assertEquals(12, caretOffset(buffer));

        SmartEditResult drained = pipeline.process(
                key(buffer, Set.of(EditorModifier.CONTROL, EditorModifier.SHIFT)),
                new EditorCommandContext(buffer));
        assertFalse(drained.isHandled());
        assertEquals("int z = bar(x + 1);", buffer.getDocument().getText());
        assertFalse(buffer.canUndo());
    }

    @Test
    void expandGrowsFromExistingSelection() {
        TypingPipeline pipeline = pipeline();
        EditorBuffer buffer = buffer("int z = bar(x + 1);");
        caretAt(buffer, 12);
        SmartEditResult result = pipeline.process(
                key(buffer, Set.of(EditorModifier.CONTROL), new TextRange(12, 13)),
                new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertSelected(buffer, 12, 17);
    }

    @Test
    void expandIgnoresCtrlAltW() {
        TypingPipeline pipeline = pipeline();
        EditorBuffer buffer = buffer("int x;");
        caretAt(buffer, 4);
        SmartEditResult result = pipeline.process(
                key(buffer, Set.of(EditorModifier.CONTROL, EditorModifier.ALT)),
                new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
    }

    @Test
    void expandIgnoresPlainW() {
        TypingPipeline pipeline = pipeline();
        EditorBuffer buffer = buffer("int x;");
        caretAt(buffer, 4);
        SmartEditResult result = pipeline.process(key(buffer, Set.of()),
                new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
    }

    @Test
    void expandRepeatedlyGrowsPastWord() {
        TypingPipeline pipeline = pipeline();
        EditorBuffer buffer = buffer("int x;");
        caretAt(buffer, 4);
        SmartEditResult first = pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL)),
                new EditorCommandContext(buffer));
        assertTrue(first.isHandled());
        assertSelected(buffer, 4, 5);
        SmartEditResult second = pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL)),
                new EditorCommandContext(buffer));
        assertTrue(second.isHandled());
        assertSelected(buffer, 0, 5);
    }

    @Test
    void expandIgnoresKeyReleased() {
        TypingPipeline pipeline = pipeline();
        EditorBuffer buffer = buffer("int x;");
        caretAt(buffer, 4);
        EditorInputEvent released = EditorInputEvent.keyReleased("W", 4,
                buffer.getDocument().currentVersion(), Set.of(EditorModifier.CONTROL));
        SmartEditResult result = pipeline.process(released, new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
    }

    @Test
    void expandIsCaseInsensitive() {
        TypingPipeline pipeline = pipeline();
        EditorBuffer buffer = buffer("int x;");
        caretAt(buffer, 4);
        EditorInputEvent lowercase = EditorInputEvent.keyPressed("w", 4,
                buffer.getDocument().currentVersion(), Set.of(EditorModifier.CONTROL));
        SmartEditResult result = pipeline.process(lowercase, new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertSelected(buffer, 4, 5);
    }

    @Test
    void shrinkAfterDocumentEditFallsThrough() {
        TypingPipeline pipeline = pipeline();
        EditorBuffer buffer = buffer("int z = bar(x + 1);");
        caretAt(buffer, 12);
        pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL)), new EditorCommandContext(buffer));
        assertSelected(buffer, 12, 13);

        buffer.insertText(0, "  ");
        SmartEditResult result = pipeline.process(
                key(buffer, Set.of(EditorModifier.CONTROL, EditorModifier.SHIFT)),
                new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
    }

    @Test
    void expandInMethodBodyGrowsToDeclaration() {
        TypingPipeline pipeline = pipeline();
        EditorBuffer buffer = buffer(METHOD);
        caretAt(buffer, 12);

        pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL)), new EditorCommandContext(buffer));
        assertSelected(buffer, 12, 13);
        pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL)), new EditorCommandContext(buffer));
        assertSelected(buffer, 8, 13);
        pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL)), new EditorCommandContext(buffer));
        assertSelected(buffer, 8, 20);
        pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL)), new EditorCommandContext(buffer));
        assertSelected(buffer, 7, 21);
        pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL)), new EditorCommandContext(buffer));
        assertSelected(buffer, 0, 21);
        pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL)), new EditorCommandContext(buffer));
        assertSelected(buffer, 0, 22);
        pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL)), new EditorCommandContext(buffer));
        assertSelected(buffer, 0, 64);
        SmartEditResult capped = pipeline.process(key(buffer, Set.of(EditorModifier.CONTROL)),
                new EditorCommandContext(buffer));
        assertFalse(capped.isHandled());
    }
}
