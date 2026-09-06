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
                "StringBuilder", "Comparable", "Iterable", "AutoCloseable", "Throwable", "Enum", "Optional", "Stream",
                "Exception", "RuntimeException", "Error", "Arrays", "Collection", "Iterator", "Collections", "Set", "HashSet", "LinkedHashSet", "TreeSet", "LinkedHashMap", "TreeMap", "Queue", "Deque", "ArrayDeque", "PriorityQueue", "Comparator"}) {
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

    @Test
    void mapsCommonMethodsToIndividualLessons() {
        JdkLearningConceptCatalog catalog = new JdkLearningConceptCatalog();

        assertMember(catalog, "java.lang.String", "isBlank", "java/jdk/string/is-blank");
        assertMember(catalog, "java.lang.String", "substring", "java/jdk/string/substring");
        assertMember(catalog, "java.lang.String", "charAt", "java/jdk/string/char-at");
        assertMember(catalog, "java.util.Map", "computeIfAbsent", "java/jdk/map/compute-if-absent");
        assertMember(catalog, "java.util.Map", "entrySet", "java/jdk/map/entry-set");
        assertMember(catalog, "java.util.Arrays", "asList", "java/jdk/arrays/as-list");
        assertMember(catalog, "java.util.Collections", "sort", "java/jdk/collections/sort");
        assertMember(catalog, "java.util.Optional", "orElseGet", "java/jdk/optional/or-else-get");
        assertMember(catalog, "java.util.Optional", "map", "java/jdk/optional/map");
        assertMember(catalog, "java.util.stream.Stream", "filter", "java/jdk/stream/filter");
        assertMember(catalog, "java.util.stream.Stream", "toList", "java/jdk/stream/to-list");
        assertMember(catalog, "java.util.Comparator", "comparing", "java/jdk/comparator/comparing");
    }

    private static void assertMember(JdkLearningConceptCatalog catalog, String owner,
                                     String member, String expectedId) {
        var concept = catalog.find(new JavaMemberTarget(owner, member, JavaMemberKind.METHOD, 1)).orElseThrow();
        assertEquals(expectedId, concept.getPage().getId());
        assertEquals(owner, concept.getQualifiedName());
    }
}
