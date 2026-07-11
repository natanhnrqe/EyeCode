package com.eyecode.learning.concepts;

import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.model.LearningContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DefaultLearningConceptEngine implements LearningConceptEngine {

    private final List<LearningConceptProvider> providers;

    public DefaultLearningConceptEngine(List<LearningConceptProvider> providers) {
        this.providers = providers == null
                ? Collections.emptyList()
                : List.copyOf(providers);
    }

    @Override
    public List<LearningConcept> analyze(LearningContext context) {
        if (context == null) {
            return Collections.emptyList();
        }

        List<LearningConcept> result = new ArrayList<>();
        for (LearningConceptProvider provider : providers) {
            List<LearningConcept> concepts = provider.analyze(context);
            if (concepts != null) {
                result.addAll(concepts);
            }
        }
        return result;
    }
}
