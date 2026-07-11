package com.eyecode.learning.concepts;

import com.eyecode.learning.analysis.DefaultLearningContextResolver;
import com.eyecode.learning.analysis.LearningAnalysisContext;
import com.eyecode.learning.analysis.LearningContextResolver;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.model.LearningContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DefaultLearningConceptEngine implements LearningConceptEngine {

    private final List<LearningConceptProvider> providers;
    private final LearningContextResolver resolver;

    public DefaultLearningConceptEngine(List<LearningConceptProvider> providers) {
        this(providers, new DefaultLearningContextResolver());
    }

    public DefaultLearningConceptEngine(List<LearningConceptProvider> providers, LearningContextResolver resolver) {
        this.providers = providers == null
                ? Collections.emptyList()
                : List.copyOf(providers);
        this.resolver = resolver == null
                ? new DefaultLearningContextResolver()
                : resolver;
    }

    @Override
    public List<LearningConcept> analyze(LearningContext context) {
        if (context == null) {
            return Collections.emptyList();
        }

        LearningAnalysisContext analysisContext = resolver.resolve(context);
        if (analysisContext == null) {
            return Collections.emptyList();
        }

        List<LearningConcept> result = new ArrayList<>();
        for (LearningConceptProvider provider : providers) {
            List<LearningConcept> concepts = provider.analyze(analysisContext);
            if (concepts != null) {
                result.addAll(concepts);
            }
        }
        return result;
    }
}
