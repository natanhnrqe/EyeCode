package com.eyecode.project;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ProjectLaunchPathResolver {
    private ProjectLaunchPathResolver() {
    }

    public static Path resolve(String[] args) {
        if (args == null || args.length == 0 || args[0] == null || args[0].isBlank()) return null;
        Path requested = Path.of(args[0]).toAbsolutePath().normalize();
        if (Files.isDirectory(requested)) return requested;
        Path candidate = requested.getParent();
        Path detectedFallback = null;
        while (candidate != null) {
            if (Files.isDirectory(candidate)) {
                if (hasProjectRootMarker(candidate)) return candidate;
                if (detectedFallback == null && ProjectDetector.detect(candidate.toFile()) != ProjectType.UNKNOWN) {
                    detectedFallback = candidate;
                }
            }
            candidate = candidate.getParent();
        }
        return detectedFallback == null ? requested : detectedFallback;
    }

    private static boolean hasProjectRootMarker(Path directory) {
        return Files.exists(directory.resolve("pom.xml"))
                || Files.exists(directory.resolve("build.gradle"))
                || Files.exists(directory.resolve("build.gradle.kts"))
                || Files.exists(directory.resolve("settings.gradle"))
                || Files.exists(directory.resolve("settings.gradle.kts"))
                || Files.exists(directory.resolve("gradlew"))
                || Files.exists(directory.resolve(".git"))
                || Files.isDirectory(directory.resolve("src"));
    }
}
