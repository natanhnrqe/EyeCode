package com.eyecode.language.java.completion;

import com.eyecode.language.completion.CompletionResult;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;

final class JavaCompletionProviderRoutingTest {
    @Test
    void validEmptyJdtResultDoesNotInvokeFallback() {
        CompletionResult emptyJdt = CompletionResult.empty();
        CompletionResult local = new CompletionResult(java.util.List.of());
        assertSame(emptyJdt, JavaCompletionProvider.selectJdtOrFallback(Optional.of(emptyJdt), local));
    }

    @Test
    void failedOrUnavailableJdtResultUsesFallback() {
        CompletionResult local = new CompletionResult(java.util.List.of());
        assertSame(local, JavaCompletionProvider.selectJdtOrFallback(Optional.empty(), local));
    }
}
