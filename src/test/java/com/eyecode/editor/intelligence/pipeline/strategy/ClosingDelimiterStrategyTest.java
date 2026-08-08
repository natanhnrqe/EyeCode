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

class ClosingDelimiterStrategyTest {

    private final ClosingDelimiterStrategy strategy = new ClosingDelimiterStrategy();

    private static TypingPipeline pipeline() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(new ClosingDelimiterStrategy());
        return new TypingPipeline(registry);
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
    void supportsOnlyPlainClosingDelimiters() {
        EditorBuffer buffer = buffer("");
        EditorCommandContext context = new EditorCommandContext(buffer);
        assertTrue(strategy.supports(typed(buffer, ')'), context));
        assertTrue(strategy.supports(typed(buffer, ']'), context));
        assertTrue(strategy.supports(typed(buffer, '}'), context));
        assertTrue(strategy.supports(typed(buffer, '"'), context));
        assertTrue(strategy.supports(typed(buffer, '\''), context));
        assertFalse(strategy.supports(typed(buffer, '('), context));
        assertFalse(strategy.supports(typed(buffer, '['), context));
        assertFalse(strategy.supports(typed(buffer, 'a'), context));
    }

    @Test
    void doesNotClaimUnderShortcutModifiers() {
        EditorBuffer buffer = buffer("");
        EditorCommandContext context = new EditorCommandContext(buffer);
        EditorInputEvent event = EditorInputEvent.characterTyped(')', 0, 0, Set.of(EditorModifier.CONTROL));
        assertFalse(strategy.supports(event, context));
    }

    @Test
    void skipOverMovesCaretOverExistingClosing() {
        EditorBuffer buffer = buffer("()");
        caretAt(buffer, 1);
        SmartEditResult result = pipeline().process(typed(buffer, ')'), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("()", buffer.getDocument().getText());
        assertEquals(new EditorPosition(0, 2), buffer.getCaret());
    }

    @Test
    void skipOverWorksForAllClosingDelimiters() {
        assertSkip("()", ')');
        assertSkip("[]", ']');
        assertSkip("{}", '}');
        assertSkip("\"\"", '"');
        assertSkip("''", '\'');
    }

    @Test
    void differentClosingDelimiterIsNotSkipped() {
        EditorBuffer buffer = buffer("()");
        caretAt(buffer, 1);
        SmartEditResult result = pipeline().process(typed(buffer, ']'), new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals("()", buffer.getDocument().getText());
        assertEquals(new EditorPosition(0, 1), buffer.getCaret());
    }

    @Test
    void caretAtEndDoesNotSkip() {
        EditorBuffer buffer = buffer("()");
        caretAt(buffer, 2);
        SmartEditResult result = pipeline().process(typed(buffer, ')'), new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals("()", buffer.getDocument().getText());
    }

    @Test
    void noSkipWhenSelectionIsActive() {
        EditorBuffer buffer = buffer("ab()");
        SmartEditResult result = pipeline().process(
                typedWithSelection(buffer, ')', new TextRange(0, 2)),
                new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals("ab()", buffer.getDocument().getText());
    }

    @Test
    void skipOverRecordsNoUndoEntry() {
        EditorBuffer buffer = buffer("()");
        caretAt(buffer, 1);
        SmartEditResult result = pipeline().process(typed(buffer, ')'), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertFalse(buffer.canUndo());
    }

    @Test
    void unclaimedClosingFallsThroughToNative() {
        EditorBuffer buffer = buffer("ab");
        caretAt(buffer, 1);
        SmartEditResult result = pipeline().process(typed(buffer, ')'), new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals("ab", buffer.getDocument().getText());
    }

    private void assertSkip(String text, char closing) {
        EditorBuffer buffer = buffer(text);
        caretAt(buffer, 1);
        SmartEditResult result = pipeline().process(typed(buffer, closing), new EditorCommandContext(buffer));
        assertTrue(result.isHandled(), "expected skip-over for " + closing);
        assertEquals(text, buffer.getDocument().getText());
        assertEquals(2, buffer.getDocument().offsetOf(buffer.getCaret()));
    }
}
