package com.eyecode.learning.hover;

import com.eyecode.learning.analysis.LearningAnalysisContext;
import com.eyecode.learning.model.LearningConcept;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class DefaultHoverEngine implements HoverEngine {

    private final List<HoverProvider> providers;

    public DefaultHoverEngine(List<HoverProvider> providers) {
        this.providers = providers == null
                ? Collections.emptyList()
                : List.copyOf(providers);
    }

    @Override
    public Optional<LearningConcept> resolve(LearningAnalysisContext context) {
        if (context == null) {
            return Optional.empty();
        }

        for (HoverProvider provider : providers) {
            Optional<LearningConcept> result = provider.getHover(context);
            if (result != null && result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }
}
