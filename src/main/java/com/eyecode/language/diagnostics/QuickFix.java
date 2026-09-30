package com.eyecode.language.diagnostics;

import java.util.List;

public record QuickFix(String title, String kind, List<QuickFixEdit> edits) {
    public QuickFix {
        title = title == null ? "" : title;
        kind = kind == null || kind.isBlank() ? "quickfix" : kind;
        edits = edits == null ? List.of() : List.copyOf(edits);
    }
}
