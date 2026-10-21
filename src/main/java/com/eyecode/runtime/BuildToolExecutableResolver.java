package com.eyecode.runtime;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BuildToolExecutableResolver {

    public static final String MAVEN_HOME_PROPERTY = "eyecode.maven.home";
    public static final String MAVEN_HOME_ENV = "EYECODE_MAVEN_HOME";
    public static final String STAGED_MAVEN_RELATIVE = "target/eyecode-desktop/runtime/maven";

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
    private final Map<String, String> systemProperties;
    private final Path userDirectory;
    private final boolean windows;

    public BuildToolExecutableResolver() {
        this(System.getenv(), System.getProperty("os.name", ""),
                Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize(),
                toMap(System.getProperties()));
    }

    BuildToolExecutableResolver(Map<String, String> environment, String operatingSystem) {
        this(environment, operatingSystem,
                Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize(),
                Map.of());
    }

    public BuildToolExecutableResolver(Map<String, String> environment, String operatingSystem, Path userDirectory) {
        this(environment, operatingSystem, userDirectory, Map.of());
    }

    public BuildToolExecutableResolver(Map<String, String> environment, String operatingSystem,
                                       Path userDirectory, Map<String, String> systemProperties) {
        this.environment = environment == null ? Map.of() : Map.copyOf(environment);
        this.systemProperties = systemProperties == null ? Map.of() : Map.copyOf(systemProperties);
        this.userDirectory = userDirectory == null
                ? Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize()
                : userDirectory;
        this.windows = operatingSystem != null && operatingSystem.toLowerCase(Locale.ROOT).contains("win");
    }

    private static Map<String, String> toMap(java.util.Properties properties) {
        Map<String, String> map = new HashMap<>();
        for (String name : properties.stringPropertyNames()) {
            map.put(name, properties.getProperty(name));
        }
        return map;
    }

    public List<String> mavenCommand(Path projectRoot, String... arguments) {
        return command(Tool.MAVEN, projectRoot, arguments);
    }

    public List<String> gradleCommand(Path projectRoot, String... arguments) {
        return command(Tool.GRADLE, projectRoot, arguments);
    }

    private List<String> command(Tool tool, Path projectRoot, String... arguments) {
        Path executable = wrapper(tool, projectRoot);
        if (executable == null && tool == Tool.MAVEN) {
            executable = overrideExecutable();
            if (executable == null) {
                executable = bundledExecutable();
            }
        }
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

    private Path overrideExecutable() {
        String property = trim(systemProperties.get(MAVEN_HOME_PROPERTY));
        if (property != null) {
            Path executable = mavenExecutableIn(Path.of(property));
            if (executable != null) {
                return executable;
            }
        }
        String env = trim(environment.get(MAVEN_HOME_ENV));
        if (env != null) {
            return mavenExecutableIn(Path.of(env));
        }
        return null;
    }

    private Path bundledExecutable() {
        String separator = String.valueOf(userDirectory.getFileSystem().getSeparator());
        return mavenExecutableIn(userDirectory.resolve(STAGED_MAVEN_RELATIVE.replace("/", separator)));
    }

    private Path mavenExecutableIn(Path home) {
        if (home == null) {
            return null;
        }
        Path bin = home.toAbsolutePath().normalize().resolve("bin");
        String name = windows ? "mvn.cmd" : "mvn";
        Path candidate = bin.resolve(name);
        if (Files.isRegularFile(candidate) && (windows || Files.isExecutable(candidate))) {
            return candidate;
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

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
