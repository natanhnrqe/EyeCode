package com.eyecode.language.java.completion;

import com.eyecode.language.completion.CompletionResult;
import com.eyecode.language.completion.CompletionCandidate;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;

final class JavaCompletionProviderRoutingTest {
    @Test
    void emptyJdtResultUsesFallback() {
        CompletionResult emptyJdt = CompletionResult.empty();
        CompletionResult local = new CompletionResult(java.util.List.of(candidate("String", "CLASS", "String")));
        assertSame(local, JavaCompletionProvider.selectJdtOrFallback(Optional.of(emptyJdt), local));
    }

    @Test
    void failedOrUnavailableJdtResultUsesFallback() {
        CompletionResult local = new CompletionResult(java.util.List.of(candidate("String", "CLASS", "String")));
        assertSame(local, JavaCompletionProvider.selectJdtOrFallback(Optional.empty(), local));
    }

    @Test
    void memberResultsKeepLocalMetadataAndAddOnlyContextualJdtItems() {
        CompletionCandidate localMethod = candidate("charAt", "METHOD", "String.charAt(int index) : char");
        CompletionCandidate duplicateJdt = candidate("charAt(int index)", "METHOD", "String.charAt(int index) : char");
        CompletionCandidate distinctJdt = candidate("codePointAt(int index)", "METHOD", "String.codePointAt(int index) : int");

        CompletionResult merged = JavaCompletionProvider.mergeMemberResults(
                new CompletionResult(java.util.List.of(localMethod)),
                new CompletionResult(java.util.List.of(duplicateJdt, distinctJdt)), "c");

        org.junit.jupiter.api.Assertions.assertEquals(2, merged.candidates().size());
        assertSame(localMethod, merged.candidates().get(0));
        org.junit.jupiter.api.Assertions.assertEquals("codePointAt(int index)", merged.candidates().get(1).label());
    }

    private static CompletionCandidate candidate(String label, String kind, String detail) {
        return new CompletionCandidate(label, kind, detail, "documentation", label + "()", label, false,
                0, 0, 0, detail, "", "String", "example", "java", java.util.List.of());
    }
}
