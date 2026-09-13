package com.eyecode.javafx.web;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class LocalWebShellSurface implements WebShellSurface, AutoCloseable {
    private static final String SOCKET_PATH = "/webshell-bridge";

    private final WebShellProtocolCodec codec = new WebShellProtocolCodec();
    private final WebShellDispatcher dispatcher = new WebShellDispatcher();
    private final String sessionToken = sessionToken();
    private final WebShellAssetServer assetServer;
    private final Server socketServer;
    private final String expectedOrigin;
    private final CountDownLatch started = new CountDownLatch(1);
    private final Object connectionLock = new Object();
    private final Object dispatchLock = new Object();
    private volatile WebSocket connection;
    private volatile boolean closed;

    public LocalWebShellSurface() {
        assetServer = WebShellAssetServer.start(this::bootstrapScript);
        expectedOrigin = assetServer.baseUrl();
        socketServer = new Server(loopbackAddress());
        socketServer.start();
        awaitStartup();
        registerShellHandlers();
        System.out.println("[LOCAL-WEBSHELL] websocket " + socketUrl());
    }

    public String entryUrl() {
        return assetServer.entryUrl();
    }

    @Override
    public void registerHandler(String channel, String name, WebShellMessageHandler handler) {
        if (!closed) {
            dispatcher.register(channel, name, handler);
        }
    }

    @Override
    public void send(WebShellEnvelope message) {
        if (message == null || closed) {
            return;
        }
        synchronized (dispatchLock) {
            WebSocket active;
            synchronized (connectionLock) {
                active = connection;
            }
            if (active != null && active.isOpen()) {
                active.send(codec.encode(message));
            }
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        synchronized (connectionLock) {
            if (connection != null) {
                connection.close();
                connection = null;
            }
        }
        try {
            socketServer.stop(500);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        assetServer.close();
    }

    private void registerShellHandlers() {
        dispatcher.register("shell", "ping", message -> message.response(Map.of("message", "pong")));
        dispatcher.register("shell", "ready", message -> {
            send(WebShellEnvelope.event("shell", "bootstrap", Map.of(
                    "protocolVersion", WebShellEnvelope.PROTOCOL,
                    "platform", System.getProperty("os.name", "unknown"),
                    "webShellMode", WebShellMode.WEB_SHELL.name())));
            return message.response(Map.of("accepted", true));
        });
    }

    private String bootstrapScript() {
        return "window.__EYECODE_LOCAL_TRANSPORT__={webSocketUrl:" + json(socketUrl())
                + ",token:" + json(sessionToken) + "};";
    }

    private String socketUrl() {
        return "ws://127.0.0.1:" + socketServer.getPort() + SOCKET_PATH + "?token=" + sessionToken;
    }

    private boolean accepts(ClientHandshake handshake) {
        if (!Objects.equals(expectedOrigin, handshake.getFieldValue("Origin"))) {
            return false;
        }
        try {
            URI uri = new URI(handshake.getResourceDescriptor());
            if (!SOCKET_PATH.equals(uri.getPath()) || uri.getQuery() == null) {
                return false;
            }
            return java.util.Arrays.stream(uri.getQuery().split("&"))
                    .map(part -> part.split("=", 2))
                    .anyMatch(part -> part.length == 2 && "token".equals(part[0])
                            && sessionToken.equals(part[1]));
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private void dispatch(WebSocket source, String json) {
        WebShellEnvelope message;
        try {
            message = codec.decode(json);
        } catch (IllegalArgumentException exception) {
            source.close(1007, "Invalid WebShell envelope");
            return;
        }
        synchronized (dispatchLock) {
            try {
                WebShellEnvelope response = dispatcher.dispatch(message);
                if (response != null && source.isOpen()) {
                    source.send(codec.encode(response));
                }
            } catch (RuntimeException exception) {
                if (source.isOpen()) {
                    source.send(codec.encode(message.error(new WebShellError("WEBSHELL_DISPATCH_FAILED",
                            exception.getMessage() == null ? "WebShell dispatch failed" : exception.getMessage(), true))));
                }
            }
        }
    }

    private void awaitStartup() {
        try {
            if (!started.await(2, TimeUnit.SECONDS)) {
                close();
                throw new IllegalStateException("Local WebShell WebSocket did not start");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            close();
            throw new IllegalStateException("Local WebShell startup was interrupted", exception);
        }
    }

    private static InetSocketAddress loopbackAddress() {
        try {
            return new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0);
        } catch (java.net.UnknownHostException exception) {
            throw new IllegalStateException("Loopback address is unavailable", exception);
        }
    }

    private static String sessionToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String json(String value) {
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private final class Server extends WebSocketServer {
        private Server(InetSocketAddress address) {
            super(address);
        }

        @Override
        public void onOpen(WebSocket candidate, ClientHandshake handshake) {
            synchronized (connectionLock) {
                if (!accepts(handshake) || (connection != null && connection.isOpen())) {
                    candidate.close(1008, "Unauthorized WebShell connection");
                    return;
                }
                connection = candidate;
            }
            System.out.println("[LOCAL-WEBSHELL] connected");
        }

        @Override
        public void onClose(WebSocket candidate, int code, String reason, boolean remote) {
            synchronized (connectionLock) {
                if (connection == candidate) {
                    connection = null;
                }
            }
        }

        @Override
        public void onMessage(WebSocket candidate, String message) {
            synchronized (connectionLock) {
                if (connection != candidate || !candidate.isOpen()) {
                    return;
                }
            }
            dispatch(candidate, message);
        }

        @Override
        public void onError(WebSocket candidate, Exception exception) {
            if (!closed) {
                System.err.println("[LOCAL-WEBSHELL] websocket error: " + exception.getMessage());
            }
        }

        @Override
        public void onStart() {
            started.countDown();
        }
    }
}
