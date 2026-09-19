package com.eyecode.language.completion;

import java.util.List;

public record CompletionResult(List<CompletionCandidate> candidates) {
    public CompletionResult {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }

    public static CompletionResult empty() {
        return new CompletionResult(List.of());
    }
}
