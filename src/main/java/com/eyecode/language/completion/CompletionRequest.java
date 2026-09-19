package com.eyecode.language.completion;

import com.eyecode.language.LanguageDocument;

public record CompletionRequest(LanguageDocument document, long version, String source, int caretOffset,
                                boolean explicit, int replaceStart, int replaceEnd) {
    public CompletionRequest {
        if (document == null) throw new IllegalArgumentException("document must not be null");
        source = source == null ? "" : source;
        caretOffset = Math.max(0, Math.min(caretOffset, source.length()));
        replaceStart = Math.max(-1, replaceStart);
        replaceEnd = Math.max(-1, replaceEnd);
    }
}
