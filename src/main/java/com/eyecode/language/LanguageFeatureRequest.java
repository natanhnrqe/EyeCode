package com.eyecode.language;

public record LanguageFeatureRequest(LanguageDocument document, long version, String source, int caretOffset) {
    public LanguageFeatureRequest {
        if (document == null) throw new IllegalArgumentException("document must not be null");
        source = source == null ? "" : source;
        caretOffset = Math.max(0, Math.min(caretOffset, source.length()));
    }
}
