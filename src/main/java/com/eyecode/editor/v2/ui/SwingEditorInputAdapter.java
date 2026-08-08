package com.eyecode.editor.v2.ui;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent.EditorModifier;

import java.awt.event.KeyEvent;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Thin translation layer between Swing {@link KeyEvent}s and Core
 * {@link EditorInputEvent}s.
 * <p>
 * This adapter contains no editing rules: it only maps the native event (plus
 * the current caret offset, document version and selection, supplied by the
 * caller) onto the UI-free event model.
 */
public final class SwingEditorInputAdapter {

    public EditorInputEvent adapt(KeyEvent event,
                                  int caretOffset,
                                  long documentVersion,
                                  TextRange selection) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        Set<EditorModifier> modifiers = modifiers(event);
        switch (event.getID()) {
            case KeyEvent.KEY_TYPED -> {
                char character = event.getKeyChar();
                return EditorInputEvent.characterTyped(character, caretOffset, documentVersion, modifiers, selection);
            }
            case KeyEvent.KEY_PRESSED -> {
                return EditorInputEvent.keyPressed(keyName(event), caretOffset, documentVersion, modifiers, selection);
            }
            case KeyEvent.KEY_RELEASED -> {
                return EditorInputEvent.keyReleased(keyName(event), caretOffset, documentVersion, modifiers, selection);
            }
            default -> throw new IllegalArgumentException("Unsupported key event id: " + event.getID());
        }
    }

    private Set<EditorModifier> modifiers(KeyEvent event) {
        EnumSet<EditorModifier> modifiers = EnumSet.noneOf(EditorModifier.class);
        if (event.isShiftDown()) {
            modifiers.add(EditorModifier.SHIFT);
        }
        if (event.isControlDown()) {
            modifiers.add(EditorModifier.CONTROL);
        }
        if (event.isAltDown()) {
            modifiers.add(EditorModifier.ALT);
        }
        if (event.isMetaDown()) {
            modifiers.add(EditorModifier.META);
        }
        return modifiers;
    }

    private String keyName(KeyEvent event) {
        char character = event.getKeyChar();
        if (character != KeyEvent.CHAR_UNDEFINED
                && !Character.isISOControl(character)) {
            return String.valueOf(character);
        }
        return KeyEvent.getKeyText(event.getKeyCode())
                .toUpperCase(Locale.ROOT)
                .replace(" ", "_");
    }
}
