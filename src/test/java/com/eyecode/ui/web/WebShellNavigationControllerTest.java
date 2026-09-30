package com.eyecode.ui.web;

import com.eyecode.editor.intelligence.document.LineMap;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.eventbus.EventBus;
import com.eyecode.filesystem.DefaultFileSystemService;
import com.eyecode.language.java.lsp.JdtLsProjectService;
import com.eyecode.language.navigation.NavigationService;
import com.eyecode.language.navigation.NavigationTarget;
import com.eyecode.language.semantic.DefinitionLocation;
import com.eyecode.project.ProjectFileOperationService;
import com.eyecode.project.ProjectLifecycleService;
import com.eyecode.workbench.editor.EditorIntelligence;
import com.eyecode.workbench.editor.EditorManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebShellNavigationControllerTest {
    private static final String SOURCE =
            "class Main {\n    void run() {\n        int counter = 1;\n        int other = counter + 2;\n    }\n}";

    @TempDir
    Path temporary;

    @Test
    void definitionFallsBackToTheLocalResolverWhenJdtIsAbsent() throws Exception {
        EditorManager manager = manager();
        Path file = Files.writeString(temporary.resolve("Main.java"), SOURCE);
        manager.openDocument(file);
        Surface surface = new Surface();
        WebShellNavigationController controller = new WebShellNavigationController(surface, manager, null);
        try {
            LineMap lines = LineMap.of(SOURCE);
            int referenceOffset = SOURCE.indexOf("counter +");
            WebShellEnvelope response = surface.handler("navigation", "definition").handle(request("definition",
                    file.toUri().toString(), lines.lineOfOffset(referenceOffset) + 1,
                    lines.columnOfOffset(referenceOffset) + 1, null));

            assertNull(response.error());
            assertEquals("local", response.payload().get("source"));
            List<Map<String, Object>> targets = targets(response);
            assertEquals(1, targets.size());
            int declarationOffset = SOURCE.indexOf("counter");
            Map<String, Object> range = range(targets.getFirst());
            assertEquals(lines.lineOfOffset(declarationOffset) + 1, ((Number) range.get("startLine")).intValue());
            assertEquals(lines.columnOfOffset(declarationOffset) + 1, ((Number) range.get("startColumn")).intValue());
            assertEquals("counter",
                    SOURCE.substring(((Number) range.get("startOffset")).intValue(), ((Number) range.get("endOffset")).intValue()));
        } finally {
            controller.dispose();
            manager.dispose();
        }
    }

    @Test
    void referencesHonorIncludeDeclarationInTheLocalFallback() throws Exception {
        EditorManager manager = manager();
        Path file = Files.writeString(temporary.resolve("Main.java"), SOURCE);
        manager.openDocument(file);
        Surface surface = new Surface();
        WebShellNavigationController controller = new WebShellNavigationController(surface, manager, null);
        try {
            LineMap lines = LineMap.of(SOURCE);
            int declarationOffset = SOURCE.indexOf("counter");
            WebShellEnvelope withDeclaration = surface.handler("navigation", "references").handle(request("references",
                    file.toUri().toString(), lines.lineOfOffset(declarationOffset) + 1,
                    lines.columnOfOffset(declarationOffset) + 1, true));
            WebShellEnvelope withoutDeclaration = surface.handler("navigation", "references").handle(request("references",
                    file.toUri().toString(), lines.lineOfOffset(declarationOffset) + 1,
                    lines.columnOfOffset(declarationOffset) + 1, false));

            assertEquals("local", withDeclaration.payload().get("source"));
            assertEquals(2, targets(withDeclaration).size());
            int withDeclarationStart = ((Number) range(targets(withDeclaration).getFirst()).get("startOffset")).intValue();
            int withoutDeclarationStart = ((Number) range(targets(withoutDeclaration).getFirst()).get("startOffset")).intValue();
            assertEquals(declarationOffset, withDeclarationStart);
            assertEquals(1, targets(withoutDeclaration).size());
            assertEquals(SOURCE.indexOf("counter +"), withoutDeclarationStart);
        } finally {
            controller.dispose();
            manager.dispose();
        }
    }

    @Test
    void jdtServiceIsUsedBeforeTheLocalFallback() throws Exception {
        EditorManager manager = manager();
        Path file = Files.writeString(temporary.resolve("Main.java"), SOURCE);
        manager.openDocument(file);
        String uri = file.toUri().toString();
        int declarationOffset = SOURCE.indexOf("counter");
        LineMap lines = LineMap.of(SOURCE);
        JdtLsProjectService jdt = new JdtLsProjectService(new ProjectLifecycleService());
        Surface surface = new Surface();
        WebShellNavigationController controller = new WebShellNavigationController(surface, manager, jdt);
        try {
            navigationField().set(jdt, new NavigationService() {
                @Override
                public List<NavigationTarget> definition(Path file, String source, long version, int line, int column) {
                    return List.of(new NavigationTarget(uri, TextRange.of(declarationOffset, declarationOffset + 7),
                            TextRange.of(declarationOffset, declarationOffset + 7)));
                }

                @Override
                public List<NavigationTarget> references(Path file, String source, long version, int line, int column,
                                                          boolean includeDeclaration) {
                    return List.of();
                }
            });

            WebShellEnvelope response = surface.handler("navigation", "definition").handle(request("definition", uri,
                    lines.lineOfOffset(SOURCE.indexOf("counter +")) + 1,
                    lines.columnOfOffset(SOURCE.indexOf("counter +")) + 1, null));

            assertNull(response.error());
            assertEquals("jdt", response.payload().get("source"));
            assertEquals(1, targets(response).size());
            assertEquals(uri, targets(response).getFirst().get("uri"));
            assertEquals(lines.lineOfOffset(declarationOffset) + 1,
                    ((Number) range(targets(response).getFirst()).get("startLine")).intValue());
            assertEquals(lines.columnOfOffset(declarationOffset) + 1,
                    ((Number) range(targets(response).getFirst()).get("startColumn")).intValue());
        } finally {
            controller.dispose();
            jdt.close();
            manager.dispose();
        }
    }

    @Test
    void failingJdtServiceFallsBackToTheLocalResolver() throws Exception {
        EditorManager manager = manager();
        Path file = Files.writeString(temporary.resolve("Main.java"), SOURCE);
        manager.openDocument(file);
        LineMap lines = LineMap.of(SOURCE);
        int referenceOffset = SOURCE.indexOf("counter +");
        JdtLsProjectService jdt = new JdtLsProjectService(new ProjectLifecycleService());
        Surface surface = new Surface();
        WebShellNavigationController controller = new WebShellNavigationController(surface, manager, jdt);
        try {
            navigationField().set(jdt, new NavigationService() {
                @Override
                public List<NavigationTarget> definition(Path file, String source, long version, int line, int column) {
                    throw new IllegalStateException("JDT LS navigation failed");
                }

                @Override
                public List<NavigationTarget> references(Path file, String source, long version, int line, int column,
                                                          boolean includeDeclaration) {
                    throw new IllegalStateException("JDT LS navigation failed");
                }
            });

            WebShellEnvelope response = surface.handler("navigation", "definition").handle(request("definition",
                    file.toUri().toString(), lines.lineOfOffset(referenceOffset) + 1,
                    lines.columnOfOffset(referenceOffset) + 1, null));

            assertNull(response.error());
            assertEquals("local", response.payload().get("source"));
            assertEquals(1, targets(response).size());
        } finally {
            controller.dispose();
            jdt.close();
            manager.dispose();
        }
    }

    @Test
    void projectServiceWithoutAJdtSessionAnswersWithTheLocalFallback() throws Exception {
        EditorManager manager = manager();
        Path file = Files.writeString(temporary.resolve("Main.java"), SOURCE);
        manager.openDocument(file);
        LineMap lines = LineMap.of(SOURCE);
        int referenceOffset = SOURCE.indexOf("counter +");
        JdtLsProjectService jdt = new JdtLsProjectService(new ProjectLifecycleService());
        Surface surface = new Surface();
        WebShellNavigationController controller = new WebShellNavigationController(surface, manager, jdt);
        try {
            assertTrue(jdt.navigation().isEmpty());

            WebShellEnvelope response = surface.handler("navigation", "definition").handle(request("definition",
                    file.toUri().toString(), lines.lineOfOffset(referenceOffset) + 1,
                    lines.columnOfOffset(referenceOffset) + 1, null));

            assertEquals("local", response.payload().get("source"));
            assertEquals(1, targets(response).size());
        } finally {
            controller.dispose();
            jdt.close();
            manager.dispose();
        }
    }

    @Test
    void unresolvableCaretAnswersNoneWithoutError() throws Exception {
        EditorManager manager = manager();
        Path file = Files.writeString(temporary.resolve("Main.java"), SOURCE);
        manager.openDocument(file);
        Surface surface = new Surface();
        WebShellNavigationController controller = new WebShellNavigationController(surface, manager, null);
        try {
            WebShellEnvelope response = surface.handler("navigation", "definition").handle(request("definition",
                    file.toUri().toString(), 1, 1, null));

            assertNull(response.error());
            assertEquals("none", response.payload().get("source"));
            assertTrue(targets(response).isEmpty());
        } finally {
            controller.dispose();
            manager.dispose();
        }
    }

    @Test
    void unknownOrLessonDocumentsAnswerNone() {
        EditorManager manager = manager();
        Surface surface = new Surface();
        WebShellNavigationController controller = new WebShellNavigationController(surface, manager, null);
        try {
            WebShellEnvelope unknown = surface.handler("navigation", "definition").handle(request("definition",
                    "file:///nowhere/OutOfReach.java", 2, 9, null));
            WebShellEnvelope lesson = surface.handler("navigation", "references").handle(request("references",
                    "lesson://language/variables/main", 2, 9, true));

            assertEquals("none", unknown.payload().get("source"));
            assertTrue(targets(unknown).isEmpty());
            assertEquals("file:///nowhere/OutOfReach.java", unknown.payload().get("uri"));
            assertEquals("none", lesson.payload().get("source"));
            assertTrue(targets(lesson).isEmpty());
            assertEquals("lesson://language/variables/main", lesson.payload().get("uri"));
        } finally {
            controller.dispose();
            manager.dispose();
        }
    }

    private static Field navigationField() throws Exception {
        Field field = JdtLsProjectService.class.getDeclaredField("navigation");
        field.setAccessible(true);
        return field;
    }

    private static WebShellEnvelope request(String operation, String uri, int line, int column,
                                            Boolean includeDeclaration) {
        Map<String, Object> payload = includeDeclaration == null
                ? Map.of("uri", uri, "line", line, "column", column)
                : Map.of("uri", uri, "line", line, "column", column, "includeDeclaration", includeDeclaration);
        return WebShellEnvelope.request("navigation", operation, "nav-" + operation, payload);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> targets(WebShellEnvelope response) {
        return (List<Map<String, Object>>) response.payload().get("targets");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> range(Map<String, Object> target) {
        return (Map<String, Object>) target.get("range");
    }

    private static EditorManager manager() {
        return new EditorManager(new EventBus(), new DefaultFileSystemService(), new WebShellEditorViewFactory(),
                Runnable::run, new ProjectFileOperationService(), new NoOpEditorIntelligence());
    }

    private static final class NoOpEditorIntelligence implements EditorIntelligence {
        @Override public void activated(com.eyecode.editor.intelligence.document.DocumentSnapshot document) { }
        @Override public void deactivated(com.eyecode.editor.intelligence.document.DocumentSnapshot document) { }
        @Override public void closed(com.eyecode.editor.intelligence.document.DocumentSnapshot document) { }
        @Override public Optional<DefinitionLocation> resolveDefinition(
                com.eyecode.editor.intelligence.document.DocumentSnapshot document, int caretOffset) {
            return Optional.empty();
        }
        @Override public void close() { }
    }

    private static final class Surface implements WebShellSurface {
        private final Map<String, WebShellMessageHandler> handlers = new java.util.concurrent.ConcurrentHashMap<>();
        private final List<WebShellEnvelope> sent = java.util.Collections.synchronizedList(new ArrayList<>());

        @Override
        public void send(WebShellEnvelope message) {
            sent.add(message);
        }

        @Override
        public void registerHandler(String channel, String name, WebShellMessageHandler handler) {
            handlers.put(channel + '/' + name, handler);
        }

        private WebShellMessageHandler handler(String channel, String name) {
            return handlers.get(channel + '/' + name);
        }
    }
}
