package com.eyecode.editor.v2.diagnostics;

import com.eyecode.editor.v2.EditorDocument;

public final class DiagnosticManager {

    private final DiagnosticEngine engine;
    private DiagnosticSnapshot currentSnapshot;

    public DiagnosticManager(DiagnosticEngine engine) {
        this.engine = engine;
        this.currentSnapshot = DiagnosticSnapshot.empty();
    }

    public void refresh(EditorDocument document) {
        currentSnapshot = engine.analyze(document);
        if (currentSnapshot == null) {
            currentSnapshot = DiagnosticSnapshot.empty();
        }
    }

    public DiagnosticSnapshot getSnapshot() {
        return currentSnapshot;
    }
}
