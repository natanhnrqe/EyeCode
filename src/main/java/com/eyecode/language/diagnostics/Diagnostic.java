package com.eyecode.language.diagnostics;

public record Diagnostic(DiagnosticSeverity severity, String code, String message,
                         int startLine, int startColumn, int endLine, int endColumn,
                         String category) {
    public Diagnostic(DiagnosticSeverity severity, String code, String message,
                      int startLine, int startColumn, int endLine, int endColumn) {
        this(severity, code, message, startLine, startColumn, endLine, endColumn, "");
    }

    public Diagnostic {
        severity = severity == null ? DiagnosticSeverity.HINT : severity;
        code = code == null ? "" : code;
        message = message == null ? "" : message;
        startLine = Math.max(1, startLine);
        startColumn = Math.max(1, startColumn);
        endLine = Math.max(startLine, endLine);
        endColumn = endLine == startLine ? Math.max(startColumn + 1, endColumn) : Math.max(1, endColumn);
        category = category == null ? "" : category;
    }
}
