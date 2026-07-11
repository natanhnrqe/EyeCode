package com.eyecode.learning.hover;

import com.eyecode.learning.analysis.LearningAnalysisContext;
import com.eyecode.learning.model.LearningConcept;

import java.util.Optional;

public interface HoverEngine {

    Optional<LearningConcept> resolve(LearningAnalysisContext context);
}
