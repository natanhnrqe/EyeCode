package com.eyecode.language.refactor;

import java.util.Objects;

public record PrepareRenameResult(int startOffset, int endOffset, String placeholder, int line, int character) {
    public PrepareRenameResult {
        if (startOffset < 0 || endOffset < startOffset) {
            throw new IllegalArgumentException("Invalid rename range");
        }
        Objects.requireNonNull(placeholder, "placeholder");
    }
}
