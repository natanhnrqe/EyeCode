package com.eyecode.terminal;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

final class TerminalWebSocketTransport implements AutoCloseable {
    private static final int INITIAL_OUTPUT_LIMIT = 64 * 1024;

    private final String token;
    private final Predicate<byte[]> input;
    private final Server server;
    private final CountDownLatch started = new CountDownLatch(1);
    private final ByteArrayOutputStream initialOutput = new ByteArrayOutputStream();
    private volatile WebSocket connection;

    private TerminalWebSocketTransport(String token, Predicate<byte[]> input) {
        this.token = token;
        this.input = input;
        this.server = new Server(new InetSocketAddress("127.0.0.1", 0));
    }

    static TerminalWebSocketTransport start(String token, Predicate<byte[]> input) {
        TerminalWebSocketTransport transport = new TerminalWebSocketTransport(token, input);
        transport.server.start();
        try {
            if (!transport.started.await(2, TimeUnit.SECONDS)) {
                transport.close();
                throw new IllegalStateException("Terminal transport did not start");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            transport.close();
            throw new IllegalStateException("Terminal transport startup was interrupted", exception);
        }
        return transport;
    }

    String endpoint() {
        return "ws://127.0.0.1:" + server.getPort() + "/terminal?token=" + token;
    }

    synchronized void send(byte[] bytes) {
        WebSocket current = connection;
        if (current != null && current.isOpen()) {
            current.send(bytes);
            return;
        }
        if (bytes == null || bytes.length == 0 || initialOutput.size() >= INITIAL_OUTPUT_LIMIT) {
            return;
        }
        int length = Math.min(bytes.length, INITIAL_OUTPUT_LIMIT - initialOutput.size());
        initialOutput.write(bytes, 0, length);
    }

    @Override
    public void close() {
        WebSocket current = connection;
        connection = null;
        if (current != null) {
            current.close();
        }
        try {
            server.stop(500);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean accepts(ClientHandshake handshake) {
        try {
            URI uri = new URI(handshake.getResourceDescriptor());
            if (!"/terminal".equals(uri.getPath())) {
                return false;
            }
            String query = uri.getQuery();
            return query != null && java.util.Arrays.stream(query.split("&"))
                    .map(part -> part.split("=", 2))
                    .anyMatch(part -> part.length == 2 && "token".equals(part[0]) && Objects.equals(token, part[1]));
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private synchronized void attach(WebSocket candidate) {
        connection = candidate;
        if (initialOutput.size() > 0) {
            candidate.send(initialOutput.toByteArray());
            initialOutput.reset();
        }
    }

    private final class Server extends WebSocketServer {
        private Server(InetSocketAddress address) {
            super(address);
        }

        @Override
        public void onOpen(WebSocket candidate, ClientHandshake handshake) {
            if (!accepts(handshake) || (connection != null && connection.isOpen())) {
                candidate.close(1008, "Unauthorized terminal connection");
                return;
            }
            attach(candidate);
        }

        @Override
        public void onClose(WebSocket candidate, int code, String reason, boolean remote) {
            if (connection == candidate) {
                connection = null;
            }
        }

        @Override
        public void onMessage(WebSocket candidate, String message) {
            candidate.close(1003, "Binary terminal input required");
        }

        @Override
        public void onMessage(WebSocket candidate, ByteBuffer bytes) {
            if (connection != candidate || !candidate.isOpen()) {
                return;
            }
            byte[] inputBytes = new byte[bytes.remaining()];
            bytes.get(inputBytes);
            input.test(inputBytes);
        }

        @Override
        public void onError(WebSocket candidate, Exception exception) {
        }

        @Override
        public void onStart() {
            started.countDown();
        }
    }
}