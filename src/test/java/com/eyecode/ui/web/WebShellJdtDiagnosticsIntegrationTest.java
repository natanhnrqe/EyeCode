package com.eyecode.ui.web;

import com.eyecode.language.java.lsp.JdtLsLifecycleState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "eyecode.jdtls.integration", matches = "true")
final class WebShellJdtDiagnosticsIntegrationTest {
    private static final String SOURCE = "public class Main {\n    void run() {\n        UnknownType value;\n        value.\n    }\n}";

    @TempDir Path temporary;

    @Test
    void jdtPublishDiagnosticsFlowThroughTheWebShellContract() throws Exception {
        Path root = Files.createDirectories(temporary.resolve("workspace"));
        Path file = root.resolve("Main.java");
        Files.writeString(file, SOURCE);
        Surface surface = new Surface();
        try (var runtime = WebShellWorkspaceComposition.create(surface)) {
            assertNull(surface.call("workspace", "openProject", Map.of("path", root.toString())).error());
            assertEquals(JdtLsLifecycleState.READY, waitForJdt(runtime, Duration.ofSeconds(30)));
            Map<String, Object> opened = document(surface.call("workspace", "openFile", Map.of("path", file.toString())));
            String uri = opened.get("uri").toString();

            String completionRequestId = UUID.randomUUID().toString();
            int memberAccessOffset = SOURCE.indexOf("value.") + "value.".length();
            surface.call("completion", "request", Map.of("uri", uri, "language", "java",
                    "version", opened.get("version"), "offset", memberAccessOffset, "explicit", true,
                    "replaceStart", memberAccessOffset, "replaceEnd", memberAccessOffset, "requestId", completionRequestId));

            Map<String, Object> published = waitForJdtPublish(surface, uri, Duration.ofSeconds(60));
            assertNotNull(published, () -> "No jdtPublish event; events=" + surface.sent);
            List<?> diagnostics = (List<?>) published.get("diagnostics");
            assertTrue(diagnostics.stream()
                    .map(item -> (Map<?, ?>) item)
                    .anyMatch(item -> "ERROR".equals(item.get("severity"))
                            && String.valueOf(item.get("message")).contains("UnknownType")
                            && "jdt".equals(item.get("source"))), () -> "diagnostics=" + diagnostics);
            Map<?, ?> error = diagnostics.stream()
                    .map(item -> (Map<?, ?>) item)
                    .filter(item -> "ERROR".equals(item.get("severity")))
                    .findFirst().orElseThrow();
            Map<?, ?> range = (Map<?, ?>) error.get("range");
            assertTrue(((Number) range.get("startLine")).intValue() >= 1, () -> "range=" + range);

            String quickFixRequestId = UUID.randomUUID().toString();
            surface.call("diagnostics", "quickFix", Map.of("uri", uri, "requestId", quickFixRequestId,
                    "range", Map.of("startLine", 3, "startColumn", 9, "endLine", 3, "endColumn", 20)));
            Map<String, Object> quickFix = waitForResponse(surface, quickFixRequestId, Duration.ofSeconds(10));
            assertNotNull(quickFix, () -> "No quickFix response; events=" + surface.sent);
            assertTrue(quickFix.containsKey("fixes"), () -> "quickFix=" + quickFix);
        }
    }

    private static JdtLsLifecycleState waitForJdt(WebShellWorkspaceRuntime runtime, Duration timeout)
            throws Exception {
        Field field = WebShellWorkspaceRuntime.class.getDeclaredField("jdt");
        field.setAccessible(true);
        var service = field.get(runtime);
        Field state = service.getClass().getDeclaredField("session");
        state.setAccessible(true);
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            Object session = state.get(service);
            if (session != null && JdtLsLifecycleState.READY.equals(((com.eyecode.language.java.lsp.JdtLsSession) session).state())) {
                return JdtLsLifecycleState.READY;
            }
            LockSupport.parkNanos(25_000_000L);
        }
        return JdtLsLifecycleState.STOPPED;
    }

    private static Map<String, Object> waitForJdtPublish(Surface surface, String uri, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            Map<String, Object> published = surface.jdtPublish(uri);
            if (published != null) return published;
            LockSupport.parkNanos(25_000_000L);
        }
        return null;
    }

    private static Map<String, Object> waitForResponse(Surface surface, String requestId, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            Map<String, Object> response = surface.response(requestId);
            if (response != null) return response;
            LockSupport.parkNanos(25_000_000L);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> document(WebShellEnvelope response) {
        assertNull(response.error());
        return (Map<String, Object>) response.payload().get("document");
    }

    private static final class Surface implements WebShellSurface {
        private final Map<String, WebShellMessageHandler> handlers = new ConcurrentHashMap<>();
        private final CopyOnWriteArrayList<WebShellEnvelope> sent = new CopyOnWriteArrayList<>();

        @Override
        public void registerHandler(String channel, String name, WebShellMessageHandler handler) {
            handlers.put(channel + "/" + name, handler);
        }

        @Override
        public void send(WebShellEnvelope event) {
            sent.add(event);
        }

        WebShellEnvelope call(String channel, String name, Map<String, Object> payload) {
            return handlers.get(channel + "/" + name).handle(WebShellEnvelope.request(channel, name,
                    payload.getOrDefault("requestId", UUID.randomUUID()).toString(), payload));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> jdtPublish(String uri) {
            return sent.stream()
                    .filter(event -> "diagnostics".equals(event.channel()) && "jdtPublish".equals(event.name())
                            && uri.equals(event.payload().get("uri")))
                    .map(event -> (Map<String, Object>) event.payload()).findFirst().orElse(null);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> response(String requestId) {
            return sent.stream().filter(event -> requestId.equals(event.requestId())
                            && (event.payload().containsKey("items") || event.payload().containsKey("fixes")))
                    .map(event -> (Map<String, Object>) event.payload()).findFirst().orElse(null);
        }
    }
}
