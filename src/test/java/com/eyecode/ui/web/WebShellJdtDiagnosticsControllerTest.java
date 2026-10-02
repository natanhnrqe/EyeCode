package com.eyecode.ui.web;

import com.eyecode.language.diagnostics.Diagnostic;
import com.eyecode.language.diagnostics.DiagnosticSeverity;
import com.eyecode.language.java.lsp.JdtLsProjectService;
import com.eyecode.project.ProjectLifecycleService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebShellJdtDiagnosticsControllerTest {

    @Test
    void quickFixRequestIsAcceptedAndTheFixesResponseArrivesThroughTheSurface() {
        Surface surface = new Surface();
        WebShellJdtDiagnosticsController controller = new WebShellJdtDiagnosticsController(surface,
                new JdtLsProjectService(new ProjectLifecycleService()));

        WebShellEnvelope ack = surface.handler("diagnostics", "quickFix").handle(request("one", Map.of(
                "uri", "file:///project/src/Main.java",
                "range", Map.of("startLine", 3, "startColumn", 5, "endLine", 3, "endColumn", 12))));

        assertEquals("RESPONSE", ack.kind().name());
        assertEquals("diagnostics", ack.channel());
        assertEquals("quickFix", ack.name());
        assertEquals(Boolean.TRUE, ack.payload().get("accepted"));
        assertEquals("one", ack.payload().get("requestId"));

        Map<String, Object> response = waitForFixes(surface, "one");
        assertNotNull(response, () -> "No quickFix response; sent=" + surface.sent);
        assertEquals(List.of(), response.get("fixes"));
        controller.dispose();
    }

    @Test
    void quickFixFallsBackToEmptyFixesWhenJdtIsAbsent() {
        Surface surface = new Surface();
        WebShellJdtDiagnosticsController controller = new WebShellJdtDiagnosticsController(surface,
                new JdtLsProjectService(new ProjectLifecycleService()));

        surface.handler("diagnostics", "quickFix").handle(request("one", Map.of(
                "uri", "file:///project/src/Main.java",
                "range", Map.of("startLine", 1, "startColumn", 1, "endLine", 1, "endColumn", 6))));

        Map<String, Object> response = waitForFixes(surface, "one");
        assertNotNull(response, () -> "No quickFix response; sent=" + surface.sent);
        assertEquals(List.of(), response.get("fixes"));
        controller.dispose();
    }

    @Test
    void quickFixRequiresADocumentUri() {
        Surface surface = new Surface();
        WebShellJdtDiagnosticsController controller = new WebShellJdtDiagnosticsController(surface,
                new JdtLsProjectService(new ProjectLifecycleService()));

        WebShellEnvelope response = surface.handler("diagnostics", "quickFix").handle(request("one", Map.of(
                "range", Map.of("startLine", 1, "startColumn", 1, "endLine", 1, "endColumn", 6))));

        assertEquals("INVALID_QUICKFIX_REQUEST", response.error().code());
        assertTrue(response.error().recoverable());
        controller.dispose();
    }

    @Test
    void jdtPublishEventCarriesTheDocumentedEnvelopeShape() {
        Surface surface = new Surface();
        WebShellJdtDiagnosticsController controller = new WebShellJdtDiagnosticsController(surface,
                new JdtLsProjectService(new ProjectLifecycleService()));

        controller.publishJdt("file:///project/src/Main.java", List.of(new Diagnostic(
                DiagnosticSeverity.ERROR, "67108964", "Foo cannot be resolved to a type", 1, 5, 1, 15, "jdt")));

        List<WebShellEnvelope> events = surface.sent.stream()
                .filter(message -> "diagnostics".equals(message.channel()) && "jdtPublish".equals(message.name()))
                .toList();
        assertEquals(1, events.size());
        WebShellEnvelope event = events.getFirst();
        assertEquals("EVENT", event.kind().name());
        assertEquals("file:///project/src/Main.java", event.payload().get("uri"));
        List<?> diagnostics = (List<?>) event.payload().get("diagnostics");
        assertEquals(1, diagnostics.size());
        Map<?, ?> diagnostic = (Map<?, ?>) diagnostics.getFirst();
        assertEquals("ERROR", diagnostic.get("severity"));
        assertEquals("Foo cannot be resolved to a type", diagnostic.get("message"));
        assertEquals("jdt", diagnostic.get("source"));
        Map<?, ?> range = (Map<?, ?>) diagnostic.get("range");
        assertEquals(1, range.get("startLine"));
        assertEquals(5, range.get("startColumn"));
        assertEquals(1, range.get("endLine"));
        assertEquals(15, range.get("endColumn"));
        controller.dispose();
    }

    @Test
    void jdtPublishAfterDisposeIsNeverSent() {
        Surface surface = new Surface();
        WebShellJdtDiagnosticsController controller = new WebShellJdtDiagnosticsController(surface,
                new JdtLsProjectService(new ProjectLifecycleService()));
        controller.dispose();

        controller.publishJdt("file:///project/src/Main.java", List.of(new Diagnostic(
                DiagnosticSeverity.ERROR, "", "boom", 1, 1, 1, 5, "jdt")));

        assertTrue(surface.sent.isEmpty());
    }

    @Test
    void blankJdtPublishIsIgnored() {
        Surface surface = new Surface();
        WebShellJdtDiagnosticsController controller = new WebShellJdtDiagnosticsController(surface,
                new JdtLsProjectService(new ProjectLifecycleService()));

        controller.publishJdt("", List.of(new Diagnostic(DiagnosticSeverity.ERROR, "", "boom", 1, 1, 1, 5, "jdt")));

        assertTrue(surface.sent.isEmpty());
        controller.dispose();
    }

    @Test
    void syncRequestIsAcceptedEvenWithoutAJdtSession() {
        Surface surface = new Surface();
        WebShellJdtDiagnosticsController controller = new WebShellJdtDiagnosticsController(surface,
                new JdtLsProjectService(new ProjectLifecycleService()));

        WebShellEnvelope ack = surface.handler("diagnostics", "sync").handle(request("sync", "two", Map.of(
                "uri", "file:///project/src/Main.java",
                "content", "public class Main { void run() { } }",
                "version", 3)));

        assertEquals("RESPONSE", ack.kind().name());
        assertEquals("diagnostics", ack.channel());
        assertEquals("sync", ack.name());
        assertEquals(Boolean.TRUE, ack.payload().get("accepted"));
        assertEquals("two", ack.payload().get("requestId"));
        controller.dispose();
    }

    @Test
    void syncRequiresAFileUriAndContent() {
        Surface surface = new Surface();
        WebShellJdtDiagnosticsController controller = new WebShellJdtDiagnosticsController(surface,
                new JdtLsProjectService(new ProjectLifecycleService()));

        WebShellEnvelope missingUri = surface.handler("diagnostics", "sync").handle(request("sync", "three", Map.of(
                "content", "class A {}")));
        assertEquals("INVALID_SYNC_REQUEST", missingUri.error().code());
        assertTrue(missingUri.error().recoverable());

        WebShellEnvelope missingContent = surface.handler("diagnostics", "sync").handle(request("sync", "four", Map.of(
                "uri", "file:///project/src/Main.java")));
        assertEquals("INVALID_SYNC_REQUEST", missingContent.error().code());

        WebShellEnvelope nonFileUri = surface.handler("diagnostics", "sync").handle(request("sync", "five", Map.of(
                "uri", "untitled:///scratch", "content", "class A {}")));
        assertEquals("INVALID_SYNC_REQUEST", nonFileUri.error().code());
        controller.dispose();
    }

    private static WebShellEnvelope request(String requestId, Map<String, Object> payload) {
        return request("quickFix", requestId, payload);
    }

    private static WebShellEnvelope request(String operation, String requestId, Map<String, Object> payload) {
        return WebShellEnvelope.request("diagnostics", operation, requestId, payload);
    }

    private static Map<String, Object> waitForFixes(Surface surface, String requestId) {
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(2).toNanos();
        while (System.nanoTime() < deadline) {
            Map<String, Object> response = surface.fixesResponse(requestId);
            if (response != null) return response;
            LockSupport.parkNanos(10_000_000L);
        }
        return null;
    }

    private static final class Surface implements WebShellSurface {
        private final Map<String, WebShellMessageHandler> handlers = new ConcurrentHashMap<>();
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

        @SuppressWarnings("unchecked")
        private Map<String, Object> fixesResponse(String requestId) {
            synchronized (sent) {
                return sent.stream()
                        .filter(message -> "quickFix".equals(message.name()) && requestId.equals(message.requestId())
                                && message.payload().containsKey("fixes"))
                        .map(message -> (Map<String, Object>) message.payload()).findFirst().orElse(null);
            }
        }
    }
}
