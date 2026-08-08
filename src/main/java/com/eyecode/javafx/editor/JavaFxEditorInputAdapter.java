package com.eyecode.javafx.editor;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent.EditorModifier;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Thin translation layer between JavaFX {@link KeyEvent}s and Core
 * {@link EditorInputEvent}s.
 * <p>
 * This adapter contains no editing rules: it only maps the native event (plus
 * the current caret offset, document version and selection, supplied by the
 * caller) onto the UI-free event model.
 */
public final class JavaFxEditorInputAdapter {

    public EditorInputEvent adapt(KeyEvent event,
                                  int caretOffset,
                                  long documentVersion,
                                  TextRange selection) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        Set<EditorModifier> modifiers = modifiers(event);
        if (event.getEventType() == KeyEvent.KEY_TYPED) {
            String character = event.getCharacter();
            char typed = character == null || character.isEmpty() ? '\0' : character.charAt(0);
            return EditorInputEvent.characterTyped(typed, caretOffset, documentVersion, modifiers, selection);
        }
        if (event.getEventType() == KeyEvent.KEY_PRESSED) {
            return EditorInputEvent.keyPressed(keyName(event), caretOffset, documentVersion, modifiers, selection);
        }
        if (event.getEventType() == KeyEvent.KEY_RELEASED) {
            return EditorInputEvent.keyReleased(keyName(event), caretOffset, documentVersion, modifiers, selection);
        }
        throw new IllegalArgumentException("Unsupported key event type: " + event.getEventType());
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
        KeyCode code = event.getCode();
        if (code == null || code == KeyCode.UNDEFINED) {
            return "";
        }
        if (code.isLetterKey() || code.isDigitKey()) {
            String text = event.getText();
            if (text != null && !text.isEmpty()) {
                return text;
            }
        }
        return code.getName().toUpperCase(Locale.ROOT).replace(" ", "_");
    }
}
