package com.eyecode.ui.web;

import com.eyecode.language.java.lsp.JdtLsLifecycleState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "eyecode.jdtls.integration", matches = "true")
final class WebShellJdtSameFileDiagnosticTest {
    @TempDir Path temporary;

    @Test
    void sameFileCompletionCanBeTracedThroughTheWebShell() throws Exception {
        Path root = Files.createDirectories(temporary.resolve("workspace"));
        Path file = root.resolve("Main.java");
        String source = "class Main { void run() { String value = \"EyeCode\"; value. } }";
        Files.writeString(file, source);
        Surface surface = new Surface();
        try (var runtime = WebShellWorkspaceComposition.create(surface)) {
            assertNull(surface.call("workspace", "openProject", Map.of("path", root.toString())).error());
            assertEquals(JdtLsLifecycleState.READY, waitForJdt(runtime, Duration.ofSeconds(30)));
            Map<String, Object> opened = document(surface.call("workspace", "openFile", Map.of("path", file.toString())));
            String uri = opened.get("uri").toString();
            int offset = source.indexOf("value.") + "value.".length();
            String requestId = UUID.randomUUID().toString();
            surface.call("completion", "request", Map.of("uri", uri, "language", "java",
                    "version", opened.get("version"), "offset", offset, "explicit", true,
                    "replaceStart", offset, "replaceEnd", offset, "requestId", requestId));
            Map<String, Object> response = waitForResponse(surface, requestId, Duration.ofSeconds(10));
            assertTrue(response != null, () -> "No WebShell response; events=" + surface.sent);
            assertTrue(((java.util.List<?>) response.get("items")).stream()
                    .map(item -> ((Map<?, ?>) item).get("label").toString())
                    .anyMatch(label -> label.equals("substring")), () -> "items=" + response.get("items"));
            assertTrue(((java.util.List<?>) response.get("items")).stream()
                    .map(item -> (Map<?, ?>) item)
                    .anyMatch(item -> "java.lang.String".equals(item.get("owner"))
                            && "substring()".equals(item.get("insertText"))));
        }
    }

    @Test
    void crossFileCompletionAlsoReturnsThroughTheWebShellContract() throws Exception {
        Path root = Files.createDirectories(temporary.resolve("cross-file"));
        Path usuario = root.resolve("Usuario.java");
        Path main = root.resolve("Main.java");
        String usuarioSource = "public class Usuario { public String getNome() { return \"EyeCode\"; } }";
        String mainSource = "public class Main { void run() { Usuario usuario = new Usuario(); usuario. } }";
        Files.writeString(usuario, usuarioSource);
        Files.writeString(main, mainSource);
        Surface surface = new Surface();
        try (var runtime = WebShellWorkspaceComposition.create(surface)) {
            assertNull(surface.call("workspace", "openProject", Map.of("path", root.toString())).error());
            assertEquals(JdtLsLifecycleState.READY, waitForJdt(runtime, Duration.ofSeconds(30)));
            Map<String, Object> userDocument = document(surface.call("workspace", "openFile", Map.of("path", usuario.toString())));
            Map<String, Object> mainDocument = document(surface.call("workspace", "openFile", Map.of("path", main.toString())));
            String userRequest = UUID.randomUUID().toString();
            surface.call("completion", "request", Map.of("uri", userDocument.get("uri"), "language", "java",
                    "version", userDocument.get("version"), "offset", usuarioSource.length(), "explicit", true,
                    "replaceStart", usuarioSource.length(), "replaceEnd", usuarioSource.length(), "requestId", userRequest));
            assertTrue(waitForResponse(surface, userRequest, Duration.ofSeconds(10)) != null);
            int offset = mainSource.indexOf("usuario.") + "usuario.".length();
            String mainRequest = UUID.randomUUID().toString();
            surface.call("completion", "request", Map.of("uri", mainDocument.get("uri"), "language", "java",
                    "version", mainDocument.get("version"), "offset", offset, "explicit", true,
                    "replaceStart", offset, "replaceEnd", offset, "requestId", mainRequest));
            Map<String, Object> response = waitForResponse(surface, mainRequest, Duration.ofSeconds(10));
            assertTrue(response != null, () -> "No cross-file response; events=" + surface.sent);
            assertTrue(((java.util.List<?>) response.get("items")).stream()
                    .map(item -> ((Map<?, ?>) item).get("label").toString())
                    .anyMatch(label -> label.startsWith("getNome()")), () -> "items=" + response.get("items"));
        }
    }

    @Test
    void hoverAndSignatureHelpReturnThroughTheWebShellContract() throws Exception {
        Path root = Files.createDirectories(temporary.resolve("language-features"));
        Path file = root.resolve("Main.java");
        String source = "class Main {\n    void run() {\n        String value = \"EyeCode\";\n        value.substring(0, 3);\n    }\n}";
        String signatureSource = source.replace("value.substring(0, 3);", "value.substring(");
        Files.writeString(file, source);
        Surface surface = new Surface();
        try (var runtime = WebShellWorkspaceComposition.create(surface)) {
            assertNull(surface.call("workspace", "openProject", Map.of("path", root.toString())).error());
            assertEquals(JdtLsLifecycleState.READY, waitForJdt(runtime, Duration.ofSeconds(30)));
            Map<String, Object> opened = document(surface.call("workspace", "openFile", Map.of("path", file.toString())));
            assertEquals(source, opened.get("content"));
            String uri = opened.get("uri").toString();
            int hoverOffset = source.indexOf("substring") + 3;
            String hoverRequest = UUID.randomUUID().toString();
            surface.call("hover", "request", Map.of("uri", uri, "language", "java",
                    "version", opened.get("version"), "offset", hoverOffset, "requestId", hoverRequest));
            Map<String, Object> hover = waitForResponse(surface, hoverRequest, Duration.ofSeconds(10));
            assertTrue(hover != null, () -> "No hover response; events=" + surface.sent);
            assertTrue(((java.util.List<?>) hover.get("contents")).stream()
                    .map(item -> ((Map<?, ?>) item).get("value").toString().toLowerCase())
                    .anyMatch(value -> value.contains("substring")), () -> "hover=" + hover);

            Map<String, Object> changed = document(surface.call("document", "change", Map.of(
                    "uri", uri, "version", opened.get("version"), "content", signatureSource)));
            int signatureOffset = signatureSource.indexOf("substring(") + "substring(".length();
            String signatureRequest = UUID.randomUUID().toString();
            surface.call("signatureHelp", "request", Map.of("uri", uri, "language", "java",
                    "version", changed.get("version"), "offset", signatureOffset, "requestId", signatureRequest));
            Map<String, Object> signature = waitForResponse(surface, signatureRequest, Duration.ofSeconds(10));
            assertTrue(signature != null, () -> "No signature response; events=" + surface.sent);
            assertTrue(((java.util.List<?>) signature.get("signatures")).stream()
                    .map(item -> ((Map<?, ?>) item).get("label").toString())
                    .anyMatch(label -> label.contains("substring")));
            assertTrue(((Number) signature.get("activeParameter")).intValue() >= 0);
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
        Map<String, Object> response(String requestId) {
            return sent.stream().filter(event -> requestId.equals(event.requestId())
                            && (event.payload().containsKey("items") || event.payload().containsKey("feature")))
                    .map(event -> (Map<String, Object>) event.payload()).findFirst().orElse(null);
        }
    }
}
