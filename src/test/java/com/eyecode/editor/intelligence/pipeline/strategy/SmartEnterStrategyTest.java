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
 * Verifies {@link SmartEnterStrategy} in isolation: same-line brace splitting,
 * open-brace-at-line-end, blank-line-before-closing-brace normalization and
 * selection replacement. Plain Enter presses that match no smart case must not
 * be claimed (they fall through to {@link AutoIndentStrategy}).
 */
class SmartEnterStrategyTest {

    private static TypingPipeline pipeline() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(new SmartEnterStrategy());
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

    private static EditorInputEvent enter(EditorBuffer buffer) {
        return EditorInputEvent.keyPressed("ENTER", caretOffset(buffer),
                buffer.getDocument().currentVersion(), Set.of());
    }

    private static EditorInputEvent enterWithSelection(EditorBuffer buffer, TextRange selection) {
        return EditorInputEvent.keyPressed("ENTER", selection.endOffset(),
                buffer.getDocument().currentVersion(), Set.of(), selection);
    }

    @Test
    void sameLineBracePairIsSplitIntoTriple() {
        EditorBuffer buffer = buffer("void m() {}");
        caretAt(buffer, 10);
        SmartEditResult result = pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("void m() {\n    \n}", buffer.getDocument().getText());
        assertEquals(new EditorPosition(1, 4), buffer.getCaret());
    }

    @Test
    void nestedSameLineBracePairKeepsNesting() {
        EditorBuffer buffer = buffer("class A {\n    void m() {}");
        caretAt(buffer, 24);
        pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertEquals("class A {\n    void m() {\n        \n    }", buffer.getDocument().getText());
        assertEquals(new EditorPosition(2, 8), buffer.getCaret());
    }

    @Test
    void openBraceAtLineEndInsertsIndentedLine() {
        EditorBuffer buffer = buffer("void m() {");
        caretAt(buffer, 10);
        pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertEquals("void m() {\n    ", buffer.getDocument().getText());
    }

    @Test
    void openBraceAtLineEndWithExistingClosingBraceBelow() {
        EditorBuffer buffer = buffer("void m() {\n}");
        caretAt(buffer, 10);
        pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertEquals("void m() {\n    \n}", buffer.getDocument().getText());
        assertEquals(new EditorPosition(1, 4), buffer.getCaret());
    }

    @Test
    void blankLineBeforeSameLineClosingBraceNormalizesIndent() {
        EditorBuffer buffer = buffer("void m() {\n        }");
        caretAt(buffer, 19);
        pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertEquals("void m() {\n    \n}", buffer.getDocument().getText());
        assertEquals(new EditorPosition(1, 4), buffer.getCaret());
    }

    @Test
    void blankLineBeforeClosingBraceOnNextLineKeepsBlockLevel() {
        EditorBuffer buffer = buffer("void m() {\n    \n}");
        caretAt(buffer, 15);
        pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertEquals("void m() {\n    \n    \n}", buffer.getDocument().getText());
        assertEquals(new EditorPosition(2, 4), buffer.getCaret());
    }

    @Test
    void closingBraceWithTrailingCommentStillNormalizes() {
        EditorBuffer buffer = buffer("void m() {\n    } // end");
        caretAt(buffer, 15);
        pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertEquals("void m() {\n    \n} // end", buffer.getDocument().getText());
    }

    @Test
    void activeSelectionIsReplacedByNewline() {
        EditorBuffer buffer = buffer("foo");
        SmartEditResult result = pipeline().process(enterWithSelection(buffer, new TextRange(0, 3)),
                new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("\n", buffer.getDocument().getText());
    }

    @Test
    void selectionReplacementUsesPolicyIndent() {
        EditorBuffer buffer = buffer("class A {\n    foo");
        SmartEditResult result = pipeline().process(enterWithSelection(buffer, new TextRange(14, 17)),
                new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("class A {\n    \n    ", buffer.getDocument().getText());
    }

    @Test
    void plainEnterWithoutSmartCaseIsNotClaimed() {
        EditorBuffer buffer = buffer("foo()");
        caretAt(buffer, 5);
        SmartEditResult result = pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals("foo()", buffer.getDocument().getText());
    }

    @Test
    void enterMidLineAfterBraceWithContentIsNotClaimed() {
        EditorBuffer buffer = buffer("void m() { foo");
        caretAt(buffer, 11);
        SmartEditResult result = pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
    }

    @Test
    void braceSplitIsSingleUndoStep() {
        EditorBuffer buffer = buffer("void m() {}");
        caretAt(buffer, 10);
        pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertEquals("void m() {\n    \n}", buffer.getDocument().getText());
        buffer.undo();
        assertEquals("void m() {}", buffer.getDocument().getText());
        assertFalse(buffer.canUndo());
    }

    @Test
    void controlEnterIsNotClaimedBySmartEnter() {
        EditorBuffer buffer = buffer("void m() {}");
        caretAt(buffer, 10);
        EditorInputEvent event = EditorInputEvent.keyPressed("ENTER", 10,
                buffer.getDocument().currentVersion(), Set.of(EditorInputEvent.EditorModifier.CONTROL));
        SmartEditResult result = pipeline().process(event, new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
    }
}
