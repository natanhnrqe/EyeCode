package com.eyecode.editor.v2.diagnostics;

import java.util.List;

public final class DiagnosticSnapshot {

    private static final DiagnosticSnapshot EMPTY = new DiagnosticSnapshot(List.of());

    private final List<Diagnostic> diagnostics;

    public DiagnosticSnapshot(List<Diagnostic> diagnostics) {
        this.diagnostics = List.copyOf(diagnostics);
    }

    public static DiagnosticSnapshot empty() {
        return EMPTY;
    }

    public List<Diagnostic> getAll() {
        return diagnostics;
    }

    public List<Diagnostic> getErrors() {
        return diagnostics.stream()
                .filter(diagnostic -> diagnostic.getSeverity() == DiagnosticSeverity.ERROR)
                .toList();
    }

    public List<Diagnostic> getWarnings() {
        return diagnostics.stream()
                .filter(diagnostic -> diagnostic.getSeverity() == DiagnosticSeverity.WARNING)
                .toList();
    }

    public boolean hasErrors() {
        return diagnostics.stream()
                .anyMatch(diagnostic -> diagnostic.getSeverity() == DiagnosticSeverity.ERROR);
    }

    public boolean isEmpty() {
        return diagnostics.isEmpty();
    }
}
