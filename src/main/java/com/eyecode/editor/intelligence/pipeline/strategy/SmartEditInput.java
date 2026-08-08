package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent.EditorModifier;

import java.util.EnumSet;
import java.util.Set;

/**
 * Input predicates shared by the smart editing strategies.
 * <p>
 * Smart editing applies only to plain key input: shortcut combinations
 * (CONTROL/ALT/META) are never claimed so native behavior wins. SHIFT stays
 * allowed for keys where it carries no selection semantics (quote characters,
 * Enter, Backspace); navigation keys (HOME/END) reject SHIFT because
 * Shift+Home/Shift+End must keep extending the selection natively.
 * <p>
 * Selection expansion is the exception to the plain-key rule: {@code Ctrl+W}
 * and {@code Ctrl+Shift+W} are claimed with an exact modifier set — any extra
 * or missing modifier (e.g. Ctrl+Alt+W, plain W) falls through to native
 * behavior. The key itself is matched case-insensitively because Swing and
 * JavaFX canonicalize letter keys differently ({@code "W"} vs {@code "w"}).
 */
final class SmartEditInput {

    private SmartEditInput() {
    }

    static boolean isPlainCharacterTyped(EditorInputEvent event) {
        return event.isCharacterTyped()
                && !event.hasModifier(EditorModifier.CONTROL)
                && !event.hasModifier(EditorModifier.ALT)
                && !event.hasModifier(EditorModifier.META);
    }

    static boolean isPlainKeyPressed(EditorInputEvent event, String key) {
        return isPlainKeyPressed(event, key, false);
    }

    static boolean isPlainKeyPressed(EditorInputEvent event, String key, boolean rejectShift) {
        return event.isKeyPressed()
                && key.equals(event.key())
                && !event.hasModifier(EditorModifier.CONTROL)
                && !event.hasModifier(EditorModifier.ALT)
                && !event.hasModifier(EditorModifier.META)
                && (!rejectShift || !event.hasModifier(EditorModifier.SHIFT));
    }

    static boolean isExtendSelection(EditorInputEvent event) {
        return isKeyPressedWithExactModifiers(event, "W", EnumSet.of(EditorModifier.CONTROL));
    }

    static boolean isShrinkSelection(EditorInputEvent event) {
        return isKeyPressedWithExactModifiers(event, "W",
                EnumSet.of(EditorModifier.CONTROL, EditorModifier.SHIFT));
    }

    private static boolean isKeyPressedWithExactModifiers(EditorInputEvent event,
                                                          String key,
                                                          Set<EditorModifier> modifiers) {
        return event.isKeyPressed()
                && key.equalsIgnoreCase(event.key())
                && event.modifiers().equals(modifiers);
    }
}
