package com.eyecode.javafx.editor;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent.EditorModifier;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent.InputKind;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JavaFxEditorInputAdapterTest {

    private final JavaFxEditorInputAdapter adapter = new JavaFxEditorInputAdapter();

    private static KeyEvent typed(char character) {
        return new KeyEvent(KeyEvent.KEY_TYPED, String.valueOf(character), String.valueOf(character),
                KeyCode.UNDEFINED, false, false, false, false);
    }

    @Test
    void keyTypedBecomesCharacterTyped() {
        EditorInputEvent event = adapter.adapt(typed('('), 3, 7, new TextRange(3, 3));

        assertEquals(InputKind.CHARACTER_TYPED, event.kind());
        assertTrue(event.isCharacterTyped());
        assertEquals('(', event.character());
        assertEquals("(", event.key());
        assertEquals(3, event.offset());
        assertEquals(7, event.documentVersion());
        assertTrue(event.modifiers().isEmpty());
    }

    @Test
    void keyTypedCarriesSelection() {
        EditorInputEvent event = adapter.adapt(typed('a'), 2, 5, new TextRange(1, 4));
        assertEquals(new TextRange(1, 4), event.selection());
    }

    @Test
    void keyPressedEnterUsesCanonicalName() {
        KeyEvent pressed = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER,
                false, false, false, false);
        EditorInputEvent event = adapter.adapt(pressed, 0, 1, new TextRange(0, 0));

        assertEquals(InputKind.KEY_PRESSED, event.kind());
        assertTrue(event.isKeyPressed());
        assertEquals("ENTER", event.key());
    }

    @Test
    void keyPressedBackspaceUsesCanonicalName() {
        KeyEvent pressed = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.BACK_SPACE,
                false, false, false, false);
        EditorInputEvent event = adapter.adapt(pressed, 4, 1, new TextRange(3, 4));

        assertEquals("BACKSPACE", event.key());
    }

    @Test
    void keyPressedLetterCarriesTypedText() {
        KeyEvent pressed = new KeyEvent(KeyEvent.KEY_PRESSED, "", "b", KeyCode.B,
                false, false, false, false);
        EditorInputEvent event = adapter.adapt(pressed, 0, 1, new TextRange(0, 0));

        assertEquals("b", event.key());
        assertEquals('\0', event.character());
    }

    @Test
    void keyPressedCarriesShiftModifier() {
        KeyEvent pressed = new KeyEvent(KeyEvent.KEY_PRESSED, "", "B", KeyCode.B,
                true, false, false, false);
        EditorInputEvent event = adapter.adapt(pressed, 0, 1, new TextRange(0, 0));

        assertTrue(event.hasModifier(EditorModifier.SHIFT));
        assertFalse(event.hasModifier(EditorModifier.CONTROL));
    }

    @Test
    void keyPressedCarriesControlModifier() {
        KeyEvent pressed = new KeyEvent(KeyEvent.KEY_PRESSED, "", "z", KeyCode.Z,
                false, true, false, false);
        EditorInputEvent event = adapter.adapt(pressed, 0, 1, new TextRange(0, 0));

        assertTrue(event.hasModifier(EditorModifier.CONTROL));
    }

    @Test
    void keyReleasedBecomesKeyReleased() {
        KeyEvent released = new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.TAB,
                false, false, false, false);
        EditorInputEvent event = adapter.adapt(released, 1, 2, new TextRange(1, 1));

        assertEquals(InputKind.KEY_RELEASED, event.kind());
        assertTrue(event.isKeyReleased());
        assertEquals("TAB", event.key());
    }

    @Test
    void adaptRejectsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> adapter.adapt(null, 0, 0, new TextRange(0, 0)));
    }
}
