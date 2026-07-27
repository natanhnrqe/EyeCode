package com.eyecode.learning.model;

import java.util.Objects;
import java.util.function.Consumer;

public final class RelatedConceptNavigator {

    private final RelatedConceptResolver resolver;
    private final Consumer<LearningConcept> onNavigate;

    public RelatedConceptNavigator(RelatedConceptResolver resolver,
                                   Consumer<LearningConcept> onNavigate) {
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
        this.onNavigate = Objects.requireNonNull(onNavigate, "onNavigate must not be null");
    }

    public boolean navigateTo(RelatedConcept concept) {
        if (concept == null || concept.id() == null || concept.id().isBlank()) {
            return false;
        }
        return resolver.resolve(concept.id())
                .map(resolved -> {
                    onNavigate.accept(resolved);
                    return true;
                })
                .orElse(false);
    }
}
