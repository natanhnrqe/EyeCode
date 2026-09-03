package com.eyecode.diagnostics;

public record JavaDiagnostic(
        String uri,
        String requestId,
        long modelVersion,
        JavaDiagnosticSeverity severity,
        String code,
        String message,
        int startLine,
        int startColumn,
        int endLine,
        int endColumn
) {
    public JavaDiagnostic {
        uri = uri == null ? "" : uri;
        requestId = requestId == null ? "" : requestId;
        severity = severity == null ? JavaDiagnosticSeverity.HINT : severity;
        code = code == null ? "" : code;
        message = message == null ? "" : message;
        startLine = Math.max(1, startLine);
        startColumn = Math.max(1, startColumn);
        endLine = Math.max(startLine, endLine);
        endColumn = endLine == startLine ? Math.max(startColumn + 1, endColumn) : Math.max(1, endColumn);
    }
}