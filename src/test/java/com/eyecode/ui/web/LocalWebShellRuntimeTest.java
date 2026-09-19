package com.eyecode.ui.web;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalWebShellRuntimeTest {
    private static final Pattern SOCKET_URL = Pattern.compile("webSocketUrl:\"([^\"]+)\"");

    @TempDir
    Path tempDir;

    @Test
    void surfaceClosesSocketOutsideConnectionLock() throws Exception {
        ExecutorService callbacks = Executors.newSingleThreadExecutor();
        try (LocalWebShellSurface surface = new LocalWebShellSurface()) {
            var lockField = LocalWebShellSurface.class.getDeclaredField("connectionLock");
            lockField.setAccessible(true);
            Object lock = lockField.get(surface);
            var connectionField = LocalWebShellSurface.class.getDeclaredField("connection");
            connectionField.setAccessible(true);
            java.util.concurrent.atomic.AtomicBoolean closeCalled = new java.util.concurrent.atomic.AtomicBoolean();
            CountDownLatch callbackStarted = new CountDownLatch(1);
            CountDownLatch callbackAcquiredConnectionLock = new CountDownLatch(1);
            Object socket = java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{org.java_websocket.WebSocket.class}, (proxy, method, args) -> {
                        if (method.getName().equals("close")) {
                            closeCalled.set(true);
                            var callback = callbacks.submit(() -> {
                                callbackStarted.countDown();
                                synchronized (lock) {
                                    callbackAcquiredConnectionLock.countDown();
                                }
                            });
                            assertTrue(callbackStarted.await(1, TimeUnit.SECONDS));
                            assertTrue(callbackAcquiredConnectionLock.await(1, TimeUnit.SECONDS),
                                    "Socket close callbacks must be able to acquire connectionLock");
                            callback.get(1, TimeUnit.SECONDS);
                            org.junit.jupiter.api.Assertions.assertFalse(Thread.holdsLock(lock),
                                    "Socket close callbacks must be able to acquire connectionLock");
                            assertNull(connectionField.get(surface));
                        }
                        return null;
                    });
            connectionField.set(surface, socket);
            surface.close();
            assertTrue(closeCalled.get());
        } finally {
            callbacks.shutdownNow();
        }
    }

    @Test
    void bundledRuntimeServesBootstrapAndDispatchesTheSharedWebProtocol() throws Exception {
        try (LocalWebShellSurface surface = new LocalWebShellSurface()) {
            WebShellWorkspaceRuntime workspace = WebShellWorkspaceComposition.create(surface);
            try {
                HttpResponse<String> entry = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                        URI.create(surface.entryUrl())).build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(200, entry.statusCode());
                assertTrue(entry.body().contains("window.__EYECODE_LOCAL_TRANSPORT__"));
                Matcher matcher = SOCKET_URL.matcher(entry.body());
                assertTrue(matcher.find());

                AtomicReference<String> received = new AtomicReference<>();
                CountDownLatch response = new CountDownLatch(1);
                WebSocketClient client = new WebSocketClient(URI.create(matcher.group(1))) {
                    @Override public void onOpen(ServerHandshake handshake) { }
                    @Override public void onMessage(String message) {
                        received.set(message);
                        response.countDown();
                    }
                    @Override public void onClose(int code, String reason, boolean remote) { }
                    @Override public void onError(Exception exception) { }
                };
                try {
                    client.addHeader("Origin", surface.backendUrl());
                    assertTrue(client.connectBlocking(2, TimeUnit.SECONDS));
                    client.send(new WebShellProtocolCodec().encode(
                            WebShellEnvelope.request("shell", "ping", "ping-1", Map.of())));
                    assertTrue(response.await(2, TimeUnit.SECONDS));
                    WebShellEnvelope envelope = new WebShellProtocolCodec().decode(received.get());
                    assertEquals("ping-1", envelope.requestId());
                    assertEquals("pong", envelope.payload().get("message"));
                } finally {
                    client.closeBlocking();
                }
            } finally {
                workspace.close();
            }
        }
    }

    @Test
    void webRuntimeReportsNativePickerOperationsAsUnavailable() {
        CapturingSurface surface = new CapturingSurface();
        WebShellWorkspaceRuntime workspace = WebShellWorkspaceComposition.create(surface);
        try {
            WebShellEnvelope openProject = surface.handler("workspace", "openProject").handle(
                    WebShellEnvelope.request("workspace", "openProject", "project-1", Map.of()));
            WebShellEnvelope chooseDirectory = surface.handler("workspace", "chooseDirectory").handle(
                    WebShellEnvelope.request("workspace", "chooseDirectory", "directory-1", Map.of()));
            WebShellEnvelope created = surface.handler("document", "new").handle(
                    WebShellEnvelope.request("document", "new", "document-1", Map.of("content", "class Main {}")));
            @SuppressWarnings("unchecked")
            Map<String, Object> document = (Map<String, Object>) created.payload().get("document");
            WebShellEnvelope save = surface.handler("document", "save").handle(
                    WebShellEnvelope.request("document", "save", "save-1", Map.of("uri", document.get("uri"))));

            assertNativeUiUnavailable(openProject);
            assertNativeUiUnavailable(chooseDirectory);
            assertNativeUiUnavailable(save);
        } finally {
            workspace.close();
        }
    }

    @Test
    void workspaceUsesTheFileSelectionPortWithoutKnowingTheNativeAdapter() throws Exception {
        Path projectRoot = Files.createDirectories(tempDir.resolve("project/src"));
        Files.writeString(projectRoot.resolve("Main.java"), "class Main {}\n");
        CapturingSurface surface = new CapturingSurface();
        WebShellNativeUi selection = new WebShellNativeUi() {
            @Override public boolean isAvailable() { return true; }
            @Override public CompletableFuture<Path> chooseDirectoryAsync(String title) {
                return CompletableFuture.completedFuture(projectRoot.getParent());
            }
            @Override public Path chooseJavaSaveTarget(String suggestedName) { return null; }
        };
        WebShellWorkspaceRuntime workspace = WebShellWorkspaceComposition.create(surface, target -> { }, selection);
        try {
            WebShellEnvelope immediate = surface.handler("workspace", "openProject").handle(
                    WebShellEnvelope.request("workspace", "openProject", "project-1", Map.of()));

            assertNull(immediate);
            assertTrue(surface.response.await(2, TimeUnit.SECONDS));
            assertTrue(surface.sent.stream().anyMatch(message -> message.kind() == WebShellEnvelope.Kind.RESPONSE
                    && "project-1".equals(message.requestId()) && message.error() == null));
        } finally {
            workspace.close();
        }
    }

    private static void assertNativeUiUnavailable(WebShellEnvelope response) {
        assertNotNull(response);
        assertNotNull(response.error());
        assertEquals("NATIVE_UI_UNAVAILABLE", response.error().code());
        assertTrue(response.error().recoverable());
    }

    private static final class CapturingSurface implements WebShellSurface {
        private final Map<String, WebShellMessageHandler> handlers = new java.util.concurrent.ConcurrentHashMap<>();
        private final java.util.List<WebShellEnvelope> sent = new java.util.concurrent.CopyOnWriteArrayList<>();
        private final CountDownLatch response = new CountDownLatch(1);

        @Override
        public void send(WebShellEnvelope message) {
            sent.add(message);
            if (message != null && message.kind() == WebShellEnvelope.Kind.RESPONSE) {
                response.countDown();
            }
        }

        @Override
        public void registerHandler(String channel, String name, WebShellMessageHandler handler) {
            handlers.put(channel + "/" + name, handler);
        }

        private WebShellMessageHandler handler(String channel, String name) {
            return handlers.get(channel + "/" + name);
        }
    }
}
