package com.eyecode.language.java.completion;

import com.eyecode.language.completion.CompletionResult;
import com.eyecode.language.completion.CompletionCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

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

    @Test
    void memberMergeRanksLocalAndJdtCandidatesTogetherWithoutLosingMetadata() {
        CompletionCandidate local = candidate("apple", "METHOD", "local");
        CompletionCandidate serverFirst = candidate("cat(int value)", "METHOD", "server cat");
        CompletionCandidate serverSecond = new CompletionCandidate("ant(int value)", "METHOD", "server ant",
                "jdt docs", "ant(7)", "ant", false, 4, 5, 0,
                "ant(int value)", "int", "Example", "code example", "Method", List.of(0, 1, 2));

        CompletionResult merged = JavaCompletionProvider.mergeMemberResults(
                new CompletionResult(List.of(local)),
                new CompletionResult(List.of(serverFirst, serverSecond)), "a", 4, 5);

        assertEquals(List.of("apple", "ant(int value)", "cat(int value)"),
                merged.candidates().stream().map(CompletionCandidate::label).toList());
        assertNotSame(serverSecond, merged.candidates().get(1));
        assertEquals("ant(7)", merged.candidates().get(1).insertText());
        assertEquals("code example", merged.candidates().get(1).example());
        assertEquals("jdt docs", merged.candidates().get(1).documentation());
        assertEquals(List.of(0), merged.candidates().get(1).matchIndices());
    }

    private static CompletionCandidate candidate(String label, String kind, String detail) {
        return new CompletionCandidate(label, kind, detail, "documentation", label + "()", label, false,
                0, 0, 0, detail, "", "String", "example", "java", java.util.List.of());
    }
}
