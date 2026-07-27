package com.eyecode.learning.service;

import com.eyecode.learning.model.LearningConcept;

@FunctionalInterface
public interface DocumentationOpener {
    void open(LearningConcept concept);
}
