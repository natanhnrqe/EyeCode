package com.eyecode.language.java.completion;

import com.eyecode.language.completion.CompletionResult;
import com.eyecode.language.completion.CompletionCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void generalMergeJdtWinsIdentityCollisionsAndKeepsResolveId() {
        CompletionCandidate localType = candidate("LinkedList", "CLASS", "java.util.LinkedList");
        CompletionCandidate localOnly = candidate("LinkedQueue", "CLASS", "LinkedQueue");
        CompletionCandidate jdtType = resolveCandidate("LinkedList", "CLASS", "java.util.LinkedList", "x1");

        CompletionResult merged = JavaCompletionProvider.mergeGeneralResults(
                new CompletionResult(List.of(localType, localOnly)),
                new CompletionResult(List.of(jdtType)), "Linke", 20, 25);

        long linkedListCount = merged.candidates().stream()
                .filter(candidate -> candidate.label().equals("LinkedList")).count();
        assertEquals(1, linkedListCount, () -> "candidates=" + merged.candidates());
        CompletionCandidate winner = merged.candidates().stream()
                .filter(candidate -> candidate.label().equals("LinkedList")).findFirst().orElseThrow();
        assertNotSame(localType, winner);
        assertEquals("x1", winner.resolveId(), () -> "JDT candidate must win to keep auto-import");
        assertTrue(merged.candidates().stream().anyMatch(candidate -> candidate.label().equals("LinkedQueue")),
                () -> "local-only candidates must survive; candidates=" + merged.candidates());
    }

    @Test
    void generalMergeFiltersJdtCandidatesByPrefixSubsequence() {
        CompletionCandidate matching = resolveCandidate("LinkedHashSet", "CLASS", "java.util.LinkedHashSet", "x2");
        CompletionCandidate unrelated = resolveCandidate("StringBuilder", "CLASS", "java.lang.StringBuilder", "x3");

        CompletionResult merged = JavaCompletionProvider.mergeGeneralResults(
                CompletionResult.empty(), new CompletionResult(List.of(matching, unrelated)), "Linke");

        assertEquals(List.of("LinkedHashSet"), merged.candidates().stream()
                .map(CompletionCandidate::label).toList());
        assertEquals("x2", merged.candidates().get(0).resolveId());
    }

    @Test
    void generalMergeWithoutJdtCandidatesKeepsEveryLocalCandidate() {
        CompletionCandidate localType = candidate("LinkedList", "CLASS", "java.util.LinkedList");

        CompletionResult merged = JavaCompletionProvider.mergeGeneralResults(
                new CompletionResult(List.of(localType)), CompletionResult.empty(), "Linke");

        assertEquals(1, merged.candidates().size());
        assertSame(localType, merged.candidates().get(0));
    }

    @Test
    void generalMergeDeduplicatesJdtPackageSuffixAgainstLocalType() {
        CompletionCandidate localType = candidate("ArrayList", "CLASS", "java.util.ArrayList");
        CompletionCandidate jdtType = resolveCandidate("ArrayList - java.util", "CLASS", "java.util.ArrayList", "x9");

        CompletionResult merged = JavaCompletionProvider.mergeGeneralResults(
                new CompletionResult(List.of(localType)),
                new CompletionResult(List.of(jdtType)), "Arra", 45, 49);

        assertEquals(1, merged.candidates().size(), () -> "candidates=" + merged.candidates());
        assertEquals("ArrayList - java.util", merged.candidates().get(0).label());
        assertEquals("x9", merged.candidates().get(0).resolveId());
        assertEquals("ArrayList", merged.candidates().get(0).insertText());
    }

    @Test
    void generalMergeFiltersInternalJdkTypes() {
        CompletionCandidate internal = resolveCandidate("ArrayCacheConst - sun.java2d.marlin", "CLASS",
                "sun.java2d.marlin.ArrayCacheConst", "x1");
        CompletionCandidate publicType = resolveCandidate("ArrayList - java.util", "CLASS",
                "java.util.ArrayList", "x2");

        CompletionResult merged = JavaCompletionProvider.mergeGeneralResults(
                CompletionResult.empty(), new CompletionResult(List.of(internal, publicType)), "Arra");

        assertEquals(List.of("ArrayList - java.util"), merged.candidates().stream()
                .map(CompletionCandidate::label).toList());
    }

    private static CompletionCandidate resolveCandidate(String label, String kind, String detail, String resolveId) {
        String insert = label.contains(" - ") ? label.substring(0, label.indexOf(" - ")) : label;
        return new CompletionCandidate(label, kind, detail, "documentation", insert, label, false,
                0, 0, 0, detail, "", "String", "example", "java", java.util.List.of(), resolveId);
    }

    private static CompletionCandidate candidate(String label, String kind, String detail) {
        return new CompletionCandidate(label, kind, detail, "documentation", label + "()", label, false,
                0, 0, 0, detail, "", "String", "example", "java", java.util.List.of());
    }
}
