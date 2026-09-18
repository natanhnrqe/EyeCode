package com.eyecode.ui.web;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalWebShellRuntimeTest {
    private static final Pattern SOCKET_URL = Pattern.compile("webSocketUrl:\"([^\"]+)\"");

    @Test
    void bundledRuntimeServesBootstrapAndDispatchesTheSharedWebProtocol() throws Exception {
        try (LocalWebShellSurface surface = new LocalWebShellSurface()) {
            WebShellWorkspaceController workspace = new WebShellWorkspaceController(surface);
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
                workspace.dispose();
            }
        }
    }

    @Test
    void webRuntimeReportsNativePickerOperationsAsUnavailable() {
        CapturingSurface surface = new CapturingSurface();
        WebShellWorkspaceController workspace = new WebShellWorkspaceController(surface);
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
            workspace.dispose();
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

        @Override public void send(WebShellEnvelope message) { }

        @Override
        public void registerHandler(String channel, String name, WebShellMessageHandler handler) {
            handlers.put(channel + "/" + name, handler);
        }

        private WebShellMessageHandler handler(String channel, String name) {
            return handlers.get(channel + "/" + name);
        }
    }
}
