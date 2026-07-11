package com.eyecode.learning.concepts;

import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.model.LearningContext;

import java.util.List;

public interface LearningConceptEngine {

    List<LearningConcept> analyze(LearningContext context);
}
