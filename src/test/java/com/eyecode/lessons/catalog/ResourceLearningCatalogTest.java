package com.eyecode.lessons.catalog;

import com.eyecode.lessons.content.LessonKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceLearningCatalogTest {
    @Test void loadsTheDeclaredCatalog() {
        LearningCatalog catalog = new ResourceLearningCatalog();
        assertEquals(java.util.List.of("java", "algorithms", "data-structures", "spring"),
                catalog.categories().stream().map(LearningCategory::id).toList());
        assertEquals("Strings", catalog.topic("java.strings").orElseThrow().title());
    }

    @Test void exposesRequiredLessonMetadata() {
        LessonDescriptor lesson = new ResourceLearningCatalog().lesson("java.deep-dive.process-builder").orElseThrow();
        assertEquals(LessonDifficulty.ADVANCED, lesson.difficulty());
        assertEquals(30, lesson.estimatedMinutes());
        assertTrue(lesson.concepts().contains("ProcessBuilder"));
    }

    @Test void exposesExplicitKindsForTheExecutableFundamentalsLessons() {
        ResourceLearningCatalog catalog = new ResourceLearningCatalog();
        assertEquals(LessonKind.THEORY, catalog.lesson("java.fundamentals.jvm-jre-jdk").orElseThrow().kind());
        assertEquals(LessonKind.PRACTICE, catalog.lesson("java.fundamentals.variables.int").orElseThrow().kind());
    }

    @Test void rejectsDuplicateCategoryIds() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceLearningCatalog(catalog("[" + category("java") + "," + category("java") + "]", "[]", "[]")));
    }

    @Test void rejectsDuplicateTopicIdsAndUnknownLessonReferences() {
        String topics = "[{\"id\":\"topic\",\"categoryId\":\"java\",\"title\":\"Topic\",\"description\":\"\",\"roadmapSections\":[]},"
                + "{\"id\":\"topic\",\"categoryId\":\"java\",\"title\":\"Topic\",\"description\":\"\",\"roadmapSections\":[]}]";
        assertThrows(IllegalArgumentException.class, () -> new ResourceLearningCatalog(catalog("[" + category("java") + "]", topics, "[]")));
        assertThrows(IllegalArgumentException.class, () -> new ResourceLearningCatalog(catalog("[]", "[]", "[" + lesson("missing", "missing") + "]")));
    }

    @Test void rejectsInvalidDifficultyAndDuration() {
        String category = "[" + category("java") + "]";
        String topic = "[{\"id\":\"topic\",\"categoryId\":\"java\",\"title\":\"Topic\",\"description\":\"\",\"roadmapSections\":[]}]";
        assertThrows(IllegalArgumentException.class, () -> new ResourceLearningCatalog(catalog(category, topic,
                "[" + lesson("java", "topic").replace("BEGINNER", "EXPERT") + "]")));
        assertThrows(IllegalArgumentException.class, () -> new ResourceLearningCatalog(catalog(category, topic,
                "[" + lesson("java", "topic").replace("\"estimatedMinutes\":1", "\"estimatedMinutes\":-1") + "]")));
    }

    private static String catalog(String categories, String topics, String lessons) {
        return "{\"categories\":" + categories + ",\"topics\":" + topics + ",\"lessons\":" + lessons + "}";
    }
    private static String category(String id) { return "{\"id\":\"" + id + "\",\"title\":\"Java\",\"description\":\"\"}"; }
    private static String lesson(String categoryId, String topicId) {
        return "{\"id\":\"lesson\",\"title\":\"Lesson\",\"description\":\"Description\",\"categoryId\":\"" + categoryId + "\",\"topicId\":\"" + topicId + "\",\"difficulty\":\"BEGINNER\",\"estimatedMinutes\":1,\"concepts\":[\"concept\"]}";
    }
}
