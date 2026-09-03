package com.eyecode.diagnostics;

import java.util.List;

public record JavaDiagnosticsResult(JavaDiagnosticRequest request, List<JavaDiagnostic> diagnostics,
                                    String infrastructureError) {
    public JavaDiagnosticsResult {
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        infrastructureError = infrastructureError == null ? "" : infrastructureError;
    }

    public boolean hasInfrastructureError() {
        return !infrastructureError.isBlank();
    }
}