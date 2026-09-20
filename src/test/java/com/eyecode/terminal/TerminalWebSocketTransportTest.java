package com.eyecode.terminal;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalWebSocketTransportTest {

    @Test
    void relaysBufferedOutputAndBinaryInputForAuthorizedConnection() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch outputReceived = new CountDownLatch(1);
        CountDownLatch inputReceived = new CountDownLatch(1);
        AtomicReference<byte[]> input = new AtomicReference<>();
        AtomicReference<byte[]> output = new AtomicReference<>();
        TerminalWebSocketTransport transport = TerminalWebSocketTransport.start("terminal-test-token", bytes -> {
            input.set(bytes);
            inputReceived.countDown();
            return true;
        }, connected::countDown);
        WebSocketClient client = new WebSocketClient(URI.create(transport.endpoint())) {
            @Override
            public void onOpen(ServerHandshake handshake) {
            }

            @Override
            public void onMessage(String message) {
            }

            @Override
            public void onMessage(ByteBuffer bytes) {
                byte[] value = new byte[bytes.remaining()];
                bytes.get(value);
                output.set(value);
                outputReceived.countDown();
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
            }

            @Override
            public void onError(Exception exception) {
            }
        };
        try {
            transport.send("ready".getBytes(StandardCharsets.UTF_8));
            assertTrue(client.connectBlocking(5, TimeUnit.SECONDS));
            assertTrue(connected.await(5, TimeUnit.SECONDS));
            assertTrue(outputReceived.await(5, TimeUnit.SECONDS));
            assertArrayEquals("ready".getBytes(StandardCharsets.UTF_8), output.get());

            client.send("input".getBytes(StandardCharsets.UTF_8));
            assertTrue(inputReceived.await(5, TimeUnit.SECONDS));
            assertArrayEquals("input".getBytes(StandardCharsets.UTF_8), input.get());
        } finally {
            client.close();
            transport.close();
        }
    }
}
