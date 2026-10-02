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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "eyecode.jdtls.integration", matches = "true")
final class WebShellCompletionAutoImportIntegrationTest {
    private static final String SOURCE = "public class Main {\n    void run() {\n        Arra\n    }\n}";

    @TempDir
    Path temporary;

    @Test
    void typeContextCompletionCarriesResolveIdAndAutoImportEdits() throws Exception {
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
            int replaceStart = SOURCE.indexOf("Arra");
            int offset = replaceStart + "Arra".length();
            surface.call("completion", "request", Map.of("uri", uri, "language", "java",
                    "version", opened.get("version"), "offset", offset, "explicit", true,
                    "replaceStart", replaceStart, "replaceEnd", offset, "requestId", completionRequestId));

            Map<String, Object> completion = waitForPayload(surface, completionRequestId, "items", Duration.ofSeconds(15));
            assertNotNull(completion, () -> "No completion response; events=" + surface.sent);
            Map<?, ?> arrayList = ((List<?>) completion.get("items")).stream()
                    .map(item -> (Map<?, ?>) item)
                    .filter(item -> String.valueOf(item.get("label")).startsWith("ArrayList"))
                    .filter(item -> !String.valueOf(item.get("resolveId")).isBlank())
                    .findFirst().orElse(null);
            assertNotNull(arrayList, () -> "ArrayList has no resolvable candidate; items=" + completion.get("items"));
            String resolveId = String.valueOf(arrayList.get("resolveId"));
            String label = String.valueOf(arrayList.get("label"));

            String resolveRequestId = UUID.randomUUID().toString();
            surface.call("completion", "resolve", Map.of("uri", uri, "requestId", resolveRequestId,
                    "resolveId", resolveId, "label", label));
            Map<String, Object> resolve = waitForPayload(surface, resolveRequestId, "edits", Duration.ofSeconds(10));
            assertNotNull(resolve, () -> "No resolve response; events=" + surface.sent);
            String edits = String.valueOf(resolve.get("edits"));
            assertTrue(edits.contains("java.util.ArrayList"), () -> "auto-import edit missing; resolve=" + resolve);
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

    private static Map<String, Object> waitForPayload(Surface surface, String requestId, String key, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            Map<String, Object> response = surface.response(requestId, key);
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
        Map<String, Object> response(String requestId, String payloadKey) {
            return sent.stream()
                    .filter(event -> requestId.equals(event.requestId()) && event.error() == null)
                    .map(event -> (Map<String, Object>) event.payload())
                    .filter(payload -> payload.containsKey(payloadKey))
                    .findFirst().orElse(null);
        }
    }
}
