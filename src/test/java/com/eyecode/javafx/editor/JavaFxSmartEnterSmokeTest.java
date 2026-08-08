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
 * Smoke test for Sprint 5.1c on the JavaFX side: a native JavaFX ENTER key
 * press flows through the {@link JavaFxEditorInputAdapter} and the smart
 * editing pipeline into the document (smart enter + auto-indent).
 */
class JavaFxSmartEnterSmokeTest {

    private static TypingPipeline pipeline() {
        return new TypingPipeline(SmartEditingStrategies.defaultRegistry());
    }

    private static KeyEvent pressedEnter() {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false);
    }

    @Test
    void enterKeyPressSplitsSameLineBracesThroughAdapter() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, "void m() {}"));
        buffer.moveCaret(buffer.getDocument().positionOf(10));
        JavaFxEditorInputAdapter adapter = new JavaFxEditorInputAdapter();
        EditorInputEvent input = adapter.adapt(pressedEnter(), 10,
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
        JavaFxEditorInputAdapter adapter = new JavaFxEditorInputAdapter();
        EditorInputEvent input = adapter.adapt(pressedEnter(), 10,
                buffer.getDocument().currentVersion(), new TextRange(10, 10));

        SmartEditResult result = pipeline().process(input, new EditorCommandContext(buffer));

        assertTrue(result.isHandled());
        assertEquals("    int x;\n    ", buffer.getDocument().getText());
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
