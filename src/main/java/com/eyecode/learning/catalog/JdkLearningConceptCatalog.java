package com.eyecode.learning.catalog;

import com.eyecode.language.documentation.JavaJdkType;
import com.eyecode.language.documentation.JavaJdkTypeCatalog;
import com.eyecode.learning.content.LearningPage;
import com.eyecode.learning.model.ConceptType;
import com.eyecode.learning.model.DifficultyLevel;
import com.eyecode.learning.model.LearningConcept;

import java.util.Map;
import java.util.Optional;

/** Maps supported JDK types to concise, bundled learning lessons. */
public final class JdkLearningConceptCatalog {

    private static final Map<String, String> IDS = Map.ofEntries(
            Map.entry("String", "java/jdk/string"),
            Map.entry("Object", "java/jdk/object"),
            Map.entry("Integer", "java/jdk/integer"),
            Map.entry("System", "java/jdk/system"),
            Map.entry("Math", "java/jdk/math"),
            Map.entry("List", "java/jdk/list"),
            Map.entry("ArrayList", "java/jdk/array-list"),
            Map.entry("LinkedList", "java/jdk/linked-list"),
            Map.entry("Map", "java/jdk/map"),
            Map.entry("HashMap", "java/jdk/hash-map"));

    public Optional<LearningConcept> find(String name) {
        return JavaJdkTypeCatalog.findSimple(name).flatMap(this::create);
    }

    private Optional<LearningConcept> create(JavaJdkType type) {
        String id = IDS.get(type.simpleName());
        if (id == null) {
            return Optional.empty();
        }
        LearningConcept concept = new LearningConcept();
        concept.setId(id);
        concept.setTitle(type.simpleName());
        concept.setDescription("A practical introduction to " + type.simpleName() + " in Java.");
        concept.setType(type.simpleName().equals("List") || type.simpleName().equals("Map")
                ? ConceptType.INTERFACE : ConceptType.CLASS);
        concept.setDifficulty(DifficultyLevel.BEGINNER);
        concept.setQualifiedName(type.qualifiedName());
        LearningPage page = new LearningPage("/learning/content/" + id + ".md");
        page.setId(id);
        concept.setPage(page);
        return Optional.of(concept);
    }
}
