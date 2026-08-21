package com.eyecode.language.documentation;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class JdkSourceLoaderTest {

    @Test
    void loadsUtf8EntryAndClosesArchive() throws Exception {
        Path zip = Files.createTempFile("eyecode-jdk", ".zip");
        try {
            try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip))) {
                output.putNextEntry(new ZipEntry("java.base/java/lang/String.java"));
                output.write("class String { String café; }".getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
            JdkSourceTarget target = new JdkSourceTarget(
                    "java.lang.String", "java.base",
                    "java.base/java/lang/String.java", "String.java");
            JdkSourceLoader loader = new JdkSourceLoader(zip);
            assertEquals("class String { String café; }", loader.load(target).orElseThrow());
            assertEquals(loader.load(target), loader.load(target));
            assertTrue(loader.load(new JdkSourceTarget(
                    "java.lang.Object", "java.base",
                    "java.base/java/lang/Object.java", "Object.java")).isEmpty());
        } finally {
            Files.deleteIfExists(zip);
        }
    }

    @Test
    void missingZipFailsSafely() {
        JdkSourceLoader loader = new JdkSourceLoader(Path.of("missing-src.zip"));
        assertTrue(loader.load(new JdkSourceTarget(
                "java.lang.String", "java.base",
                "java.base/java/lang/String.java", "String.java")).isEmpty());
    }
}
