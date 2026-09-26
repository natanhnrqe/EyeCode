package com.eyecode.language.inlay;

import com.eyecode.language.LanguageDocument;

public record InlayHintRequest(LanguageDocument document, long version, String source, int fromOffset, int toOffset,
                               InlayHintMode mode) {
    public InlayHintRequest {
        if (document == null) {
            throw new IllegalArgumentException("document must not be null");
        }
        source = source == null ? "" : source;
        fromOffset = Math.max(0, Math.min(fromOffset, source.length()));
        toOffset = Math.max(0, Math.min(toOffset, source.length()));
        if (toOffset < fromOffset) {
            int swap = fromOffset;
            fromOffset = toOffset;
            toOffset = swap;
        }
        mode = mode == null ? InlayHintMode.NAME : mode;
    }
}
