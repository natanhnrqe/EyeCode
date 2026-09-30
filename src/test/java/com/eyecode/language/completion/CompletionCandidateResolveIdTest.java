package com.eyecode.language.completion;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompletionCandidateResolveIdTest {
    @Test
    void legacyConstructorDefaultsResolveIdToEmpty() {
        var candidate = new CompletionCandidate("label", "KEYWORD", "", "", "label", "label",
                false, 0, 0, 0, "", "", "", "", "", List.of());
        assertEquals("", candidate.resolveId());
    }

    @Test
    void canonicalConstructorPreservesResolveId() {
        var candidate = new CompletionCandidate("label", "KEYWORD", "", "", "label", "label",
                false, 0, 0, 0, "", "", "", "", "", List.of(), "a1");
        assertEquals("a1", candidate.resolveId());
    }

    @Test
    void nullResolveIdDefaultsToEmpty() {
        var candidate = new CompletionCandidate("label", "KEYWORD", "", "", "label", "label",
                false, 0, 0, 0, "", "", "", "", "", null, null);
        assertEquals("", candidate.resolveId());
        assertEquals(List.of(), candidate.matchIndices());
    }

    @Test
    void resolveIdParticipatesInEquality() {
        var first = new CompletionCandidate("label", "KEYWORD", "", "", "label", "label",
                false, 0, 0, 0, "", "", "", "", "", List.of(), "a1");
        var second = new CompletionCandidate("label", "KEYWORD", "", "", "label", "label",
                false, 0, 0, 0, "", "", "", "", "", List.of(), "a2");
        assertEquals(false, first.equals(second));
    }
}
