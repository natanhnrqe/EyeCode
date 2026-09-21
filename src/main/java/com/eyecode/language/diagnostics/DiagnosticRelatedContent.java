package com.eyecode.language.diagnostics;

public record DiagnosticRelatedContent(String id, String title, String description) {
    public DiagnosticRelatedContent {
        id = id == null ? "" : id;
        title = title == null ? "" : title;
        description = description == null ? "" : description;
    }
}
