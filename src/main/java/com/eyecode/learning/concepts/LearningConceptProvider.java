package com.eyecode.learning.concepts;

import com.eyecode.learning.analysis.LearningAnalysisContext;
import com.eyecode.learning.model.LearningConcept;

import java.util.List;

public interface LearningConceptProvider {

    List<LearningConcept> analyze(LearningAnalysisContext context);
}
