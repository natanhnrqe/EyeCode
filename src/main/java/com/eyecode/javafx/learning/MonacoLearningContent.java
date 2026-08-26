package com.eyecode.javafx.learning;

import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.content.LearningMetadata;
import com.eyecode.language.documentation.JdkSourceTarget;

public record MonacoLearningContent(LearningConcept concept, LearningMetadata metadata,
                                    JdkSourceTarget sourceTarget, String payload) {
    public MonacoLearningContent {
        if (concept == null) {
            throw new IllegalArgumentException("concept must not be null");
        }
        payload = payload == null ? "" : payload;
    }

    public MonacoLearningContent(LearningConcept concept, String payload) {
        this(concept, null, null, payload);
    }
}
