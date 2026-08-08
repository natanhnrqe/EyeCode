package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
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
 * Proves the full chain for Sprint 5.1c with the default registry: ENTER key
 * presses resolve through SmartEnterStrategy/AutoIndentStrategy, mutate the
 * document in a single undoable transaction and bump the document version, with
 * delimiters still working alongside.
 */
class SmartIndentIntegrationTest {

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

    private static EditorInputEvent enter(EditorBuffer buffer) {
        return EditorInputEvent.keyPressed("ENTER", caretOffset(buffer),
                buffer.getDocument().currentVersion(), Set.of());
    }

    @Test
    void methodBlockBuiltWithSmartEnterThroughFullChain() {
        EditorBuffer buffer = buffer("class A {");
        caretAt(buffer, 9);
        pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertEquals("class A {\n    ", buffer.getDocument().getText());

        buffer.insertText(caretOffset(buffer), "void m() {}");
        caretAt(buffer, 24);
        pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertEquals("class A {\n    void m() {\n        \n    }", buffer.getDocument().getText());
        assertEquals(new EditorPosition(2, 8), buffer.getCaret());
    }

    @Test
    void closingBraceThenEnterDedentsThroughFullChain() {
        EditorBuffer buffer = buffer("class A {\n    void m() {\n        x();\n    }");
        caretAt(buffer, 43);
        SmartEditResult result = pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("class A {\n    void m() {\n        x();\n    }\n    ", buffer.getDocument().getText());
    }

    @Test
    void undoRestoresExactTextAfterSplit() {
        EditorBuffer buffer = buffer("void m() {}");
        caretAt(buffer, 10);
        pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertEquals("void m() {\n    \n}", buffer.getDocument().getText());
        buffer.undo();
        assertEquals("void m() {}", buffer.getDocument().getText());
        assertFalse(buffer.canUndo());
    }

    @Test
    void documentVersionIncrementsOnSmartEnter() {
        EditorBuffer buffer = buffer("void m() {}");
        long before = buffer.getDocument().currentVersion();
        caretAt(buffer, 10);
        pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertTrue(buffer.getDocument().currentVersion() > before);
    }

    @Test
    void undoOfSmartEnterAndDelimiterAreIndependentSteps() {
        EditorBuffer buffer = buffer("");
        pipeline().process(EditorInputEvent.characterTyped('(', 0,
                buffer.getDocument().currentVersion(), Set.of()), new EditorCommandContext(buffer));
        assertEquals("()", buffer.getDocument().getText());
        buffer.undo();
        assertEquals("", buffer.getDocument().getText());

        EditorBuffer other = buffer("void m() {}");
        caretAt(other, 10);
        pipeline().process(enter(other), new EditorCommandContext(other));
        other.undo();
        assertEquals("void m() {}", other.getDocument().getText());
        assertFalse(other.canUndo());
    }

    @Test
    void delimiterSkipOverStillWorksWithDefaultRegistry() {
        EditorBuffer buffer = buffer("()");
        caretAt(buffer, 1);
        SmartEditResult result = pipeline().process(EditorInputEvent.characterTyped(')', 1,
                buffer.getDocument().currentVersion(), Set.of()), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("()", buffer.getDocument().getText());
        assertEquals(2, caretOffset(buffer));
    }

    @Test
    void enterWithSelectionThroughFullChainReplacesSelection() {
        EditorBuffer buffer = buffer("class A {\n    foo");
        EditorInputEvent event = EditorInputEvent.keyPressed("ENTER", 17,
                buffer.getDocument().currentVersion(), Set.of(), new TextRange(14, 17));
        SmartEditResult result = pipeline().process(event, new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("class A {\n    \n    ", buffer.getDocument().getText());
    }

    @Test
    void smartEnterWinsOverAutoIndentForBraceCases() {
        EditorBuffer buffer = buffer("void m() {");
        caretAt(buffer, 10);
        SmartEditResult result = pipeline().process(enter(buffer), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("void m() {\n    ", buffer.getDocument().getText());
        assertEquals(new EditorPosition(1, 4), buffer.getCaret());
    }
}
