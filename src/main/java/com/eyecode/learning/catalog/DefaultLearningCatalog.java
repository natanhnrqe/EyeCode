package com.eyecode.learning.catalog;

import com.eyecode.learning.model.ConceptType;
import com.eyecode.learning.model.DifficultyLevel;
import com.eyecode.learning.model.LearningConcept;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class DefaultLearningCatalog implements LearningCatalog {

    private final Map<ConceptType, LearningConcept> concepts;

    public DefaultLearningCatalog() {
        this.concepts = new EnumMap<>(ConceptType.class);
        register(ConceptType.CLASS, "class", "Class",
                "A Java class defines a blueprint for creating objects.",
                DifficultyLevel.BEGINNER);
        register(ConceptType.INTERFACE, "interface", "Interface",
                "A Java interface defines a contract that implementing classes must fulfill.",
                DifficultyLevel.INTERMEDIATE);
        register(ConceptType.ENUM, "enum", "Enum",
                "A Java enum defines a fixed set of named constants.",
                DifficultyLevel.BEGINNER);
        register(ConceptType.RECORD, "record", "Record",
                "A Java record is a concise way to define immutable data carriers.",
                DifficultyLevel.INTERMEDIATE);
    }

    private void register(ConceptType type, String id, String title,
                          String description, DifficultyLevel difficulty) {
        LearningConcept concept = new LearningConcept();
        concept.setId(id);
        concept.setTitle(title);
        concept.setDescription(description);
        concept.setType(type);
        concept.setDifficulty(difficulty);
        concept.setRelatedConcepts(Collections.emptyList());
        concepts.put(type, concept);
    }

    @Override
    public LearningConcept get(ConceptType type) {
        return concepts.get(type);
    }

    @Override
    public boolean contains(ConceptType type) {
        return concepts.containsKey(type);
    }
}
