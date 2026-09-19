package com.eyecode.language.diagnostics;

import com.eyecode.language.LanguageDocument;

public record DiagnosticsRequest(LanguageDocument document, long version, String source) {
    public DiagnosticsRequest {
        if (document == null) throw new IllegalArgumentException("document must not be null");
        source = source == null ? "" : source;
    }
}
