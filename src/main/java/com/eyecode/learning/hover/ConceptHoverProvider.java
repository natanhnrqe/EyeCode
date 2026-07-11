package com.eyecode.learning.hover;

import com.eyecode.learning.analysis.LearningAnalysisContext;
import com.eyecode.learning.concepts.LearningConceptEngine;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.model.LearningContext;

import java.util.List;
import java.util.Optional;

public final class ConceptHoverProvider implements HoverProvider {

    private final LearningConceptEngine conceptEngine;

    public ConceptHoverProvider(LearningConceptEngine conceptEngine) {
        this.conceptEngine = conceptEngine;
    }

    @Override
    public Optional<LearningConcept> getHover(LearningAnalysisContext context) {
        if (context == null) {
            return Optional.empty();
        }

        LearningContext learningContext = context.getLearningContext();
        if (learningContext == null) {
            return Optional.empty();
        }

        List<LearningConcept> concepts = conceptEngine.analyze(learningContext);
        if (concepts == null || concepts.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(concepts.getFirst());
    }
}
