package com.eyecode.editor.v2.ui;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.SmartEditResult;
import com.eyecode.editor.intelligence.pipeline.TypingPipeline;
import com.eyecode.editor.intelligence.pipeline.strategy.SmartEditingStrategies;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorPosition;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test for Sprint 5.1d on the Swing side: native HOME, END and BACK_SPACE
 * key presses flow through the {@link SwingEditorInputAdapter} and the smart
 * editing pipeline into the Core (caret moves + smart dedent). Unhandled events
 * are reported as not handled so the native editor keeps them.
 */
class SwingSmartNavigationSmokeTest {

    private static final JPanel SOURCE = new JPanel();
    private static final long WHEN = System.currentTimeMillis();

    private static TypingPipeline pipeline() {
        return new TypingPipeline(SmartEditingStrategies.defaultRegistry());
    }

    private static EditorInputEvent press(EditorBuffer buffer, int keyCode) {
        KeyEvent keyEvent = new KeyEvent(SOURCE, KeyEvent.KEY_PRESSED, WHEN, 0, keyCode, KeyEvent.CHAR_UNDEFINED);
        return new SwingEditorInputAdapter().adapt(keyEvent, buffer.getDocument().offsetOf(buffer.getCaret()),
                buffer.getDocument().currentVersion(), new TextRange(0, 0));
    }

    @Test
    void homeKeyPressMovesToFirstNonWhitespaceThroughAdapter() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, "    int x;"));
        buffer.moveCaret(buffer.getDocument().positionOf(12));
        SmartEditResult result = pipeline().process(press(buffer, KeyEvent.VK_HOME), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals(new EditorPosition(0, 4), buffer.getCaret());
    }

    @Test
    void endKeyPressMovesToLogicalEndThroughAdapter() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, "int x;   "));
        buffer.moveCaret(buffer.getDocument().positionOf(0));
        SmartEditResult result = pipeline().process(press(buffer, KeyEvent.VK_END), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals(new EditorPosition(0, 6), buffer.getCaret());
    }

    @Test
    void backspaceKeyPressDedentsThroughAdapter() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, "        int x;"));
        buffer.moveCaret(buffer.getDocument().positionOf(8));
        SmartEditResult result = pipeline().process(press(buffer, KeyEvent.VK_BACK_SPACE), new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("    int x;", buffer.getDocument().getText());
        assertEquals(new EditorPosition(0, 4), buffer.getCaret());
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
