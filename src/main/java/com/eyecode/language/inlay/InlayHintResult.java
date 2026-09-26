package com.eyecode.language.inlay;

import java.util.List;

public record InlayHintResult(List<InlayHint> hints) {
    public InlayHintResult {
        hints = hints == null ? List.of() : List.copyOf(hints);
    }
}
