package com.eyecode.learning.model;

import java.util.Optional;

public interface RelatedConceptResolver {

    Optional<LearningConcept> resolve(String id);

    static RelatedConceptResolver empty() {
        return id -> Optional.empty();
    }
}
