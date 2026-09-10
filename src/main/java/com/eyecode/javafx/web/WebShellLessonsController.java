package com.eyecode.javafx.web;

import com.eyecode.lessons.catalog.LearningCatalog;
import com.eyecode.lessons.catalog.LearningCategory;
import com.eyecode.lessons.catalog.LearningRoadmapItem;
import com.eyecode.lessons.catalog.LearningRoadmapSection;
import com.eyecode.lessons.catalog.LearningTopic;
import com.eyecode.lessons.catalog.LessonDescriptor;
import com.eyecode.lessons.catalog.ResourceLearningCatalog;
import com.eyecode.lessons.content.LessonContentService;
import com.eyecode.lessons.content.LessonContentBlock;
import com.eyecode.lessons.content.LessonEditorCommand;
import com.eyecode.lessons.content.LessonEditorRange;
import com.eyecode.lessons.session.LessonSessionService;
import com.eyecode.lessons.session.LessonSessionSnapshot;
import com.eyecode.lessons.practice.PracticeValidator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WebShellLessonsController {
    private final LearningCatalog catalog;
    private final String catalogError;
    private final LessonContentService contentService;
    private final LessonSessionService sessionService;
    private final PracticeValidator practiceValidator;

    public WebShellLessonsController(WebShellSurface surface) {
        LearningCatalog loadedCatalog = null;
        String loadError = null;
        try {
            loadedCatalog = new ResourceLearningCatalog();
        } catch (RuntimeException exception) {
            loadError = exception.getMessage();
        }
        catalog = loadedCatalog;
        catalogError = loadError;
        contentService = new LessonContentService();
        sessionService = new LessonSessionService(contentService);
        practiceValidator = new PracticeValidator();
        surface.registerHandler("lessons", "catalog", this::catalog);
        surface.registerHandler("lessons", "session/start", this::start);
        surface.registerHandler("lessons", "session/next", this::next);
        surface.registerHandler("lessons", "session/previous", this::previous);
        surface.registerHandler("lessons", "session/close", this::close);
        surface.registerHandler("lessons", "session/verify", this::verify);
    }

    public void closeActiveSession() {
        sessionService.closeActive();
    }

    private WebShellEnvelope catalog(WebShellEnvelope request) {
        if (catalog == null) {
            return request.error(new WebShellError("LESSONS_CATALOG_UNAVAILABLE",
                    catalogError == null ? "O catálogo de aulas está indisponível" : catalogError, true));
        }
        return request.response(catalogPayload(catalog, contentService));
    }

    static Map<String, Object> catalogPayload(LearningCatalog catalog) {
        return catalogPayload(catalog, new LessonContentService());
    }

    static Map<String, Object> catalogPayload(LearningCatalog catalog, LessonContentService contentService) {
        List<Map<String, Object>> categories = catalog.categories().stream()
                .map(category -> categoryPayload(catalog, contentService, category))
                .toList();
        return Map.of("categories", categories);
    }

    private static Map<String, Object> categoryPayload(LearningCatalog catalog, LessonContentService contentService, LearningCategory category) {
        Map<String, Object> payload = basePayload(category.id(), category.title(), category.description());
        payload.put("topics", catalog.topicsForCategory(category.id()).stream()
                .map(topic -> topicPayload(catalog, contentService, topic))
                .toList());
        return payload;
    }

    private static Map<String, Object> topicPayload(LearningCatalog catalog, LessonContentService contentService, LearningTopic topic) {
        Map<String, Object> payload = basePayload(topic.id(), topic.title(), topic.description());
        payload.put("categoryId", topic.categoryId());
        payload.put("roadmapSections", topic.roadmapSections().stream()
                .map(WebShellLessonsController::roadmapSectionPayload)
                .toList());
        payload.put("lessons", catalog.lessonsForTopic(topic.id()).stream()
                .map(lesson -> lessonPayload(contentService, lesson))
                .toList());
        return payload;
    }

    private static Map<String, Object> roadmapSectionPayload(LearningRoadmapSection section) {
        Map<String, Object> payload = basePayload(section.id(), section.title(), "");
        payload.put("items", section.items().stream().map(WebShellLessonsController::roadmapItemPayload).toList());
        return payload;
    }

    private static Map<String, Object> roadmapItemPayload(LearningRoadmapItem item) {
        return basePayload(item.id(), item.title(), item.description());
    }

    private static Map<String, Object> lessonPayload(LessonContentService contentService, LessonDescriptor lesson) {
        Map<String, Object> payload = basePayload(lesson.id(), lesson.title(), lesson.description());
        payload.put("categoryId", lesson.categoryId());
        payload.put("topicId", lesson.topicId());
        payload.put("difficulty", lesson.difficulty().name());
        payload.put("kind", lesson.kind().name());
        payload.put("estimatedMinutes", lesson.estimatedMinutes());
        payload.put("concepts", lesson.concepts());
        payload.put("executable", contentService.hasContent(lesson.id()));
        return payload;
    }

    private WebShellEnvelope start(WebShellEnvelope request) {
        return request.response(sessionPayload(sessionService.start(text(request, "lessonId"))));
    }

    private WebShellEnvelope next(WebShellEnvelope request) {
        return request.response(sessionPayload(sessionService.next(text(request, "sessionId"))));
    }

    private WebShellEnvelope previous(WebShellEnvelope request) {
        return request.response(sessionPayload(sessionService.previous(text(request, "sessionId"))));
    }

    private WebShellEnvelope close(WebShellEnvelope request) {
        return request.response(sessionPayload(sessionService.close(text(request, "sessionId"))));
    }

    private WebShellEnvelope verify(WebShellEnvelope request) {
        try {
            return request.response(verifyPayload(sessionService, practiceValidator, text(request, "sessionId"),
                    text(request, "practiceId"), source(request)));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return request.error(new WebShellError("LESSON_PRACTICE_INVALID", exception.getMessage(), true));
        }
    }

    static Map<String, Object> verifyPayload(LessonSessionService sessionService, PracticeValidator validator,
                                             String sessionId, String practiceId, String source) {
        var verification = sessionService.verifyPractice(sessionId, practiceId, source, validator);
        return Map.of("verification", Map.of("status", verification.verification().status().name(),
                        "message", verification.verification().message()),
                "session", sessionPayload(verification.session()));
    }

    static Map<String, Object> sessionPayload(LessonSessionSnapshot snapshot) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", snapshot.sessionId());
        payload.put("lessonId", snapshot.lessonId());
        payload.put("kind", snapshot.kind().name());
        payload.put("currentStep", snapshot.currentStepIndex());
        payload.put("totalSteps", snapshot.totalSteps());
        payload.put("currentPresentation", snapshot.currentPresentationIndex());
        payload.put("presentationId", snapshot.presentation().id());
        payload.put("state", snapshot.state().name());
        payload.put("phase", snapshot.phase().name());
        payload.put("practiceCompleted", snapshot.practiceCompleted());
        if (snapshot.practice() != null) payload.put("practice", Map.of("id", snapshot.practice().id(),
                "instruction", snapshot.practice().instruction(), "starterCode", snapshot.practice().starterCode()));
        payload.put("title", snapshot.step().title());
        payload.put("message", snapshot.step().message());
        payload.put("canPrevious", snapshot.canPrevious());
        payload.put("canNext", snapshot.canNext());
        payload.put("contentBlocks", snapshot.step().contentBlocks().stream().map(WebShellLessonsController::blockPayload).toList());
        if (snapshot.presentation().annotation() != null) {
            payload.put("annotation", Map.of("title", snapshot.presentation().annotation().title(),
                    "message", snapshot.presentation().annotation().message(), "range", rangePayload(snapshot.presentation().annotation().range())));
        }
        payload.put("commands", snapshot.presentation().commands().stream().map(WebShellLessonsController::commandPayload).toList());
        return payload;
    }

    private static Map<String, Object> commandPayload(LessonEditorCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", command.type().name());
        if (command.code() != null) payload.put("code", command.code());
        if (command.replacementText() != null) payload.put("replacementText", command.replacementText());
        if (command.range() != null) payload.put("range", rangePayload(command.range()));
        if (command.finalCode() != null) payload.put("finalCode", command.finalCode());
        if (command.cadenceMillis() != null) payload.put("cadenceMillis", command.cadenceMillis());
        return payload;
    }

    private static Map<String, Object> blockPayload(LessonContentBlock block) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", block.type().name());
        if (block.text() != null) payload.put("text", block.text());
        if (block.title() != null) payload.put("title", block.title());
        if (block.language() != null) payload.put("language", block.language());
        if (block.code() != null) payload.put("code", block.code());
        if (!block.items().isEmpty()) payload.put("items", block.items());
        if (!block.inlineContent().isEmpty()) payload.put("inlineContent", block.inlineContent().stream()
                .map(inline -> inline.url() == null ? Map.of("type", inline.type().name(), "text", inline.text())
                        : Map.of("type", inline.type().name(), "text", inline.text(), "url", inline.url()))
                .toList());
        if (block.ordered()) payload.put("ordered", true);
        return payload;
    }

    private static Map<String, Object> rangePayload(LessonEditorRange range) {
        return Map.of("startLineNumber", range.startLineNumber(), "startColumn", range.startColumn(),
                "endLineNumber", range.endLineNumber(), "endColumn", range.endColumn());
    }

    private static String text(WebShellEnvelope request, String name) {
        Object value = request.payload().get(name);
        if (value instanceof String text && !text.isBlank()) return text;
        throw new IllegalArgumentException("Campo obrigatório ausente: " + name);
    }

    private static String source(WebShellEnvelope request) {
        Object value = request.payload().get("source");
        if (value instanceof String source) return source;
        throw new IllegalArgumentException("Campo obrigatório ausente: source");
    }

    private static Map<String, Object> basePayload(String id, String title, String description) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", id);
        payload.put("title", title);
        payload.put("description", description);
        return payload;
    }
}
