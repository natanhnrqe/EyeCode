package com.eyecode.terminal;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalServiceTest {

    @Test
    void streamsProcessOutputAndCompletes() throws Exception {
        Path directory = Files.createTempDirectory("eyecode-terminal-service");
        TerminalService service = new TerminalService(path -> List.of("cmd.exe", "/c", "echo terminal-ready"));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<String> output = new AtomicReference<>("");
        service.addListener(new TerminalService.Listener() {
            @Override public void onStarted(Path workingDirectory) { started.countDown(); }
            @Override public void onOutput(String text, boolean error) { output.updateAndGet(value -> value + text); }
            @Override public void onFinished(int exitCode, boolean stopped) { finished.countDown(); }
        });

        assertTrue(service.start(directory));
        assertTrue(started.await(10, TimeUnit.SECONDS));
        assertTrue(finished.await(10, TimeUnit.SECONDS));
        assertTrue(output.get().contains("terminal-ready"));
        service.dispose();
    }
}
