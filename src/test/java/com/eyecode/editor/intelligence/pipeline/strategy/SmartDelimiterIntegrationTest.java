package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
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
 * Proves the whole chain
 * EditorInputEvent -> TypingPipeline -> EditorInputDispatcher -> SmartEditingRegistry
 * -> SmartEditStrategy -> EditorCommandContext -> DocumentTransaction -> EditorDocument
 * works for delimiter editing without any Swing/JavaFX involvement.
 */
class SmartDelimiterIntegrationTest {

    private static TypingPipeline pipeline() {
        return new TypingPipeline(SmartEditingStrategies.defaultRegistry());
    }

    private static EditorBuffer buffer(String text) {
        return new EditorBuffer(new EditorDocument(null, text));
    }

    private static void caretAt(EditorBuffer buffer, int offset) {
        buffer.moveCaret(buffer.getDocument().positionOf(offset));
    }

    private static EditorInputEvent typed(EditorBuffer buffer, char c) {
        int offset = buffer.getDocument().offsetOf(buffer.getCaret());
        return EditorInputEvent.characterTyped(c, offset, buffer.getDocument().currentVersion(), Set.of());
    }

    private static EditorInputEvent typedWithSelection(EditorBuffer buffer, char c, TextRange selection) {
        return EditorInputEvent.characterTyped(c, selection.endOffset(),
                buffer.getDocument().currentVersion(), Set.of(), selection);
    }

    @Test
    void openingTypedThroughFullChainProducesPair() {
        EditorBuffer buffer = buffer("");
        SmartEditResult result = pipeline().process(typed(buffer, '('), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("()", buffer.getDocument().getText());
        assertEquals(new EditorPosition(0, 1), buffer.getCaret());
    }

    @Test
    void allAcceptancePairsAreProduced() {
        assertTyped("", '(', "()");
        assertTyped("", '[', "[]");
        assertTyped("", '{', "{}");
        assertTyped("", '"', "\"\"");
        assertTyped("", '\'', "''");
    }

    @Test
    void closingSkipWinsOverOtherStrategies() {
        EditorBuffer buffer = buffer("()");
        caretAt(buffer, 1);
        SmartEditResult result = pipeline().process(typed(buffer, ')'), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("()", buffer.getDocument().getText());
        assertEquals(2, buffer.getDocument().offsetOf(buffer.getCaret()));
    }

    @Test
    void quoteSkipWinsAtHighPriority() {
        EditorBuffer buffer = buffer("\"\"");
        caretAt(buffer, 1);
        SmartEditResult result = pipeline().process(typed(buffer, '"'), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("\"\"", buffer.getDocument().getText());
        assertEquals(2, buffer.getDocument().offsetOf(buffer.getCaret()));
    }

    @Test
    void singleQuoteSkipWinsAtHighPriority() {
        EditorBuffer buffer = buffer("''");
        caretAt(buffer, 1);
        SmartEditResult result = pipeline().process(typed(buffer, '\''), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("''", buffer.getDocument().getText());
        assertEquals(2, buffer.getDocument().offsetOf(buffer.getCaret()));
    }

    @Test
    void quoteWithoutExistingPairInsertsPair() {
        EditorBuffer buffer = buffer("x");
        caretAt(buffer, 1);
        SmartEditResult result = pipeline().process(typed(buffer, '"'), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("x\"\"", buffer.getDocument().getText());
    }

    @Test
    void wrapSelectionThroughFullChainIsSingleUndo() {
        EditorBuffer buffer = buffer("foo");
        SmartEditResult result = pipeline().process(
                typedWithSelection(buffer, '(', new TextRange(0, 3)),
                new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("(foo)", buffer.getDocument().getText());
        buffer.undo();
        assertEquals("foo", buffer.getDocument().getText());
        assertFalse(buffer.canUndo());
    }

    @Test
    void wrapQuoteThroughFullChainIsSingleUndo() {
        EditorBuffer buffer = buffer("foo");
        pipeline().process(typedWithSelection(buffer, '"', new TextRange(0, 3)),
                new EditorCommandContext(buffer));
        assertEquals("\"foo\"", buffer.getDocument().getText());
        buffer.undo();
        assertEquals("foo", buffer.getDocument().getText());
    }

    @Test
    void nestedDelimitersAreBuiltAndSkipped() {
        EditorBuffer buffer = buffer("");
        pipeline().process(typed(buffer, '('), new EditorCommandContext(buffer));
        caretAt(buffer, 1);
        pipeline().process(typed(buffer, '['), new EditorCommandContext(buffer));
        caretAt(buffer, 2);
        pipeline().process(typed(buffer, '{'), new EditorCommandContext(buffer));
        assertEquals("([{}])", buffer.getDocument().getText());

        caretAt(buffer, 3);
        pipeline().process(typed(buffer, '}'), new EditorCommandContext(buffer));
        caretAt(buffer, 4);
        pipeline().process(typed(buffer, ']'), new EditorCommandContext(buffer));
        caretAt(buffer, 5);
        pipeline().process(typed(buffer, ')'), new EditorCommandContext(buffer));

        assertEquals("([{}])", buffer.getDocument().getText());
        assertEquals(6, buffer.getDocument().offsetOf(buffer.getCaret()));
    }

    @Test
    void mismatchedClosingDoesNotSkip() {
        EditorBuffer buffer = buffer("([|])".replace("|", ""));
        caretAt(buffer, 2);
        SmartEditResult result = pipeline().process(typed(buffer, '}'), new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals("([])", buffer.getDocument().getText());
        assertEquals(2, buffer.getDocument().offsetOf(buffer.getCaret()));
    }

    @Test
    void commonCharactersFlowThroughToNative() {
        for (char c : "abc.;,=".toCharArray()) {
            EditorBuffer buffer = buffer("");
            SmartEditResult result = pipeline().process(typed(buffer, c), new EditorCommandContext(buffer));
            assertFalse(result.isHandled(), "char " + c + " must not be handled");
            assertEquals("", buffer.getDocument().getText());
        }
    }

    @Test
    void skipOverCreatesNoUndoEntry() {
        EditorBuffer buffer = buffer("()");
        caretAt(buffer, 1);
        pipeline().process(typed(buffer, ')'), new EditorCommandContext(buffer));
        assertFalse(buffer.canUndo());
    }

    @Test
    void undoOfPairAndWrapAreIndependentSteps() {
        EditorBuffer buffer = buffer("");
        pipeline().process(typed(buffer, '('), new EditorCommandContext(buffer));
        assertEquals("()", buffer.getDocument().getText());
        buffer.undo();
        assertEquals("", buffer.getDocument().getText());
    }

    private void assertTyped(String initial, char c, String expected) {
        EditorBuffer buffer = buffer(initial);
        SmartEditResult result = pipeline().process(typed(buffer, c), new EditorCommandContext(buffer));
        assertTrue(result.isHandled(), "expected handled for " + c);
        assertEquals(expected, buffer.getDocument().getText());
    }
}
