package com.eyecode.editor.intelligence.pipeline;

import com.eyecode.editor.intelligence.document.TextRange;

import java.util.EnumSet;
import java.util.Set;

/**
 * Raw, UI-free representation of an editor input.
 * <p>
 * Views translate native key events into these value objects through thin
 * adapters before handing them to the {@link TypingPipeline}. The event never
 * references Swing or JavaFX types; {@link EditorModifier} is the Core-level
 * modifier model.
 *
 * @param kind            the kind of key event
 * @param key             canonical key identifier ({@code "("}, {@code "ENTER"},
 *                        {@code "BACK_SPACE"}, ...); for character events it is
 *                        the typed character as a single-char string
 * @param character       the typed character, or {@code '\0'} when not applicable
 * @param modifiers       the modifier keys held when the event was produced
 * @param offset          the caret offset in the document at event time
 * @param documentVersion the document version at event time
 * @param selection       the selection range at event time (empty when collapsed)
 */
public record EditorInputEvent(InputKind kind,
                               String key,
                               char character,
                               Set<EditorModifier> modifiers,
                               int offset,
                               long documentVersion,
                               TextRange selection) {

    public enum InputKind {
        CHARACTER_TYPED,
        KEY_PRESSED,
        KEY_RELEASED
    }

    public enum EditorModifier {
        SHIFT,
        CONTROL,
        ALT,
        META
    }

    public EditorInputEvent {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset < 0: " + offset);
        }
        key = key == null ? "" : key;
        modifiers = modifiers == null ? Set.of() : Set.copyOf(modifiers);
        selection = selection == null ? new TextRange(offset, offset) : selection;
    }

    public static EditorInputEvent characterTyped(char character,
                                                  int offset,
                                                  long documentVersion,
                                                  Set<EditorModifier> modifiers) {
        return characterTyped(character, offset, documentVersion, modifiers, new TextRange(offset, offset));
    }

    public static EditorInputEvent characterTyped(char character,
                                                  int offset,
                                                  long documentVersion,
                                                  Set<EditorModifier> modifiers,
                                                  TextRange selection) {
        return new EditorInputEvent(
                InputKind.CHARACTER_TYPED,
                String.valueOf(character),
                character,
                modifiers,
                offset,
                documentVersion,
                selection
        );
    }

    public static EditorInputEvent keyPressed(String key,
                                              int offset,
                                              long documentVersion,
                                              Set<EditorModifier> modifiers) {
        return keyPressed(key, offset, documentVersion, modifiers, new TextRange(offset, offset));
    }

    public static EditorInputEvent keyPressed(String key,
                                              int offset,
                                              long documentVersion,
                                              Set<EditorModifier> modifiers,
                                              TextRange selection) {
        return new EditorInputEvent(InputKind.KEY_PRESSED, key, '\0', modifiers, offset, documentVersion, selection);
    }

    public static EditorInputEvent keyReleased(String key,
                                               int offset,
                                               long documentVersion,
                                               Set<EditorModifier> modifiers) {
        return keyReleased(key, offset, documentVersion, modifiers, new TextRange(offset, offset));
    }

    public static EditorInputEvent keyReleased(String key,
                                               int offset,
                                               long documentVersion,
                                               Set<EditorModifier> modifiers,
                                               TextRange selection) {
        return new EditorInputEvent(InputKind.KEY_RELEASED, key, '\0', modifiers, offset, documentVersion, selection);
    }

    public boolean isCharacterTyped() {
        return kind == InputKind.CHARACTER_TYPED;
    }

    public boolean isKeyPressed() {
        return kind == InputKind.KEY_PRESSED;
    }

    public boolean isKeyReleased() {
        return kind == InputKind.KEY_RELEASED;
    }

    public boolean hasModifier(EditorModifier modifier) {
        return modifier != null && modifiers.contains(modifier);
    }
}
