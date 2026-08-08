package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent.EditorModifier;

/**
 * Input predicates shared by the delimiter strategies.
 * <p>
 * Smart delimiter editing applies only to plain character input: shortcut
 * combinations (CONTROL/ALT/META) are never claimed so native behavior wins.
 * SHIFT stays allowed because quote characters are produced with SHIFT on most
 * keyboard layouts.
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
}
