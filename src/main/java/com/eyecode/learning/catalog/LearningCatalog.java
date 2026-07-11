package com.eyecode.learning.catalog;

import com.eyecode.learning.model.ConceptType;
import com.eyecode.learning.model.LearningConcept;

public interface LearningCatalog {

    LearningConcept get(ConceptType type);

    boolean contains(ConceptType type);
}
