package com.eyecode.workbench.editor;

import com.eyecode.eventbus.EventBus;
import com.eyecode.language.java.JavaEditorIntelligence;

final class EditorIntelligenceDefaults {
    private EditorIntelligenceDefaults() {
    }

    static EditorIntelligence create(EventBus eventBus) {
        return new JavaEditorIntelligence(eventBus);
    }
}
