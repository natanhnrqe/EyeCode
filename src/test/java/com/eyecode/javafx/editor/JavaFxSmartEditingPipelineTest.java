package com.eyecode.javafx.editor;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.SmartEditResult;
import com.eyecode.editor.intelligence.pipeline.TypingPipeline;
import com.eyecode.editor.intelligence.pipeline.strategy.SmartEditingStrategies;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test for the JavaFX adapter: a native JavaFX key event flows through
 * the {@link JavaFxEditorInputAdapter} and the smart editing pipeline into the
 * document. Mirrors the acceptance criterion of section 17 of Sprint 5.1b.
 */
class JavaFxSmartEditingPipelineTest {

    private static TypingPipeline pipeline() {
        return new TypingPipeline(SmartEditingStrategies.defaultRegistry());
    }

    private static KeyEvent typed(char character) {
        return new KeyEvent(KeyEvent.KEY_TYPED, String.valueOf(character), String.valueOf(character),
                KeyCode.UNDEFINED, false, false, false, false);
    }

    private static EditorInputEvent adapt(JavaFxEditorInputAdapter adapter, KeyEvent event,
                                          EditorBuffer buffer, int offset) {
        return adapter.adapt(event, offset, buffer.getDocument().currentVersion(), new TextRange(offset, offset));
    }

    @Test
    void typedParenthesisThroughAdapterAndPipelineProducesPair() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument());
        JavaFxEditorInputAdapter adapter = new JavaFxEditorInputAdapter();
        EditorInputEvent input = adapt(adapter, typed('('), buffer, 0);

        SmartEditResult result = pipeline().process(input, new EditorCommandContext(buffer));

        assertTrue(result.isHandled());
        assertEquals("()", buffer.getDocument().getText());
        assertEquals(1, buffer.getDocument().offsetOf(buffer.getCaret()));
    }

    @Test
    void typedClosingParenthesisThroughAdapterSkipsExistingPair() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, "()"));
        buffer.moveCaret(buffer.getDocument().positionOf(1));
        JavaFxEditorInputAdapter adapter = new JavaFxEditorInputAdapter();
        EditorInputEvent input = adapt(adapter, typed(')'), buffer, 1);

        SmartEditResult result = pipeline().process(input, new EditorCommandContext(buffer));

        assertTrue(result.isHandled());
        assertEquals("()", buffer.getDocument().getText());
        assertEquals(2, buffer.getDocument().offsetOf(buffer.getCaret()));
    }

    @Test
    void typedQuoteThroughAdapterProducesQuotePair() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument());
        JavaFxEditorInputAdapter adapter = new JavaFxEditorInputAdapter();
        EditorInputEvent input = adapt(adapter, typed('"'), buffer, 0);

        SmartEditResult result = pipeline().process(input, new EditorCommandContext(buffer));

        assertTrue(result.isHandled());
        assertEquals("\"\"", buffer.getDocument().getText());
    }

    @Test
    void commonCharacterThroughAdapterIsNotIntercepted() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, "ab"));
        JavaFxEditorInputAdapter adapter = new JavaFxEditorInputAdapter();
        EditorInputEvent input = adapt(adapter, typed('c'), buffer, 1);

        SmartEditResult result = pipeline().process(input, new EditorCommandContext(buffer));

        assertFalse(result.isHandled());
        assertEquals("ab", buffer.getDocument().getText());
    }
}
