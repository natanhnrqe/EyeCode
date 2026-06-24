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
        currentSnapshot = engine.complete(context);
        if (currentSnapshot == null) {
            currentSnapshot = CompletionSnapshot.empty();
        }
    }

    public CompletionSnapshot getSnapshot() {
        return currentSnapshot;
    }
}
