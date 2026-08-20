package com.eyecode.learning.catalog;

import com.eyecode.learning.content.LearningPage;
import com.eyecode.learning.model.ConceptType;
import com.eyecode.learning.model.DifficultyLevel;
import com.eyecode.learning.model.LearningConcept;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class DefaultLearningCatalog implements LearningCatalog {

    private final Map<ConceptType, LearningConcept> concepts;

    public DefaultLearningCatalog() {
        this.concepts = new EnumMap<>(ConceptType.class);
        register(ConceptType.CLASS, "class", "Class",
                "A Java class defines a blueprint for creating objects.",
                DifficultyLevel.BEGINNER,
                "/learning/content/java/types/class.md",
                List.of("object", "interface", "record"));
        register(ConceptType.INTERFACE, "interface", "Interface",
                "A Java interface defines a contract that implementing classes must fulfill.",
                DifficultyLevel.INTERMEDIATE,
                "/learning/content/java/types/interface.md",
                List.of("class", "object"));
        register(ConceptType.ENUM, "enum", "Enum",
                "A Java enum defines a fixed set of named constants.",
                DifficultyLevel.BEGINNER,
                "/learning/content/java/types/enum.md",
                List.of("class"));
        register(ConceptType.RECORD, "record", "Record",
                "A Java record is a concise way to define immutable data carriers.",
                DifficultyLevel.INTERMEDIATE,
                "/learning/content/java/types/record.md",
                List.of("class", "object"));
        register(ConceptType.OBJECT, "object", "Object",
                "An object is an instance of a class, with state and behavior.",
                DifficultyLevel.BEGINNER,
                "/learning/content/java/types/object.md",
                List.of("class"));
    }

    private void register(ConceptType type, String id, String title,
                          String description, DifficultyLevel difficulty,
                          String resourcePath, List<String> relatedConceptIds) {
        LearningConcept concept = new LearningConcept();
        concept.setId(id);
        concept.setTitle(title);
        concept.setDescription(description);
        concept.setType(type);
        concept.setDifficulty(difficulty);
        concept.setRelatedConcepts(List.copyOf(relatedConceptIds));
        LearningPage page = new LearningPage(resourcePath);
        page.setId("java/types/" + id);
        concept.setPage(page);
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

    @Override
    public List<LearningConcept> allConcepts() {
        return List.copyOf(concepts.values());
    }
}
