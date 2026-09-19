package com.eyecode.language.diagnostics;

import java.util.List;

public record DiagnosticsResult(List<Diagnostic> diagnostics, String infrastructureError) {
    public DiagnosticsResult {
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        infrastructureError = infrastructureError == null ? "" : infrastructureError;
    }

    public static DiagnosticsResult empty() {
        return new DiagnosticsResult(List.of(), "");
    }

    public boolean hasInfrastructureError() {
        return !infrastructureError.isBlank();
    }
}
