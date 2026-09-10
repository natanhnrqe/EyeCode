package com.eyecode.javafx.web;

import com.eyecode.lessons.catalog.ResourceLearningCatalog;
import com.eyecode.lessons.content.LessonContentService;
import com.eyecode.lessons.session.LessonSessionService;
import com.eyecode.lessons.practice.PracticeValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebShellLessonsControllerTest {
    @Test void serializesCategoriesTopicsAndLessonDescriptors() {
        Map<String, Object> payload = WebShellLessonsController.catalogPayload(new ResourceLearningCatalog());
        List<Map<String, Object>> categories = maps(payload.get("categories"));
        assertEquals(4, categories.size());
        Map<String, Object> java = categories.getFirst();
        assertEquals("java", java.get("id"));
        Map<String, Object> strings = maps(java.get("topics")).stream()
                .filter(topic -> "java.strings".equals(topic.get("id"))).findFirst().orElseThrow();
        Map<String, Object> lesson = maps(strings.get("lessons")).stream()
                .filter(item -> "java.strings.manipulation".equals(item.get("id"))).findFirst().orElseThrow();
        assertEquals("BEGINNER", lesson.get("difficulty"));
        assertTrue(((List<?>) lesson.get("concepts")).contains("substring"));
    }

    @Test void marksPrimitiveTypesAsExecutableWhenItsContentResourceExists() {
        Map<String, Object> payload = WebShellLessonsController.catalogPayload(new ResourceLearningCatalog());
        Map<String, Object> java = maps(payload.get("categories")).getFirst();
        Map<String, Object> fundamentals = maps(java.get("topics")).stream()
                .filter(topic -> "java.fundamentals".equals(topic.get("id"))).findFirst().orElseThrow();
        Map<String, Object> primitiveTypes = maps(fundamentals.get("lessons")).stream()
                .filter(lesson -> "java.fundamentals.variables.int".equals(lesson.get("id"))).findFirst().orElseThrow();

        assertEquals("Tipos Primitivos", primitiveTypes.get("title"));
        assertEquals(true, primitiveTypes.get("executable"));
        assertEquals("PRACTICE", primitiveTypes.get("kind"));
        Map<String, Object> jvm = maps(fundamentals.get("lessons")).stream()
                .filter(lesson -> "java.fundamentals.jvm-jre-jdk".equals(lesson.get("id"))).findFirst().orElseThrow();
        assertEquals("THEORY", jvm.get("kind"));
        assertEquals(true, jvm.get("executable"));
    }

    @Test void preservesSessionStepAndCommandsInTheBridgePayload() {
        var sessions = new LessonSessionService(new LessonContentService());
        var started = sessions.start("java.fundamentals.variables.int");
        var snapshot = sessions.next(started.sessionId());
        Map<String, Object> payload = WebShellLessonsController.sessionPayload(snapshot);
        assertEquals(1, payload.get("currentStep"));
        assertEquals(0, payload.get("currentPresentation"));
        assertEquals("int", payload.get("presentationId"));
        assertEquals("PRACTICE", payload.get("kind"));
        assertEquals(6, payload.get("totalSteps"));
        assertEquals("HEADING", ((Map<?, ?>) ((List<?>) payload.get("contentBlocks")).getFirst()).get("type"));
        Map<?, ?> annotation = (Map<?, ?>) payload.get("annotation");
        assertEquals("int", annotation.get("title"));
        assertEquals(4, ((Map<?, ?>) annotation.get("range")).get("startLineNumber"));
        Map<?, ?> animate = (Map<?, ?>) ((List<?>) payload.get("commands")).get(2);
        assertEquals("ANIMATE_EDIT", animate.get("type"));
        assertTrue(((String) animate.get("replacementText")).contains("int age = 20;"));
        assertTrue(((String) animate.get("finalCode")).contains("int age = 20;"));
        assertEquals(18, animate.get("cadenceMillis"));
    }

    @Test void serializesTheorySessionPayloadWithoutCommandsOrPractice() {
        var sessions = new LessonSessionService(new LessonContentService());
        var snapshot = sessions.start("java.fundamentals.jvm-jre-jdk");
        Map<String, Object> payload = WebShellLessonsController.sessionPayload(snapshot);
        assertEquals("THEORY", payload.get("kind"));
        assertEquals(List.of(), payload.get("commands"));
        assertFalse(payload.containsKey("practice"));
        assertFalse(payload.toString().contains("<script"));
        assertTrue(maps(payload.get("contentBlocks")).stream().anyMatch(block -> block.containsKey("inlineContent")));
    }

    @Test void serializesTheActivePracticeContract() {
        var sessions = new LessonSessionService(new LessonContentService());
        var started = sessions.start("java.fundamentals.variables.int");
        sessions.next(started.sessionId());
        sessions.next(started.sessionId());
        var practice = sessions.next(started.sessionId());

        Map<String, Object> payload = WebShellLessonsController.sessionPayload(practice);
        assertEquals("PRACTICE", payload.get("phase"));
        assertEquals(false, payload.get("practiceCompleted"));
        @SuppressWarnings("unchecked")
        Map<String, Object> practicePayload = (Map<String, Object>) payload.get("practice");
        assertEquals("integer-score", practicePayload.get("id"));
        assertEquals("Crie uma variável `int` chamada `score` com valor `100` dentro do método `main`.",
                practicePayload.get("instruction"));
        assertEquals("public class Main {\n\n    public static void main(String[] args) {\n\n    }\n}\n",
                practicePayload.get("starterCode"));
    }

    @Test void verifiesSubmittedPracticeSourceAndReturnsAuthoritativeSession() {
        PracticeSession practice = practiceSession();
        Map<String, Object> failed = WebShellLessonsController.verifyPayload(practice.service(), new PracticeValidator(),
                practice.sessionId(), "integer-score", source("int score = 99;"));
        Map<?, ?> failure = (Map<?, ?>) failed.get("verification");
        Map<?, ?> failedSession = (Map<?, ?>) failed.get("session");
        assertEquals("WRONG_INITIALIZER", failure.get("status"));
        assertEquals("Inicialize `score` com o valor inteiro `100`.", failure.get("message"));
        assertEquals(false, failedSession.get("practiceCompleted"));

        Map<String, Object> succeeded = WebShellLessonsController.verifyPayload(practice.service(), new PracticeValidator(),
                practice.sessionId(), "integer-score", source("int score = 100;"));
        Map<?, ?> success = (Map<?, ?>) succeeded.get("verification");
        Map<?, ?> successfulSession = (Map<?, ?>) succeeded.get("session");
        assertEquals("SUCCESS", success.get("status"));
        assertEquals(true, successfulSession.get("practiceCompleted"));
        assertEquals("PRACTICE", successfulSession.get("phase"));
        assertEquals(true, successfulSession.get("canNext"));
    }

    @Test void rejectsWrongPracticeAndVerificationOutsidePractice() {
        PracticeSession practice = practiceSession();
        assertThrows(IllegalArgumentException.class, () -> WebShellLessonsController.verifyPayload(practice.service(),
                new PracticeValidator(), practice.sessionId(), "other-practice", source("int score = 100;")));

        LessonSessionService beforePractice = new LessonSessionService(new LessonContentService());
        String sessionId = beforePractice.start("java.fundamentals.variables.int").sessionId();
        assertThrows(IllegalStateException.class, () -> WebShellLessonsController.verifyPayload(beforePractice,
                new PracticeValidator(), sessionId, "integer-score", source("int score = 100;")));
    }

    private static PracticeSession practiceSession() {
        LessonSessionService sessions = new LessonSessionService(new LessonContentService());
        var first = sessions.start("java.fundamentals.variables.int");
        sessions.next(first.sessionId());
        sessions.next(first.sessionId());
        sessions.next(first.sessionId());
        return new PracticeSession(sessions, first.sessionId());
    }

    private static String source(String declaration) {
        return "public class Main {\n    public static void main(String[] args) {\n        "
                + declaration + "\n    }\n}\n";
    }

    private record PracticeSession(LessonSessionService service, String sessionId) {
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(Object value) { return (List<Map<String, Object>>) value; }
}
