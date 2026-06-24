package com.eyecode.editor.v2.diagnostics;

import com.eyecode.editor.v2.EditorDocument;

public interface DiagnosticEngine {

    DiagnosticSnapshot analyze(EditorDocument document);
}
