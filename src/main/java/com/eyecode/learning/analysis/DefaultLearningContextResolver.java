package com.eyecode.learning.analysis;

import com.eyecode.learning.model.LearningContext;

public final class DefaultLearningContextResolver implements LearningContextResolver {

    @Override
    public LearningAnalysisContext resolve(LearningContext context) {
        if (context == null) {
            return null;
        }

        return new LearningAnalysisContext(
                context,
                context.getCurrentSymbol(),
                context.getCurrentScope(),
                context.getCurrentNode(),
                context.getCurrentClass(),
                context.getCurrentMethod()
        );
    }
}
