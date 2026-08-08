package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent.EditorModifier;

/**
 * Input predicates shared by the smart editing strategies.
 * <p>
 * Smart editing applies only to plain key input: shortcut combinations
 * (CONTROL/ALT/META) are never claimed so native behavior wins. SHIFT stays
 * allowed for keys where it carries no selection semantics (quote characters,
 * Enter, Backspace); navigation keys (HOME/END) reject SHIFT because
 * Shift+Home/Shift+End must keep extending the selection natively.
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
}
