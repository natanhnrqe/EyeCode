package com.eyecode.learning.swing;

import com.eyecode.learning.model.RelatedConcept;

import java.util.ArrayList;
import java.util.List;

public final class RecordingLearningCardActions implements LearningCardActions {

    private int openDocCalls = 0;
    private int explainCalls = 0;
    private final List<String> copiedCodes = new ArrayList<>();
    private final List<List<RelatedConcept>> relatedShown = new ArrayList<>();

    @Override
    public void openDocumentation() {
        openDocCalls++;
    }

    @Override
    public void explainMore() {
        explainCalls++;
    }

    @Override
    public void copyCode(String code) {
        copiedCodes.add(code);
    }

    @Override
    public void showRelatedConcepts(List<RelatedConcept> concepts) {
        relatedShown.add(concepts != null ? List.copyOf(concepts) : List.of());
    }

    public int openDocCalls() {
        return openDocCalls;
    }

    public int explainCalls() {
        return explainCalls;
    }

    public List<String> copiedCodes() {
        return List.copyOf(copiedCodes);
    }

    public List<List<RelatedConcept>> relatedShown() {
        return List.copyOf(relatedShown);
    }

    public void reset() {
        openDocCalls = 0;
        explainCalls = 0;
        copiedCodes.clear();
        relatedShown.clear();
    }

    public String lastCopiedCode() {
        return copiedCodes.isEmpty() ? null : copiedCodes.get(copiedCodes.size() - 1);
    }

    public List<RelatedConcept> lastRelatedShown() {
        return relatedShown.isEmpty() ? List.of() : relatedShown.get(relatedShown.size() - 1);
    }
}
