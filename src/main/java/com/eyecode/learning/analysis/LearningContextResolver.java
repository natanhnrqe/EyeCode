package com.eyecode.learning.analysis;

import com.eyecode.learning.model.LearningContext;

public interface LearningContextResolver {

    LearningAnalysisContext resolve(LearningContext context);
}
