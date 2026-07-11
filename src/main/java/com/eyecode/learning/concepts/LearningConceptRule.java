package com.eyecode.learning.concepts;

import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.model.LearningContext;

import java.util.List;

public interface LearningConceptRule {

    boolean supports(LearningContext context);

    List<LearningConcept> evaluate(LearningContext context);
}
