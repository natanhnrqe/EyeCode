package com.eyecode.editor.v2.completion;

import com.eyecode.editor.v2.language.LanguageContext;

public final class CompletionManager {

    private final CompletionEngine engine;
    private CompletionSnapshot currentSnapshot;

    public CompletionManager(CompletionEngine engine) {
        this.engine = engine;
        this.currentSnapshot = CompletionSnapshot.empty();
    }

    public void refresh(LanguageContext context) {
        refresh(context, false);
    }

    public void refresh(LanguageContext context, boolean manual) {
        currentSnapshot = engine.complete(context, manual);
        if (currentSnapshot == null) {
            currentSnapshot = CompletionSnapshot.empty();
        }
    }

    public CompletionSnapshot getSnapshot() {
        return currentSnapshot;
    }
}
