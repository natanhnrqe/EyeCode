package com.eyecode.terminal;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TerminalServiceTest {

    @Test
    void startsPtyAndCompletes() throws Exception {
        Path directory = Files.createTempDirectory("eyecode-terminal-service");
        TerminalService service = new TerminalService(path -> List.of("cmd.exe", "/c", "echo terminal-ready"));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger exitCode = new java.util.concurrent.atomic.AtomicInteger(Integer.MIN_VALUE);
        java.util.concurrent.atomic.AtomicBoolean stopped = new java.util.concurrent.atomic.AtomicBoolean(true);
        service.addListener(new TerminalService.Listener() {
            @Override public void onStarted(Path workingDirectory) { started.countDown(); }
            @Override public void onOutput(String text, boolean error) { }
            @Override public void onFinished(int code, boolean wasStopped) {
                exitCode.set(code);
                stopped.set(wasStopped);
                finished.countDown();
            }
        });

        assertTrue(service.start(directory));
        assertTrue(started.await(10, TimeUnit.SECONDS));
        assertTrue(finished.await(10, TimeUnit.SECONDS));
        assertEquals(0, exitCode.get());
        assertFalse(stopped.get());
        service.dispose();
    }
}
