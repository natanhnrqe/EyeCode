package com.eyecode.filesystem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DefaultFileSystemServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void atomicSaveLeavesOnlyTheRealProjectFile() throws Exception {
        Path file = tempDir.resolve("Main.java");
        new DefaultFileSystemService().writeFile(file, "class Main {}\n");

        assertEquals("class Main {}\n", Files.readString(file));
        try (var files = Files.list(tempDir)) {
            assertFalse(files.anyMatch(path -> ExternalFileWatcher.isInternalAtomicSaveArtifact(path)));
        }
    }
}
