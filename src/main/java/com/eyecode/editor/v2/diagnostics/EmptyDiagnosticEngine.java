package com.eyecode.editor.v2.diagnostics;

import com.eyecode.editor.v2.EditorDocument;

public final class EmptyDiagnosticEngine implements DiagnosticEngine {

    @Override
    public DiagnosticSnapshot analyze(EditorDocument document) {
        return DiagnosticSnapshot.empty();
    }
}
