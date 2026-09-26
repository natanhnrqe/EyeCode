package com.eyecode.ui.web;

import com.eyecode.eventbus.EventBus;
import com.eyecode.filesystem.DefaultFileSystemService;
import com.eyecode.language.DocumentLanguageResolver;
import com.eyecode.language.ExtensionDocumentLanguageResolver;
import com.eyecode.language.LanguageFeatureRequest;
import com.eyecode.language.LanguageId;
import com.eyecode.language.hover.HoverContent;
import com.eyecode.language.hover.HoverProvider;
import com.eyecode.language.hover.HoverResult;
import com.eyecode.language.hover.HoverService;
import com.eyecode.language.inlay.InlayHintService;
import com.eyecode.language.java.inlay.JavaInlayHintProvider;
import com.eyecode.language.signature.SignatureHelpProvider;
import com.eyecode.language.signature.SignatureHelpResult;
import com.eyecode.language.signature.SignatureHelpService;
import com.eyecode.language.signature.SignatureInformation;
import com.eyecode.language.signature.SignatureParameter;
import com.eyecode.project.ProjectFileOperationService;
import com.eyecode.workbench.editor.EditorIntelligence;
import com.eyecode.workbench.editor.EditorManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebShellLanguageFeatureControllerTest {
    @TempDir Path temporary;

    @Test
    void signatureHelpReturnsMappedResultForKnownSession() throws Exception {
        Surface surface = new Surface();
        EditorManager manager = manager();
        Path file = Files.writeString(temporary.resolve("Main.java"), "class Main { }");
        var session = manager.openDocument(file);
        var document = manager.getBuffer(session.getSessionId()).orElseThrow().getDocument();
        StubSignatureProvider provider = new StubSignatureProvider();
        provider.result = signatureResult();
        WebShellLanguageFeatureController controller = controller(surface, manager,
                new HoverService(resolver(), List.of()), new SignatureHelpService(resolver(), List.of(provider)));
        try {
            surface.handler("signatureHelp", "request").handle(WebShellEnvelope.request("signatureHelp", "request", "sig", Map.of(
                    "uri", file.toUri().toString(), "language", "java", "version", 4L, "offset", 30)));
            Map<String, Object> response = awaitResponse(surface, "sig");
            assertEquals("signatureHelp", response.get("feature"));
            assertEquals(file.toUri().toString(), response.get("uri"));
            assertEquals(document.currentVersion(), ((Number) response.get("version")).longValue());
            List<?> signatures = (List<?>) response.get("signatures");
            assertNotNull(signatures);
            assertEquals(1, signatures.size());
            Map<?, ?> signature = (Map<?, ?>) signatures.getFirst();
            assertEquals("substring(int begin, int end)", signature.get("label"));
            assertEquals("extrai trecho", signature.get("documentation"));
            assertEquals(1, signature.get("activeParameter"));
            List<?> parameters = (List<?>) signature.get("parameters");
            assertEquals(2, parameters.size());
            Map<?, ?> firstParameter = (Map<?, ?>) parameters.getFirst();
            assertEquals(30, firstParameter.get("labelStart"));
            assertEquals(35, firstParameter.get("labelEnd"));
            assertEquals("primeiro", firstParameter.get("documentation"));
            assertEquals(0, response.get("activeSignature"));
            assertEquals(1, response.get("activeParameter"));
        } finally {
            controller.dispose();
            manager.dispose();
        }
    }

    @Test
    void signatureHelpForUnknownUriReturnsStructuredEmpty() throws Exception {
        Surface surface = new Surface();
        EditorManager manager = manager();
        StubSignatureProvider provider = new StubSignatureProvider();
        WebShellLanguageFeatureController controller = controller(surface, manager,
                new HoverService(resolver(), List.of()), new SignatureHelpService(resolver(), List.of(provider)));
        try {
            surface.handler("signatureHelp", "request").handle(WebShellEnvelope.request("signatureHelp", "request", "missing", Map.of(
                    "uri", "file:///definitely-missing/Nowhere.java", "language", "java", "version", 4L, "offset", 0)));
            Map<String, Object> response = awaitResponse(surface, "missing");
            assertEquals("signatureHelp", response.get("feature"));
            List<?> signatures = (List<?>) response.get("signatures");
            assertNotNull(signatures, "resposta vazia deve manter a forma estruturada");
            assertTrue(signatures.isEmpty());
        } finally {
            controller.dispose();
            manager.dispose();
        }
    }

    @Test
    void signatureHelpForwardsTriggerCharacterToProvider() throws Exception {
        Surface surface = new Surface();
        EditorManager manager = manager();
        Path file = Files.writeString(temporary.resolve("Main.java"), "class Main { }");
        var session = manager.openDocument(file);
        StubSignatureProvider provider = new StubSignatureProvider();
        WebShellLanguageFeatureController controller = controller(surface, manager,
                new HoverService(resolver(), List.of()), new SignatureHelpService(resolver(), List.of(provider)));
        try {
            surface.handler("signatureHelp", "request").handle(WebShellEnvelope.request("signatureHelp", "request", "trigger", Map.of(
                    "uri", file.toUri().toString(), "language", "java", "version", 4L, "offset", 30,
                    "triggerCharacter", ",")));
            assertNotNull(provider.called.await(2, TimeUnit.SECONDS));
            assertEquals(",", provider.request.triggerCharacter());
        } finally {
            controller.dispose();
            manager.dispose();
        }
    }

    @Test
    void signatureHelpWithoutTriggerCharacterDefaultsToEmpty() throws Exception {
        Surface surface = new Surface();
        EditorManager manager = manager();
        Path file = Files.writeString(temporary.resolve("Main.java"), "class Main { }");
        var session = manager.openDocument(file);
        StubSignatureProvider provider = new StubSignatureProvider();
        WebShellLanguageFeatureController controller = controller(surface, manager,
                new HoverService(resolver(), List.of()), new SignatureHelpService(resolver(), List.of(provider)));
        try {
            surface.handler("signatureHelp", "request").handle(WebShellEnvelope.request("signatureHelp", "request", "invoked", Map.of(
                    "uri", file.toUri().toString(), "language", "java", "version", 4L, "offset", 30)));
            assertNotNull(provider.called.await(2, TimeUnit.SECONDS));
            assertEquals("", provider.request.triggerCharacter());
        } finally {
            controller.dispose();
            manager.dispose();
        }
    }

    @Test
    void hoverReturnsMappedResultForKnownSession() throws Exception {
        Surface surface = new Surface();
        EditorManager manager = manager();
        Path file = Files.writeString(temporary.resolve("Main.java"), "class Main { }");
        var session = manager.openDocument(file);
        var document = manager.getBuffer(session.getSessionId()).orElseThrow().getDocument();
        document.setText("class Main { String value; }");
        StubHoverProvider provider = new StubHoverProvider();
        provider.result = new HoverResult(List.of(new HoverContent("markdown", "**campo**")), 19, 24);
        WebShellLanguageFeatureController controller = controller(surface, manager,
                new HoverService(resolver(), List.of(provider)), new SignatureHelpService(resolver(), List.of()));
        try {
            surface.handler("hover", "request").handle(WebShellEnvelope.request("hover", "request", "hv", Map.of(
                    "uri", file.toUri().toString(), "language", "java", "version", 7L, "offset", 19)));
            Map<String, Object> response = awaitResponse(surface, "hv");
            assertEquals("hover", response.get("feature"));
            assertEquals(document.currentVersion(), ((Number) response.get("version")).longValue());
            List<?> contents = (List<?>) response.get("contents");
            assertNotNull(contents);
            assertEquals(1, contents.size());
            Map<?, ?> content = (Map<?, ?>) contents.getFirst();
            assertEquals("markdown", content.get("kind"));
            assertEquals("**campo**", content.get("value"));
            assertEquals(19, response.get("rangeStart"));
            assertEquals(24, response.get("rangeEnd"));
        } finally {
            controller.dispose();
            manager.dispose();
        }
    }

    @Test
    void hoverForUnknownUriReturnsStructuredEmpty() throws Exception {
        Surface surface = new Surface();
        EditorManager manager = manager();
        StubHoverProvider provider = new StubHoverProvider();
        WebShellLanguageFeatureController controller = controller(surface, manager,
                new HoverService(resolver(), List.of(provider)), new SignatureHelpService(resolver(), List.of()));
        try {
            surface.handler("hover", "request").handle(WebShellEnvelope.request("hover", "request", "missing", Map.of(
                    "uri", "file:///definitely-missing/Nowhere.java", "language", "java", "version", 7L, "offset", 0)));
            Map<String, Object> response = awaitResponse(surface, "missing");
            assertEquals("hover", response.get("feature"));
            List<?> contents = (List<?>) response.get("contents");
            assertNotNull(contents, "resposta vazia deve manter a forma estruturada");
            assertTrue(contents.isEmpty());
        } finally {
            controller.dispose();
            manager.dispose();
        }
    }

    @Test
    void hoverUsesSnapshotSourceAndVersionNotPayload() throws Exception {
        Surface surface = new Surface();
        EditorManager manager = manager();
        Path file = Files.writeString(temporary.resolve("Main.java"), "class Main { }");
        var session = manager.openDocument(file);
        var document = manager.getBuffer(session.getSessionId()).orElseThrow().getDocument();
        document.setText("class Main { String authoritative; }");
        StubHoverProvider provider = new StubHoverProvider();
        WebShellLanguageFeatureController controller = controller(surface, manager,
                new HoverService(resolver(), List.of(provider)), new SignatureHelpService(resolver(), List.of()));
        try {
            surface.handler("hover", "request").handle(WebShellEnvelope.request("hover", "request", "snapshot", Map.of(
                    "uri", file.toUri().toString(), "language", "java", "version", 99L, "offset", 19,
                    "content", "class Main { String stale; }")));
            assertNotNull(provider.called.await(2, TimeUnit.SECONDS));
            assertEquals(document.snapshot().getText(), provider.request.source());
            assertEquals(document.currentVersion(), provider.request.version());
        } finally {
            controller.dispose();
            manager.dispose();
        }
    }

    @Test
    void inlayHintsReturnsHintsForKnownSession() throws Exception {
        Surface surface = new Surface();
        EditorManager manager = manager();
        Path file = Files.writeString(temporary.resolve("Main.java"), "class Main { }");
        var session = manager.openDocument(file);
        var document = manager.getBuffer(session.getSessionId()).orElseThrow().getDocument();
        document.setText("class Main {\n"
                + "    void run() {\n"
                + "        int x = add(1, 2);\n"
                + "    }\n"
                + "    int add(int left, int right) {\n"
                + "        return left + right;\n"
                + "    }\n"
                + "}");
        WebShellLanguageFeatureController controller = new WebShellLanguageFeatureController(surface, manager,
                new HoverService(resolver(), List.of()), new SignatureHelpService(resolver(), List.of()),
                new InlayHintService(resolver(), List.of(new JavaInlayHintProvider())));
        try {
            String source = document.snapshot().getText();
            surface.handler("inlayHints", "request").handle(WebShellEnvelope.request("inlayHints", "request", "inlay", Map.of(
                    "uri", file.toUri().toString(), "language", "java", "version", 1L,
                    "fromOffset", 0, "toOffset", source.length(), "mode", "name")));
            Map<String, Object> response = awaitResponse(surface, "inlay");
            assertEquals("inlayHints", response.get("feature"));
            assertEquals(document.currentVersion(), ((Number) response.get("version")).longValue());
            List<?> hints = (List<?>) response.get("hints");
            assertNotNull(hints);
            assertEquals(2, hints.size());
            Map<?, ?> first = (Map<?, ?>) hints.getFirst();
            assertEquals("left:", first.get("label"));
            assertEquals(source.indexOf("add(1, 2)") + "add(".length(), first.get("offset"));
            Map<?, ?> second = (Map<?, ?>) hints.get(1);
            assertEquals("right:", second.get("label"));
            assertEquals(source.indexOf(", 2)") + ", ".length(), second.get("offset"));
        } finally {
            controller.dispose();
            manager.dispose();
        }
    }

    private static Map<String, Object> awaitResponse(Surface surface, String requestId) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            Optional<WebShellEnvelope> found = surface.sent.stream()
                    .filter(message -> message.payload().containsKey("feature")
                            && requestId.equals(message.payload().get("requestId")))
                    .findFirst();
            if (found.isPresent()) return found.get().payload();
            TimeUnit.MILLISECONDS.sleep(10);
        }
        throw new AssertionError("sem resposta de language feature; sent=" + surface.sent);
    }

    private static SignatureHelpResult signatureResult() {
        SignatureParameter begin = new SignatureParameter("begin", "primeiro", 30, 35);
        SignatureParameter end = new SignatureParameter("end", null, 37, 39);
        SignatureInformation signature = new SignatureInformation("substring(int begin, int end)", "extrai trecho",
                List.of(begin, end), 1);
        return new SignatureHelpResult(List.of(signature), 0, 1);
    }

    private static WebShellLanguageFeatureController controller(Surface surface, EditorManager manager,
                                                                HoverService hoverService,
                                                                SignatureHelpService signatureService) {
        return new WebShellLanguageFeatureController(surface, manager, hoverService, signatureService,
                new InlayHintService(resolver(), List.of()));
    }

    private static EditorManager manager() {
        return new EditorManager(new EventBus(), new DefaultFileSystemService(), new WebShellEditorViewFactory(),
                Runnable::run, new ProjectFileOperationService(), new NoOpEditorIntelligence());
    }

    private static DocumentLanguageResolver resolver() {
        return new ExtensionDocumentLanguageResolver(Map.of(LanguageId.JAVA, Set.of("java")));
    }

    private static final class StubSignatureProvider implements SignatureHelpProvider {
        private final CountDownLatch called = new CountDownLatch(1);
        private volatile LanguageFeatureRequest request;
        private volatile SignatureHelpResult result;

        @Override public LanguageId languageId() { return LanguageId.JAVA; }

        @Override
        public Optional<SignatureHelpResult> signatureHelp(LanguageFeatureRequest value) {
            request = value;
            called.countDown();
            return Optional.ofNullable(result);
        }
    }

    private static final class StubHoverProvider implements HoverProvider {
        private final CountDownLatch called = new CountDownLatch(1);
        private volatile LanguageFeatureRequest request;
        private volatile HoverResult result;

        @Override public LanguageId languageId() { return LanguageId.JAVA; }

        @Override
        public Optional<HoverResult> hover(LanguageFeatureRequest value) {
            request = value;
            called.countDown();
            return Optional.ofNullable(result);
        }
    }

    private static final class NoOpEditorIntelligence implements EditorIntelligence {
        @Override public void activated(com.eyecode.editor.intelligence.document.DocumentSnapshot document) { }
        @Override public void deactivated(com.eyecode.editor.intelligence.document.DocumentSnapshot document) { }
        @Override public void closed(com.eyecode.editor.intelligence.document.DocumentSnapshot document) { }
        @Override public Optional<com.eyecode.language.semantic.DefinitionLocation> resolveDefinition(
                com.eyecode.editor.intelligence.document.DocumentSnapshot document, int caretOffset) {
            return Optional.empty();
        }
        @Override public void close() { }
    }

    private static final class Surface implements WebShellSurface {
        private final Map<String, WebShellMessageHandler> handlers = new ConcurrentHashMap<>();
        private final List<WebShellEnvelope> sent = Collections.synchronizedList(new ArrayList<>());

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
