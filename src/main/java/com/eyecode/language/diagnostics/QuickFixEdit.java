package com.eyecode.language.diagnostics;

public record QuickFixEdit(int startLine, int startColumn, int endLine, int endColumn, String newText) {
    public QuickFixEdit {
        startLine = Math.max(1, startLine);
        startColumn = Math.max(1, startColumn);
        endLine = Math.max(startLine, endLine);
        endColumn = Math.max(1, endColumn);
        newText = newText == null ? "" : newText;
    }
}
