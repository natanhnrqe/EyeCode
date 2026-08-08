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

class QuoteCompletionStrategyTest {

    private static TypingPipeline pipeline() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(new QuoteCompletionStrategy());
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

    private static TextRange selectionRange(EditorBuffer buffer) {
        return new TextRange(
                buffer.getDocument().offsetOf(buffer.getSelection().getStart()),
                buffer.getDocument().offsetOf(buffer.getSelection().getEnd()));
    }

    @Test
    void doubleQuoteInsertsPairWithCaretBetween() {
        EditorBuffer buffer = buffer("");
        SmartEditResult result = pipeline().process(typed(buffer, '"'), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("\"\"", buffer.getDocument().getText());
        assertEquals(new EditorPosition(0, 1), buffer.getCaret());
    }

    @Test
    void singleQuoteInsertsPairWithCaretBetween() {
        EditorBuffer buffer = buffer("");
        SmartEditResult result = pipeline().process(typed(buffer, '\''), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("''", buffer.getDocument().getText());
        assertEquals(new EditorPosition(0, 1), buffer.getCaret());
    }

    @Test
    void quoteAtDocumentStartInsertsPair() {
        EditorBuffer buffer = buffer("ab");
        caretAt(buffer, 0);
        pipeline().process(typed(buffer, '"'), new EditorCommandContext(buffer));
        assertEquals("\"\"ab", buffer.getDocument().getText());
    }

    @Test
    void quoteAtDocumentEndInsertsPair() {
        EditorBuffer buffer = buffer("ab");
        caretAt(buffer, 2);
        pipeline().process(typed(buffer, '\''), new EditorCommandContext(buffer));
        assertEquals("ab''", buffer.getDocument().getText());
    }

    @Test
    void quoteBeforeDifferentCharacterInsertsPair() {
        EditorBuffer buffer = buffer("x");
        caretAt(buffer, 1);
        pipeline().process(typed(buffer, '"'), new EditorCommandContext(buffer));
        assertEquals("x\"\"", buffer.getDocument().getText());
        assertEquals(2, buffer.getDocument().offsetOf(buffer.getCaret()));
    }

    @Test
    void quoteWrapsSelection() {
        EditorBuffer buffer = buffer("foo");
        pipeline().process(typedWithSelection(buffer, '"', new TextRange(0, 3)),
                new EditorCommandContext(buffer));
        assertEquals("\"foo\"", buffer.getDocument().getText());
        assertEquals(new TextRange(1, 4), selectionRange(buffer));
    }

    @Test
    void singleQuoteWrapsSelection() {
        EditorBuffer buffer = buffer("foo");
        pipeline().process(typedWithSelection(buffer, '\'', new TextRange(0, 3)),
                new EditorCommandContext(buffer));
        assertEquals("'foo'", buffer.getDocument().getText());
    }

    @Test
    void quoteSkipOverWorksStandalone() {
        EditorBuffer buffer = buffer("\"\"");
        caretAt(buffer, 1);
        SmartEditResult result = pipeline().process(typed(buffer, '"'), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("\"\"", buffer.getDocument().getText());
        assertEquals(2, buffer.getDocument().offsetOf(buffer.getCaret()));
    }

    @Test
    void singleQuoteSkipOverWorksStandalone() {
        EditorBuffer buffer = buffer("''");
        caretAt(buffer, 1);
        pipeline().process(typed(buffer, '\''), new EditorCommandContext(buffer));
        assertEquals("''", buffer.getDocument().getText());
        assertEquals(2, buffer.getDocument().offsetOf(buffer.getCaret()));
    }

    @Test
    void quoteUndoIsSingleStep() {
        EditorBuffer buffer = buffer("");
        pipeline().process(typed(buffer, '"'), new EditorCommandContext(buffer));
        assertEquals("\"\"", buffer.getDocument().getText());
        buffer.undo();
        assertEquals("", buffer.getDocument().getText());
        assertFalse(buffer.canUndo());
    }

    @Test
    void quoteWrapUndoIsSingleStep() {
        EditorBuffer buffer = buffer("foo");
        pipeline().process(typedWithSelection(buffer, '"', new TextRange(0, 3)),
                new EditorCommandContext(buffer));
        assertEquals("\"foo\"", buffer.getDocument().getText());
        buffer.undo();
        assertEquals("foo", buffer.getDocument().getText());
        assertFalse(buffer.canUndo());
    }

    @Test
    void doesNotClaimUnderShortcutModifiers() {
        EditorBuffer buffer = buffer("");
        EditorCommandContext context = new EditorCommandContext(buffer);
        EditorInputEvent event = EditorInputEvent.characterTyped('"', 0, 0, Set.of(EditorModifier.CONTROL));
        assertFalse(new QuoteCompletionStrategy().supports(event, context));
    }

    @Test
    void commonCharactersAreNotClaimed() {
        for (char c : "ab.;,=".toCharArray()) {
            EditorBuffer buffer = buffer("ab");
            SmartEditResult result = pipeline().process(typed(buffer, c), new EditorCommandContext(buffer));
            assertFalse(result.isHandled(), "char " + c + " must not be handled");
        }
    }
}
