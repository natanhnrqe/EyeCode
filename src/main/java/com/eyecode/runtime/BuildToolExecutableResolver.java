package com.eyecode.runtime;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class BuildToolExecutableResolver {

    enum Tool {
        MAVEN("Maven", List.of("mvnw.cmd", "mvnw"), List.of("mvn.cmd", "mvn.bat", "mvn.exe", "mvn")),
        GRADLE("Gradle", List.of("gradlew.bat", "gradlew"), List.of("gradle.bat", "gradle.cmd", "gradle.exe", "gradle"));

        private final String displayName;
        private final List<String> wrapperNames;
        private final List<String> systemNames;

        Tool(String displayName, List<String> wrapperNames, List<String> systemNames) {
            this.displayName = displayName;
            this.wrapperNames = wrapperNames;
            this.systemNames = systemNames;
        }
    }

    private final Map<String, String> environment;
    private final boolean windows;

    BuildToolExecutableResolver() {
        this(System.getenv(), System.getProperty("os.name", ""));
    }

    BuildToolExecutableResolver(Map<String, String> environment, String operatingSystem) {
        this.environment = environment == null ? Map.of() : Map.copyOf(environment);
        this.windows = operatingSystem != null && operatingSystem.toLowerCase(Locale.ROOT).contains("win");
    }

    List<String> mavenCommand(Path projectRoot, String... arguments) {
        return command(Tool.MAVEN, projectRoot, arguments);
    }

    List<String> gradleCommand(Path projectRoot, String... arguments) {
        return command(Tool.GRADLE, projectRoot, arguments);
    }

    private List<String> command(Tool tool, Path projectRoot, String... arguments) {
        Path executable = wrapper(tool, projectRoot);
        if (executable == null) {
            executable = systemExecutable(tool);
        }
        if (executable == null) {
            throw new IllegalArgumentException(tool.displayName + " could not be found. Add "
                    + tool.displayName + " to PATH or use the " + tool.displayName + " Wrapper ("
                    + wrapperHint(tool) + ") in this project.");
        }
        List<String> command = new ArrayList<>();
        if (windows && isCommandScript(executable)) {
            command.add("cmd");
            command.add("/c");
        }
        command.add(executable.toString());
        command.addAll(List.of(arguments));
        return command;
    }

    private Path wrapper(Tool tool, Path projectRoot) {
        if (projectRoot == null) {
            return null;
        }
        Path root = projectRoot.toAbsolutePath().normalize();
        for (String name : tool.wrapperNames) {
            Path candidate = root.resolve(name);
            if (Files.isRegularFile(candidate) && (windows || Files.isExecutable(candidate))) {
                return candidate;
            }
        }
        return null;
    }

    private Path systemExecutable(Tool tool) {
        String path = environment.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase("PATH"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("");
        if (path.isBlank()) {
            return null;
        }
        for (String entry : path.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            Path directory;
            try {
                directory = Path.of(entry.trim());
            } catch (RuntimeException ignored) {
                continue;
            }
            for (String name : tool.systemNames) {
                Path candidate = directory.resolve(name);
                if (Files.isRegularFile(candidate) && (windows || Files.isExecutable(candidate))) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private boolean isCommandScript(Path executable) {
        String name = executable.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".cmd") || name.endsWith(".bat");
    }

    private String wrapperHint(Tool tool) {
        if (windows) {
            return tool.wrapperNames.getFirst();
        }
        return tool.wrapperNames.getLast();
    }
}