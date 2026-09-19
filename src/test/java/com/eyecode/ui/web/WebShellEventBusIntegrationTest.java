package com.eyecode.ui.web;

import com.eyecode.application.WorkspaceApplication;
import com.eyecode.eventbus.EventBus;
import com.eyecode.filesystem.DefaultFileSystemService;
import com.eyecode.diagnostics.JavaDiagnosticsProvider;
import com.eyecode.language.DocumentLanguageResolver;
import com.eyecode.language.ExtensionDocumentLanguageResolver;
import com.eyecode.language.LanguageId;
import com.eyecode.language.diagnostics.DiagnosticsService;
import com.eyecode.language.java.event.TokensUpdatedEvent;
import com.eyecode.project.MavenProjectCreationService;
import com.eyecode.project.ProjectFileOperationService;
import com.eyecode.project.ProjectLifecycleService;
import com.eyecode.runtime.RunService;
import com.eyecode.terminal.TerminalService;
import com.eyecode.workbench.editor.EditorManager;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebShellEventBusIntegrationTest {

    @Test
    void documentChangePublishesDerivedLexicalSnapshotToTheSharedBus() {
        EventBus eventBus = new EventBus();
        ProjectFileOperationService fileOperations = new ProjectFileOperationService();
        EditorManager manager = new EditorManager(eventBus, new DefaultFileSystemService(),
                new WebShellEditorViewFactory(), fileOperations);
        ProjectLifecycleService lifecycle = new ProjectLifecycleService();
        RunService runService = new RunService(lifecycle);
        TerminalService terminalService = new TerminalService();
        WorkspaceApplication application = new WorkspaceApplication(manager, lifecycle, runService, terminalService);
        CapturingSurface surface = new CapturingSurface();
        DocumentLanguageResolver languageResolver = languageResolver();
        WebShellDiagnosticsController diagnostics = new WebShellDiagnosticsController(surface,
                new DiagnosticsService(languageResolver, List.of(new JavaDiagnosticsProvider())));
        WebShellExecutionController execution = new WebShellExecutionController(surface, lifecycle, runService, terminalService);
        WebShellDocumentController documents = new WebShellDocumentController(surface, target -> { },
                null, WebShellNativeUi.unavailable(), manager, diagnostics, languageResolver);
        List<TokensUpdatedEvent> updates = new CopyOnWriteArrayList<>();
        eventBus.subscribe(TokensUpdatedEvent.class, updates::add);
        try {
            WebShellEnvelope created = surface.handler("document", "new").handle(
                    WebShellEnvelope.request("document", "new", "new-1", Map.of("content", "class Main {}")));
            @SuppressWarnings("unchecked")
            Map<String, Object> document = (Map<String, Object>) created.payload().get("document");

            WebShellEnvelope changed = surface.handler("document", "change").handle(
                    WebShellEnvelope.request("document", "change", "change-1", Map.of(
                            "uri", document.get("uri"),
                            "version", document.get("version"),
                            "content", "class Main { int age = 20; }")));

            assertEquals(1, updates.size());
            assertEquals(((Number) ((Map<?, ?>) changed.payload().get("document")).get("version")).longValue(),
                    updates.getFirst().getSnapshot().version());
            assertTrue(updates.getFirst().getSnapshot().tokens().stream().anyMatch(token -> token.text().equals("age")));
        } finally {
            documents.dispose();
            diagnostics.dispose();
            execution.close();
            application.close();
        }
    }

    @Test
    void webRunProtocolReportsStateOutputAndSuccessfulCompletion() throws Exception {
        Path root = Files.createTempDirectory("eyecode-web-run");
        Path source = root.resolve("src/main/java");
        Files.createDirectories(source);
        Files.writeString(source.resolve("Main.java"),
                "public class Main { public static void main(String[] args) { System.out.println(\"WEB_RUN_OK\"); } }");
        ProjectLifecycleService lifecycle = new ProjectLifecycleService();
        RunService runService = new RunService(lifecycle);
        TerminalService terminalService = new TerminalService();
        CapturingSurface surface = new CapturingSurface();
        WebShellExecutionController execution = new WebShellExecutionController(surface, lifecycle, runService, terminalService);
        lifecycle.open(root);

        try {
            WebShellEnvelope response = surface.handler("run", "run").handle(
                    WebShellEnvelope.request("run", "run", "run-1", Map.of()));

            assertEquals(true, response.payload().get("started"));
            assertTrue(await(() -> surface.events().stream().anyMatch(event -> event.channel().equals("run")
                    && event.name().equals("state") && Boolean.FALSE.equals(event.payload().get("running"))
                    && Boolean.TRUE.equals(event.payload().get("finished"))
                    && Integer.valueOf(0).equals(event.payload().get("exitCode"))), 20, TimeUnit.SECONDS));
            assertTrue(surface.events().stream().anyMatch(event -> event.channel().equals("run")
                    && event.name().equals("state") && Boolean.TRUE.equals(event.payload().get("running"))));
            assertTrue(surface.events().stream().anyMatch(event -> event.channel().equals("run")
                    && event.name().equals("state") && "RUNNING".equals(event.payload().get("phase"))));
            assertTrue(surface.events().stream().anyMatch(event -> event.channel().equals("run")
                    && event.name().equals("output") && String.valueOf(event.payload().get("text")).contains("WEB_RUN_OK")));
        } finally {
            execution.close();
            runService.dispose();
            terminalService.dispose();
        }
    }

    private static DocumentLanguageResolver languageResolver() {
        return new ExtensionDocumentLanguageResolver(Map.of(LanguageId.JAVA, java.util.Set.of("java")));
    }

    private static boolean await(BooleanSupplier condition, long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) return true;
            Thread.sleep(20);
        }
        return condition.getAsBoolean();
    }

    private static final class CapturingSurface implements WebShellSurface {
        private final Map<String, WebShellMessageHandler> handlers = new java.util.concurrent.ConcurrentHashMap<>();
        private final List<WebShellEnvelope> events = new CopyOnWriteArrayList<>();

        @Override
        public void send(WebShellEnvelope message) {
            events.add(message);
        }

        @Override
        public void registerHandler(String channel, String name, WebShellMessageHandler handler) {
            handlers.put(channel + "/" + name, handler);
        }

        private WebShellMessageHandler handler(String channel, String name) {
            return handlers.get(channel + "/" + name);
        }

        private List<WebShellEnvelope> events() {
            return events;
        }
    }
}
