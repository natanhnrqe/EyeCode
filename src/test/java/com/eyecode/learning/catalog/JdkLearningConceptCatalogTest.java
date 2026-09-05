package com.eyecode.learning.catalog;

import com.eyecode.language.semantic.JavaMemberKind;
import com.eyecode.language.semantic.JavaMemberTarget;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdkLearningConceptCatalogTest {

    @Test
    void exposesEveryPracticalGoldenLesson() {
        JdkLearningConceptCatalog catalog = new JdkLearningConceptCatalog();

        for (String name : new String[]{"String", "Object", "Integer", "System", "Math",
                "List", "ArrayList", "LinkedList", "Map", "HashMap", "Number", "Byte",
                "Short", "Long", "Float", "Double", "Boolean", "Character", "CharSequence",
                "StringBuilder", "Comparable", "Iterable", "AutoCloseable", "Throwable",
                "Exception", "RuntimeException", "Error"}) {
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

    @Test
    void mapsSemanticMemberTargetsToExistingLessonDocuments() {
        var target = new JavaMemberTarget(
                "java.lang.String", "contains", JavaMemberKind.METHOD, 1);

        var concept = new JdkLearningConceptCatalog().find(target).orElseThrow();

        assertEquals("java/jdk/string/contains", concept.getPage().getId());
        assertEquals("java.lang.String", concept.getQualifiedName());
    }
}
