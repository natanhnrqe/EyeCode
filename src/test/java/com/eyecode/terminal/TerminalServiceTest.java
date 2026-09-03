package com.eyecode.terminal;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalServiceTest {

    @Test
    void startsPtyAndCompletes() throws Exception {
        Path directory = Files.createTempDirectory("eyecode-terminal-service");
        TerminalService service = new TerminalService(path -> List.of("cmd.exe", "/c", "echo terminal-ready"));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        service.addListener(new TerminalService.Listener() {
            @Override public void onStarted(Path workingDirectory) { started.countDown(); }
            @Override public void onFinished(int exitCode, boolean stopped) { finished.countDown(); }
        });

        assertTrue(service.start(directory));
        assertTrue(started.await(10, TimeUnit.SECONDS));
        assertTrue(finished.await(10, TimeUnit.SECONDS));
        service.dispose();
    }
}
