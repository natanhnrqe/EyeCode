package com.eyecode.learning.swing;

import com.eyecode.learning.model.RelatedConcept;

import java.util.List;

public interface LearningCardActions {

    void openDocumentation();

    void explainMore();

    void copyCode(String code);

    void showRelatedConcepts(List<RelatedConcept> concepts);

    static LearningCardActions noop() {
        return new LearningCardActions() {
            @Override public void openDocumentation() {}
            @Override public void explainMore() {}
            @Override public void copyCode(String code) {}
            @Override public void showRelatedConcepts(List<RelatedConcept> concepts) {}
        };
    }
}
