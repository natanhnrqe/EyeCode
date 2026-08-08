package com.eyecode.editor.v2.ui;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent.EditorModifier;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent.InputKind;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.*;

class SwingEditorInputAdapterTest {

    private static final JPanel SOURCE = new JPanel();
    private static final long WHEN = System.currentTimeMillis();

    private final SwingEditorInputAdapter adapter = new SwingEditorInputAdapter();

    @Test
    void keyTypedBecomesCharacterTyped() {
        EditorInputEvent event = adapter.adapt(
                new KeyEvent(SOURCE, KeyEvent.KEY_TYPED, WHEN, 0, KeyEvent.VK_UNDEFINED, '('),
                3, 7, new TextRange(3, 3));

        assertEquals(InputKind.CHARACTER_TYPED, event.kind());
        assertTrue(event.isCharacterTyped());
        assertEquals('(', event.character());
        assertEquals("(", event.key());
        assertEquals(3, event.offset());
        assertEquals(7, event.documentVersion());
        assertTrue(event.modifiers().isEmpty());
    }

    @Test
    void keyTypedCarriesSelectionAndModifiers() {
        EditorInputEvent event = adapter.adapt(
                new KeyEvent(SOURCE, KeyEvent.KEY_TYPED, WHEN, KeyEvent.SHIFT_DOWN_MASK, KeyEvent.VK_UNDEFINED, 'A'),
                2, 5, new TextRange(1, 4));

        assertEquals(new TextRange(1, 4), event.selection());
        assertTrue(event.hasModifier(EditorModifier.SHIFT));
        assertFalse(event.hasModifier(EditorModifier.CONTROL));
    }

    @Test
    void keyPressedBecomesKeyPressedWithCanonicalName() {
        EditorInputEvent event = adapter.adapt(
                new KeyEvent(SOURCE, KeyEvent.KEY_PRESSED, WHEN, 0, KeyEvent.VK_ENTER, '\n'),
                0, 1, new TextRange(0, 0));

        assertEquals(InputKind.KEY_PRESSED, event.kind());
        assertTrue(event.isKeyPressed());
        assertEquals("ENTER", event.key());
    }

    @Test
    void keyPressedBackspaceAndDeleteUseKeyCodeNames() {
        EditorInputEvent backspace = adapter.adapt(
                new KeyEvent(SOURCE, KeyEvent.KEY_PRESSED, WHEN, 0, KeyEvent.VK_BACK_SPACE, '\b'),
                4, 1, new TextRange(3, 4));
        EditorInputEvent delete = adapter.adapt(
                new KeyEvent(SOURCE, KeyEvent.KEY_PRESSED, WHEN, 0, KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED),
                4, 1, new TextRange(4, 4));

        assertEquals("BACKSPACE", backspace.key());
        assertEquals("DELETE", delete.key());
    }

    @Test
    void keyPressedCarriesControlModifier() {
        EditorInputEvent event = adapter.adapt(
                new KeyEvent(SOURCE, KeyEvent.KEY_PRESSED, WHEN, KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_Z, 'z'),
                0, 1, new TextRange(0, 0));

        assertTrue(event.hasModifier(EditorModifier.CONTROL));
    }

    @Test
    void keyReleasedBecomesKeyReleased() {
        EditorInputEvent event = adapter.adapt(
                new KeyEvent(SOURCE, KeyEvent.KEY_RELEASED, WHEN, 0, KeyEvent.VK_TAB, '\t'),
                1, 2, new TextRange(1, 1));

        assertEquals(InputKind.KEY_RELEASED, event.kind());
        assertTrue(event.isKeyReleased());
        assertEquals("TAB", event.key());
    }

    @Test
    void adaptRejectsNullAndUnknownId() {
        assertThrows(IllegalArgumentException.class,
                () -> adapter.adapt(null, 0, 0, new TextRange(0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.adapt(new KeyEvent(SOURCE, KeyEvent.KEY_LOCATION_UNKNOWN, WHEN, 0,
                        KeyEvent.VK_UNDEFINED, '\0'), 0, 0, new TextRange(0, 0)));
    }
}
