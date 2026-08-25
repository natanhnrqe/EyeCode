package com.eyecode.filesystem;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalFileWatcherTest {

    @Test
    void recognizesOnlyEyeCodeAtomicSaveArtifacts() {
        assertTrue(ExternalFileWatcher.isInternalAtomicSaveArtifact(
                Path.of("C:/project/.eyecode-save-123.tmp")));
        assertFalse(ExternalFileWatcher.isInternalAtomicSaveArtifact(
                Path.of("C:/project/Main.java.tmp")));
        assertFalse(ExternalFileWatcher.isInternalAtomicSaveArtifact(
                Path.of("C:/project/.user-save-123.tmp")));
    }

    @Test
    void watchesExternalFileAndClosesWithoutJavaFx() throws Exception {
        Path root = Files.createTempDirectory("eyecode-watch");
        Path file = root.resolve("Main.java");
        Files.writeString(file, "class Main {}\n");
        CountDownLatch changed = new CountDownLatch(1);
        AtomicReference<Path> observed = new AtomicReference<>();

        ExternalFileWatcher watcher = new ExternalFileWatcher();
        watcher.addListener(path -> {
            observed.set(path);
            changed.countDown();
        });
        watcher.watchRoot(root);
        Files.writeString(file, "class Main { int value; }\n");

        assertTrue(changed.await(2, TimeUnit.SECONDS));
        assertEquals(file.toAbsolutePath().normalize(), observed.get());
        watcher.close();
    }
}
