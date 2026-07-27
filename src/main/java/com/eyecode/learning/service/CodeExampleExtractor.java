package com.eyecode.learning.service;

import com.eyecode.learning.model.LearningConcept;
import java.util.Optional;

public interface CodeExampleExtractor {

    Optional<String> extractFirstExample(LearningConcept concept);

    default boolean hasExample(LearningConcept concept) {
        return extractFirstExample(concept).isPresent();
    }
}
