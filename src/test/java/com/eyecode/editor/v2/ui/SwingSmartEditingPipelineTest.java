package com.eyecode.editor.v2.ui;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.SmartEditResult;
import com.eyecode.editor.intelligence.pipeline.TypingPipeline;
import com.eyecode.editor.intelligence.pipeline.strategy.SmartEditingStrategies;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test for the Swing adapter: a native AWT key event flows through the
 * {@link SwingEditorInputAdapter} and the smart editing pipeline into the
 * document, proving the Sprint 5.1b behaviors stay adapter-driven.
 */
class SwingSmartEditingPipelineTest {

    private static final JPanel SOURCE = new JPanel();
    private static final long WHEN = System.currentTimeMillis();

    private static TypingPipeline pipeline() {
        return new TypingPipeline(SmartEditingStrategies.defaultRegistry());
    }

    @Test
    void typedParenthesisThroughAdapterAndPipelineProducesPair() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument());
        SwingEditorInputAdapter adapter = new SwingEditorInputAdapter();
        KeyEvent keyEvent = new KeyEvent(SOURCE, KeyEvent.KEY_TYPED, WHEN, 0, KeyEvent.VK_UNDEFINED, '(');
        EditorInputEvent input = adapter.adapt(keyEvent, 0, buffer.getDocument().currentVersion(), new TextRange(0, 0));

        SmartEditResult result = pipeline().process(input, new EditorCommandContext(buffer));

        assertTrue(result.isHandled());
        assertEquals("()", buffer.getDocument().getText());
        assertEquals(1, buffer.getDocument().offsetOf(buffer.getCaret()));
    }

    @Test
    void typedClosingParenthesisThroughAdapterSkipsExistingPair() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, "()"));
        buffer.moveCaret(buffer.getDocument().positionOf(1));
        SwingEditorInputAdapter adapter = new SwingEditorInputAdapter();
        KeyEvent keyEvent = new KeyEvent(SOURCE, KeyEvent.KEY_TYPED, WHEN, 0, KeyEvent.VK_UNDEFINED, ')');
        EditorInputEvent input = adapter.adapt(keyEvent, 1, buffer.getDocument().currentVersion(), new TextRange(1, 1));

        SmartEditResult result = pipeline().process(input, new EditorCommandContext(buffer));

        assertTrue(result.isHandled());
        assertEquals("()", buffer.getDocument().getText());
        assertEquals(2, buffer.getDocument().offsetOf(buffer.getCaret()));
    }

    @Test
    void typedQuoteThroughAdapterProducesQuotePair() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument());
        SwingEditorInputAdapter adapter = new SwingEditorInputAdapter();
        KeyEvent keyEvent = new KeyEvent(SOURCE, KeyEvent.KEY_TYPED, WHEN, 0, KeyEvent.VK_UNDEFINED, '"');
        EditorInputEvent input = adapter.adapt(keyEvent, 0, buffer.getDocument().currentVersion(), new TextRange(0, 0));

        SmartEditResult result = pipeline().process(input, new EditorCommandContext(buffer));

        assertTrue(result.isHandled());
        assertEquals("\"\"", buffer.getDocument().getText());
    }

    @Test
    void commonCharacterThroughAdapterIsNotIntercepted() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, "ab"));
        SwingEditorInputAdapter adapter = new SwingEditorInputAdapter();
        KeyEvent keyEvent = new KeyEvent(SOURCE, KeyEvent.KEY_TYPED, WHEN, 0, KeyEvent.VK_UNDEFINED, 'c');
        EditorInputEvent input = adapter.adapt(keyEvent, 1, buffer.getDocument().currentVersion(), new TextRange(1, 1));

        SmartEditResult result = pipeline().process(input, new EditorCommandContext(buffer));

        assertFalse(result.isHandled());
        assertEquals("ab", buffer.getDocument().getText());
    }
}
