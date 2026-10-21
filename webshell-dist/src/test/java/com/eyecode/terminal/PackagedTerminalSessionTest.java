package com.eyecode.terminal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackagedTerminalSessionTest {

    @Test
    @Timeout(30)
    void packagedPtySessionStartsAndStreamsOutput() throws Exception {
        Path workDir = Files.createTempDirectory("eyecode-cp1-terminal");
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicInteger exitCode = new AtomicInteger(-999);
        StringBuilder collected = new StringBuilder();

        List<String> command = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? List.of("cmd.exe", "/c", "echo CP1-TERMINAL-OK")
                : List.of("sh", "-c", "echo CP1-TERMINAL-OK");

        TerminalSession session = new TerminalSession("cp1", workDir, command, new TerminalSession.Listener() {
            @Override
            public void onStarted(Path workingDirectory) {
                started.countDown();
            }

            @Override
            public void onOutput(byte[] bytes) {
                synchronized (collected) {
                    collected.append(new String(bytes));
                }
            }

            @Override
            public void onFinished(int code, boolean stopped) {
                exitCode.set(code);
                finished.countDown();
            }
        });
        try {
            session.start();
            assertTrue(started.await(15, TimeUnit.SECONDS), "pty session should start");
            assertTrue(session.isRunning());
            assertTrue(finished.await(15, TimeUnit.SECONDS), "pty session should finish");
            assertEquals(0, exitCode.get(), "echo command should exit 0");
            String text = collected.toString();
            assertTrue(text.contains("CP1-TERMINAL-OK"),
                    "pty output should contain the marker, got: " + text.replace("\r", "\\r").replace("\n", "\\n"));
        } finally {
            session.dispose();
            Files.walk(workDir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> path.toFile().delete());
        }
    }

    @Test
    @Timeout(20)
    void packagedTerminalServiceExposesLoopbackWsEndpoint() throws Exception {
        Path workDir = Files.createTempDirectory("eyecode-cp1-terminal-service");
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicInteger exitCode = new AtomicInteger(-999);

        List<String> command = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? List.of("cmd.exe", "/c", "echo service-ok")
                : List.of("sh", "-c", "echo service-ok");

        TerminalService service = new TerminalService(directory -> command);
        try {
            service.addListener(new TerminalService.Listener() {
                @Override
                public void onStarted(Path workingDirectory) {
                    started.countDown();
                }

                @Override
                public void onOutput(String text, boolean error) {
                }

                @Override
                public void onFinished(int code, boolean stopped) {
                    exitCode.set(code);
                    finished.countDown();
                }
            });

            assertTrue(service.start(workDir));
            assertTrue(started.await(15, TimeUnit.SECONDS), "terminal service session should start");
            assertTrue(service.isRunning());

            TerminalService.Status status = service.status();
            assertNotNull(status.sessionId());
            assertFalse(status.sessionId().isBlank(), "session id should be assigned");
            assertNotNull(status.endpoint());
            assertTrue(status.endpoint().startsWith("ws://127.0.0.1:"), "terminal WS endpoint should be loopback");
            assertTrue(status.endpoint().contains("/terminal?token="), "terminal WS endpoint should carry a token");

            assertTrue(finished.await(15, TimeUnit.SECONDS), "terminal service session should finish");
            assertEquals(0, exitCode.get(), "echo command should exit 0");
        } finally {
            service.dispose();
            Files.walk(workDir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> path.toFile().delete());
        }
    }

    @Test
    @Timeout(15)
    void disposeIsIdempotentAndStopsSession() throws Exception {
        Path workDir = Files.createTempDirectory("eyecode-cp1-terminal-dispose");
        List<String> command = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? List.of("cmd.exe", "/c", "echo disposable")
                : List.of("sh", "-c", "echo disposable");
        TerminalService service = new TerminalService(directory -> command);
        try {
            service.start(workDir);
            Thread.sleep(500);
            service.dispose();
            service.dispose();
            assertFalse(service.isRunning());
        } finally {
            Files.walk(workDir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> path.toFile().delete());
        }
    }
}
