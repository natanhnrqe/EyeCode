package com.eyecode.learning.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdkLearningConceptCatalogTest {

    @Test
    void exposesEveryPracticalGoldenLesson() {
        JdkLearningConceptCatalog catalog = new JdkLearningConceptCatalog();

        for (String name : new String[]{"String", "Object", "Integer", "System", "Math",
                "List", "ArrayList", "Map", "HashMap"}) {
            var concept = catalog.find(name).orElseThrow();
            assertEquals(name, concept.getTitle());
            assertTrue(concept.getPage().getId().startsWith("java/jdk/"));
            assertTrue(concept.getQualifiedName().startsWith("java."));
        }
    }

    @Test
    void doesNotCreateCardsForUnsupportedJdkTypes() {
        assertTrue(new JdkLearningConceptCatalog().find("Thread").isEmpty());
    }
}
