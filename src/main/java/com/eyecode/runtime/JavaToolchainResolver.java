package com.eyecode.runtime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public final class JavaToolchainResolver {

    public static final String JDK_HOME_PROPERTY = "eyecode.jdk.home";
    public static final String JDK_HOME_ENV = "EYECODE_JDK_HOME";
    public static final String STAGED_JDK_RELATIVE = "target/eyecode-desktop/runtime/jdk";

    private final Map<String, String> environment;
    private final Map<String, String> systemProperties;
    private final Path userDirectory;
    private final boolean windows;

    public JavaToolchainResolver() {
        this(System.getenv(), toMap(System.getProperties()),
                Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize(),
                isWindows());
    }

    JavaToolchainResolver(Map<String, String> environment, Map<String, String> systemProperties,
                          Path userDirectory, boolean windows) {
        this.environment = environment == null ? Map.of() : Map.copyOf(environment);
        this.systemProperties = systemProperties == null ? Map.of() : Map.copyOf(systemProperties);
        this.userDirectory = userDirectory == null ? Path.of(".").toAbsolutePath().normalize() : userDirectory;
        this.windows = windows;
    }

    private static Map<String, String> toMap(java.util.Properties properties) {
        Map<String, String> map = new java.util.HashMap<>();
        for (String name : properties.stringPropertyNames()) {
            map.put(name, properties.getProperty(name));
        }
        return map;
    }

    public Optional<JavaToolchain> resolve() {
        Optional<JavaToolchain> explicit = validate(pathFromOverride());
        if (explicit.isPresent()) return explicit;

        Optional<JavaToolchain> staged = validate(stagedJdkHome());
        if (staged.isPresent()) return staged;

        return validate(hostJavaHome());
    }

    public JavaToolchain require() {
        return resolve().orElseThrow(() -> new IllegalArgumentException(
                "No usable Java development toolchain was found.\n"
                        + "Required: a JDK 21 home with bin/java" + exeSuffix() + " and bin/javac" + exeSuffix() + ".\n"
                        + "Options:\n"
                        + "  - set -D" + JDK_HOME_PROPERTY + "=<jdk-home> or EYECODE_JDK_HOME=<jdk-home>\n"
                        + "  - stage the desktop JDK: powershell -ExecutionPolicy Bypass -File scripts/stage-eyecode-jdk.ps1\n"
                        + "  - run EyeCode on a machine with a JDK 21 installation"));
    }

    public Map<String, String> childEnvironment() {
        return resolve().map(JavaToolchainResolver::environmentFor).orElse(Map.of());
    }

    public static Map<String, String> environmentFor(JavaToolchain toolchain) {
        String bin = toolchain.binDirectory().toString();
        String path = System.getenv("PATH");
        String pathValue = path == null || path.isBlank() ? bin : bin + java.io.File.pathSeparator + path;
        return Map.of(
                "JAVA_HOME", toolchain.javaHomeValue(),
                "PATH", pathValue);
    }

    private Path pathFromOverride() {
        String property = trim(systemProperties.get(JDK_HOME_PROPERTY));
        if (property != null) return Path.of(property);
        String env = trim(environment.get(JDK_HOME_ENV));
        if (env != null) return Path.of(env);
        return null;
    }

    private Path stagedJdkHome() {
        return userDirectory.resolve(STAGED_JDK_RELATIVE.replace('/', userDirectory.getFileSystem().getSeparator().charAt(0)));
    }

    private Path hostJavaHome() {
        String home = trim(systemProperties.get("java.home"));
        if (home == null) return null;
        return Path.of(home);
    }

    private Optional<JavaToolchain> validate(Path candidate) {
        if (candidate == null) return Optional.empty();
        Path home = candidate.toAbsolutePath().normalize();
        if (!Files.isDirectory(home)) return Optional.empty();
        Path java = home.resolve("bin").resolve(javaExecutableName());
        Path javac = home.resolve("bin").resolve(javacExecutableName());
        if (!Files.isRegularFile(java) || !Files.isExecutable(java)) return Optional.empty();
        if (!Files.isRegularFile(javac) || !Files.isExecutable(javac)) return Optional.empty();
        return Optional.of(new JavaToolchain(home, java, javac));
    }

    private String javaExecutableName() {
        return windows ? "java.exe" : "java";
    }

    private String javacExecutableName() {
        return windows ? "javac.exe" : "javac";
    }

    private String exeSuffix() {
        return windows ? ".exe" : "";
    }

    private static String trim(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
