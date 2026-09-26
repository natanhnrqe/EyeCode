package com.eyecode.language.inlay;

public record InlayHint(int offset, String label) {
    public InlayHint {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
    }
}
