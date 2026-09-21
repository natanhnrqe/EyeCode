package com.eyecode.ui.web;

import com.eyecode.eventbus.EventBus;
import com.eyecode.filesystem.DefaultFileSystemService;
import com.eyecode.language.DocumentLanguageResolver;
import com.eyecode.language.ExtensionDocumentLanguageResolver;
import com.eyecode.language.LanguageId;
import com.eyecode.language.completion.CompletionCandidate;
import com.eyecode.language.completion.CompletionProvider;
import com.eyecode.language.completion.CompletionRequest;
import com.eyecode.language.completion.CompletionResult;
import com.eyecode.language.completion.CompletionService;
import com.eyecode.language.semantic.DefinitionLocation;
import com.eyecode.project.ProjectFileOperationService;
import com.eyecode.workbench.editor.EditorIntelligence;
import com.eyecode.workbench.editor.EditorManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebShellCompletionControllerTest {
    @TempDir Path temporary;

    @Test
    void existingEditorSessionIgnoresStalePayloadTextAndVersion() throws Exception {
        Surface surface = new Surface();
        EditorManager manager = manager();
        Path file = Files.writeString(temporary.resolve("Main.java"), "class Main { String current; }");
        var session = manager.openDocument(file);
        var document = manager.getBuffer(session.getSessionId()).orElseThrow().getDocument();
        document.setText("class Main { String authoritative; }");
        CapturingProvider provider = new CapturingProvider();
        WebShellCompletionController controller = new WebShellCompletionController(surface, manager,
                new CompletionService(resolver(), List.of(provider)));
        try {
            surface.handler("completion", "request").handle(WebShellEnvelope.request("completion", "request", "current", Map.of(
                    "uri", file.toUri().toString(), "language", "java", "content", "class Main { String stale; }",
                    "version", 1L, "offset", 20, "explicit", true)));
            assertTrue(provider.called.await(2, TimeUnit.SECONDS));
            assertEquals(document.snapshot().getText(), provider.request.source());
            assertEquals(document.currentVersion(), provider.request.version());
        } finally {
            controller.dispose();
            manager.dispose();
        }
    }

    @Test
    void publishesOnlyTheLatestCompletionForADocument() throws InterruptedException {
        Surface surface = new Surface();
        EditorManager manager = manager();
        BlockingProvider provider = new BlockingProvider();
        WebShellCompletionController controller = new WebShellCompletionController(surface, manager,
                new CompletionService(resolver(), List.of(provider)));
        try {
            surface.handler("completion", "request").handle(request("one", "first", 1));
            assertTrue(provider.firstStarted.await(2, TimeUnit.SECONDS));
            surface.handler("completion", "request").handle(request("two", "second", 2));
            provider.releaseFirst.countDown();

            assertTrue(surface.secondPublished.await(2, TimeUnit.SECONDS));
            List<WebShellEnvelope> responses = surface.sent.stream()
                    .filter(message -> message.payload().containsKey("items")).toList();
            assertEquals(1, responses.size());
            assertEquals("two", responses.getFirst().payload().get("requestId"));
            assertEquals(2L, responses.getFirst().payload().get("version"));
        } finally {
            controller.dispose();
            manager.dispose();
        }
    }

    private static EditorManager manager() {
        return new EditorManager(new EventBus(), new DefaultFileSystemService(), new WebShellEditorViewFactory(),
                Runnable::run, new ProjectFileOperationService(), new NoOpEditorIntelligence());
    }

    private static DocumentLanguageResolver resolver() {
        return new ExtensionDocumentLanguageResolver(Map.of(LanguageId.JAVA, Set.of("java")));
    }

    private static WebShellEnvelope request(String requestId, String content, long version) {
        return WebShellEnvelope.request("completion", "request", requestId, Map.of(
                "uri", "lesson://language/stale/main",
                "language", "java",
                "lessonPractice", true,
                "content", content,
                "version", version,
                "offset", content.length(),
                "explicit", true,
                "replaceStart", 0,
                "replaceEnd", content.length()));
    }

    private static final class BlockingProvider implements CompletionProvider {
        private final CountDownLatch firstStarted = new CountDownLatch(1);
        private final CountDownLatch releaseFirst = new CountDownLatch(1);

        @Override public LanguageId languageId() { return LanguageId.JAVA; }

        @Override
        public CompletionResult complete(CompletionRequest request) {
            if (request.source().equals("first")) {
                firstStarted.countDown();
                try {
                    releaseFirst.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return CompletionResult.empty();
                }
            }
            return new CompletionResult(List.of(new CompletionCandidate(request.source(), "KEYWORD", "", "",
                    request.source(), request.source(), false, 0, request.source().length(), 0,
                    "", "", "", "", "", List.of())));
        }
    }

    private static final class CapturingProvider implements CompletionProvider {
        private final CountDownLatch called = new CountDownLatch(1);
        private volatile CompletionRequest request;
        @Override public LanguageId languageId() { return LanguageId.JAVA; }
        @Override public CompletionResult complete(CompletionRequest value) {
            request = value;
            called.countDown();
            return CompletionResult.empty();
        }
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
        private final CountDownLatch secondPublished = new CountDownLatch(1);

        @Override
        public void send(WebShellEnvelope message) {
            sent.add(message);
            if (message.payload().containsKey("items") && "two".equals(message.payload().get("requestId"))) {
                secondPublished.countDown();
            }
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
