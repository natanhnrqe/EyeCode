package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorCommand;
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

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpeningDelimiterStrategyTest {

    private final OpeningDelimiterStrategy strategy = new OpeningDelimiterStrategy();

    private static TypingPipeline pipeline() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(new OpeningDelimiterStrategy());
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
    void supportsOnlyPlainOpeningDelimiters() {
        EditorBuffer buffer = buffer("");
        EditorCommandContext context = new EditorCommandContext(buffer);
        assertTrue(strategy.supports(typed(buffer, '('), context));
        assertTrue(strategy.supports(typed(buffer, '['), context));
        assertTrue(strategy.supports(typed(buffer, '{'), context));
        assertFalse(strategy.supports(typed(buffer, ')'), context));
        assertFalse(strategy.supports(typed(buffer, 'a'), context));
        assertFalse(strategy.supports(typed(buffer, '"'), context));
        assertFalse(strategy.supports(typed(buffer, '\''), context));
    }

    @Test
    void doesNotClaimDelimiterUnderShortcutModifiers() {
        EditorBuffer buffer = buffer("");
        EditorCommandContext context = new EditorCommandContext(buffer);
        EditorInputEvent control = EditorInputEvent.characterTyped('(', 0, 0, Set.of(EditorModifier.CONTROL));
        EditorInputEvent alt = EditorInputEvent.characterTyped('(', 0, 0, Set.of(EditorModifier.ALT));
        EditorInputEvent meta = EditorInputEvent.characterTyped('(', 0, 0, Set.of(EditorModifier.META));
        assertFalse(strategy.supports(control, context));
        assertFalse(strategy.supports(alt, context));
        assertFalse(strategy.supports(meta, context));
    }

    @Test
    void createCommandBuildsPairInsert() {
        EditorBuffer buffer = buffer("foo");
        caretAt(buffer, 3);
        EditorCommandContext context = new EditorCommandContext(buffer);
        Optional<EditorCommand> command = strategy.createCommand(typed(buffer, '('), context);
        assertTrue(command.isPresent());
        assertEquals("insert-delimiter-pair-(", command.get().name());
    }

    @Test
    void openingParenthesisInsertsPairWithCaretBetween() {
        EditorBuffer buffer = buffer("");
        SmartEditResult result = pipeline().process(typed(buffer, '('), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("()", buffer.getDocument().getText());
        assertEquals(new EditorPosition(0, 1), buffer.getCaret());
    }

    @Test
    void allOpeningDelimitersInsertPairs() {
        assertPair('(', "()");
        assertPair('[', "[]");
        assertPair('{', "{}");
    }

    @Test
    void insertsPairAtDocumentStart() {
        EditorBuffer buffer = buffer("ab");
        caretAt(buffer, 0);
        pipeline().process(typed(buffer, '('), new EditorCommandContext(buffer));
        assertEquals("()ab", buffer.getDocument().getText());
    }

    @Test
    void insertsPairAtDocumentEnd() {
        EditorBuffer buffer = buffer("ab");
        caretAt(buffer, 2);
        pipeline().process(typed(buffer, '('), new EditorCommandContext(buffer));
        assertEquals("ab()", buffer.getDocument().getText());
    }

    @Test
    void insertsPairBetweenTwoCharacters() {
        EditorBuffer buffer = buffer("ab");
        caretAt(buffer, 1);
        pipeline().process(typed(buffer, '{'), new EditorCommandContext(buffer));
        assertEquals("a{}b", buffer.getDocument().getText());
    }

    @Test
    void consecutivePairsNest() {
        EditorBuffer buffer = buffer("");
        pipeline().process(typed(buffer, '('), new EditorCommandContext(buffer));
        caretAt(buffer, 1);
        pipeline().process(typed(buffer, '['), new EditorCommandContext(buffer));
        assertEquals("([])", buffer.getDocument().getText());
    }

    @Test
    void wrapsSingleCharacterSelection() {
        EditorBuffer buffer = buffer("ab");
        SmartEditResult result = pipeline().process(
                typedWithSelection(buffer, '(', new TextRange(0, 1)),
                new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("(a)b", buffer.getDocument().getText());
        assertEquals(new TextRange(1, 2), selectionRange(buffer));
    }

    @Test
    void wrapsMultiCharacterSelection() {
        EditorBuffer buffer = buffer("foobar");
        pipeline().process(typedWithSelection(buffer, '(', new TextRange(3, 6)),
                new EditorCommandContext(buffer));
        assertEquals("foo(bar)", buffer.getDocument().getText());
        assertEquals(new TextRange(4, 7), selectionRange(buffer));
    }

    @Test
    void wrapsEntireDocument() {
        EditorBuffer buffer = buffer("foo");
        pipeline().process(typedWithSelection(buffer, '[', new TextRange(0, 3)),
                new EditorCommandContext(buffer));
        assertEquals("[foo]", buffer.getDocument().getText());
        assertEquals(new TextRange(1, 4), selectionRange(buffer));
    }

    @Test
    void wrapsWithEveryDelimiter() {
        assertWrap('(', "(foo)");
        assertWrap('[', "[foo]");
        assertWrap('{', "{foo}");
    }

    @Test
    void commonCharactersAreNotClaimed() {
        for (char c : "abc.;,=".toCharArray()) {
            EditorBuffer buffer = buffer("ab");
            SmartEditResult result = pipeline().process(typed(buffer, c), new EditorCommandContext(buffer));
            assertFalse(result.isHandled(), "char " + c + " must not be handled");
            assertEquals("ab", buffer.getDocument().getText());
        }
    }

    @Test
    void undoOfPairInsertRestoresDocumentInOneStep() {
        EditorBuffer buffer = buffer("");
        pipeline().process(typed(buffer, '('), new EditorCommandContext(buffer));
        assertEquals("()", buffer.getDocument().getText());
        buffer.undo();
        assertEquals("", buffer.getDocument().getText());
        assertFalse(buffer.canUndo());
    }

    @Test
    void undoOfWrapRestoresDocumentInOneStep() {
        EditorBuffer buffer = buffer("foo");
        pipeline().process(typedWithSelection(buffer, '(', new TextRange(0, 3)),
                new EditorCommandContext(buffer));
        assertEquals("(foo)", buffer.getDocument().getText());
        buffer.undo();
        assertEquals("foo", buffer.getDocument().getText());
        assertFalse(buffer.canUndo());
    }

    private void assertPair(char opening, String expected) {
        EditorBuffer buffer = buffer("");
        pipeline().process(typed(buffer, opening), new EditorCommandContext(buffer));
        assertEquals(expected, buffer.getDocument().getText());
        assertEquals(1, buffer.getDocument().offsetOf(buffer.getCaret()));
    }

    private void assertWrap(char opening, String expected) {
        EditorBuffer buffer = buffer("foo");
        pipeline().process(typedWithSelection(buffer, opening, new TextRange(0, 3)),
                new EditorCommandContext(buffer));
        assertEquals(expected, buffer.getDocument().getText());
    }
}
