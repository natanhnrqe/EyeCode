package com.eyecode.language;

public record LanguageFeatureRequest(LanguageDocument document, long version, String source, int caretOffset,
                                     String triggerCharacter) {
    public LanguageFeatureRequest(LanguageDocument document, long version, String source, int caretOffset) {
        this(document, version, source, caretOffset, "");
    }

    public LanguageFeatureRequest {
        if (document == null) throw new IllegalArgumentException("document must not be null");
        source = source == null ? "" : source;
        caretOffset = Math.max(0, Math.min(caretOffset, source.length()));
        triggerCharacter = triggerCharacter == null ? "" : triggerCharacter;
    }
}
