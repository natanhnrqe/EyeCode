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
 * Smoke test for Sprint 5.1c on the Swing side: a native AWT ENTER key press
 * flows through the {@link SwingEditorInputAdapter} and the smart editing
 * pipeline into the document (smart enter + auto-indent).
 */
class SwingSmartEnterSmokeTest {

    private static final JPanel SOURCE = new JPanel();
    private static final long WHEN = System.currentTimeMillis();

    private static TypingPipeline pipeline() {
        return new TypingPipeline(SmartEditingStrategies.defaultRegistry());
    }

    @Test
    void enterKeyPressSplitsSameLineBracesThroughAdapter() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, "void m() {}"));
        buffer.moveCaret(buffer.getDocument().positionOf(10));
        SwingEditorInputAdapter adapter = new SwingEditorInputAdapter();
        KeyEvent keyEvent = new KeyEvent(SOURCE, KeyEvent.KEY_PRESSED, WHEN, 0,
                KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED);
        EditorInputEvent input = adapter.adapt(keyEvent, 10,
                buffer.getDocument().currentVersion(), new TextRange(10, 10));

        SmartEditResult result = pipeline().process(input, new EditorCommandContext(buffer));

        assertTrue(result.isHandled());
        assertEquals("void m() {\n    \n}", buffer.getDocument().getText());
        assertEquals(15, buffer.getDocument().offsetOf(buffer.getCaret()));
    }

    @Test
    void enterKeyPressAutoIndentsThroughAdapter() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, "    int x;"));
        buffer.moveCaret(buffer.getDocument().positionOf(10));
        SwingEditorInputAdapter adapter = new SwingEditorInputAdapter();
        KeyEvent keyEvent = new KeyEvent(SOURCE, KeyEvent.KEY_PRESSED, WHEN, 0,
                KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED);
        EditorInputEvent input = adapter.adapt(keyEvent, 10,
                buffer.getDocument().currentVersion(), new TextRange(10, 10));

        SmartEditResult result = pipeline().process(input, new EditorCommandContext(buffer));

        assertTrue(result.isHandled());
        assertEquals("    int x;\n    ", buffer.getDocument().getText());
    }

    @Test
    void plainTypedCharacterIsNotIntercepted() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, "ab"));
        SwingEditorInputAdapter adapter = new SwingEditorInputAdapter();
        KeyEvent keyEvent = new KeyEvent(SOURCE, KeyEvent.KEY_TYPED, WHEN, 0, KeyEvent.VK_UNDEFINED, 'c');
        EditorInputEvent input = adapter.adapt(keyEvent, 1,
                buffer.getDocument().currentVersion(), new TextRange(1, 1));

        SmartEditResult result = pipeline().process(input, new EditorCommandContext(buffer));

        assertFalse(result.isHandled());
        assertEquals("ab", buffer.getDocument().getText());
    }
}
