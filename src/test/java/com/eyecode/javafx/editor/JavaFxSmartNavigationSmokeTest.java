package com.eyecode.javafx.editor;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.SmartEditResult;
import com.eyecode.editor.intelligence.pipeline.TypingPipeline;
import com.eyecode.editor.intelligence.pipeline.strategy.SmartEditingStrategies;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorPosition;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test for Sprint 5.1d on the JavaFX side: native HOME, END and BACK_SPACE
 * key presses flow through the {@link JavaFxEditorInputAdapter} and the smart
 * editing pipeline into the Core. Unhandled events stay unhandled so RichTextFX
 * keeps its native behavior.
 */
class JavaFxSmartNavigationSmokeTest {

    private static TypingPipeline pipeline() {
        return new TypingPipeline(SmartEditingStrategies.defaultRegistry());
    }

    private static KeyEvent pressed(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false);
    }

    @Test
    void homeKeyPressMovesToFirstNonWhitespaceThroughAdapter() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, "    int x;"));
        buffer.moveCaret(buffer.getDocument().positionOf(12));
        JavaFxEditorInputAdapter adapter = new JavaFxEditorInputAdapter();
        EditorInputEvent input = adapter.adapt(pressed(KeyCode.HOME), 12,
                buffer.getDocument().currentVersion(), new TextRange(12, 12));
        SmartEditResult result = pipeline().process(input, new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals(new EditorPosition(0, 4), buffer.getCaret());
    }

    @Test
    void endKeyPressMovesToLogicalEndThroughAdapter() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, "int x;   "));
        buffer.moveCaret(buffer.getDocument().positionOf(0));
        JavaFxEditorInputAdapter adapter = new JavaFxEditorInputAdapter();
        EditorInputEvent input = adapter.adapt(pressed(KeyCode.END), 0,
                buffer.getDocument().currentVersion(), new TextRange(0, 0));
        SmartEditResult result = pipeline().process(input, new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals(new EditorPosition(0, 6), buffer.getCaret());
    }

    @Test
    void backspaceKeyPressDedentsThroughAdapter() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, "        int x;"));
        buffer.moveCaret(buffer.getDocument().positionOf(8));
        JavaFxEditorInputAdapter adapter = new JavaFxEditorInputAdapter();
        EditorInputEvent input = adapter.adapt(pressed(KeyCode.BACK_SPACE), 8,
                buffer.getDocument().currentVersion(), new TextRange(8, 8));
        SmartEditResult result = pipeline().process(input, new EditorCommandContext(buffer));
        assertTrue(result.isHandled());
        assertEquals("    int x;", buffer.getDocument().getText());
        assertEquals(new EditorPosition(0, 4), buffer.getCaret());
    }

    @Test
    void plainTypedCharacterIsNotIntercepted() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, "ab"));
        JavaFxEditorInputAdapter adapter = new JavaFxEditorInputAdapter();
        KeyEvent typed = new KeyEvent(KeyEvent.KEY_TYPED, "c", "c", KeyCode.UNDEFINED,
                false, false, false, false);
        EditorInputEvent input = adapter.adapt(typed, 1,
                buffer.getDocument().currentVersion(), new TextRange(1, 1));
        SmartEditResult result = pipeline().process(input, new EditorCommandContext(buffer));
        assertFalse(result.isHandled());
        assertEquals("ab", buffer.getDocument().getText());
    }
}
