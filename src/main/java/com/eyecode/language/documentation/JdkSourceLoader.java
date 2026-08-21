package com.eyecode.language.documentation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.ZipFile;

public final class JdkSourceLoader {

    private final Path sourceZip;

    public JdkSourceLoader() {
        this(locateSourceZip().orElse(null));
    }

    public JdkSourceLoader(Path sourceZip) {
        this.sourceZip = sourceZip;
    }

    public Optional<String> load(JdkSourceTarget target) {
        if (sourceZip == null || target == null || !Files.isRegularFile(sourceZip)) {
            return Optional.empty();
        }
        try (ZipFile zip = new ZipFile(sourceZip.toFile())) {
            var entry = zip.getEntry(target.sourceEntryPath());
            if (entry == null) {
                return Optional.empty();
            }
            try (var stream = zip.getInputStream(entry)) {
                return Optional.of(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (IOException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public Optional<Path> sourceZip() {
        return sourceZip != null && Files.isRegularFile(sourceZip)
                ? Optional.of(sourceZip) : Optional.empty();
    }

    static Optional<Path> locateSourceZip() {
        String javaHome = System.getProperty("java.home");
        if (javaHome == null || javaHome.isBlank()) {
            return Optional.empty();
        }
        Path home = Path.of(javaHome);
        Path direct = home.resolve("lib").resolve("src.zip");
        if (Files.isRegularFile(direct)) {
            return Optional.of(direct);
        }
        Path parent = home.getParent();
        if (parent != null) {
            Path sibling = parent.resolve("lib").resolve("src.zip");
            if (Files.isRegularFile(sibling)) {
                return Optional.of(sibling);
            }
        }
        return Optional.empty();
    }
}
