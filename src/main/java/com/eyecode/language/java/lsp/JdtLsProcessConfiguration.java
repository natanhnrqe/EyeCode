package com.eyecode.language.java.lsp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public record JdtLsProcessConfiguration(JdtLsToolingJava toolingJava, Path installationRoot,
                                        Path launcherJar, Path configurationDirectory, Path dataDirectory,
                                        Path workspaceDirectory, Map<String, Object> initializationOptions) {
    public JdtLsProcessConfiguration {
        if (toolingJava == null) throw new IllegalArgumentException("JDT LS tooling Java is required");
        installationRoot = requireDirectory(installationRoot, "JDT LS installation root");
        launcherJar = requireFile(launcherJar, "JDT LS Equinox launcher");
        configurationDirectory = requireDirectory(configurationDirectory, "JDT LS platform configuration");
        workspaceDirectory = requireDirectory(workspaceDirectory, "JDT LS workspace");
        dataDirectory = dataDirectory == null ? null : dataDirectory.toAbsolutePath().normalize();
        if (dataDirectory == null) throw new IllegalArgumentException("JDT LS data directory is required");
        initializationOptions = initializationOptions == null ? Map.of() : Map.copyOf(initializationOptions);
    }

    public static JdtLsProcessConfiguration fromInstallation(JdtLsToolingJava toolingJava, Path installationRoot,
                                                              Path dataDirectory, Path workspaceDirectory) {
        Path root = requireDirectory(installationRoot, "JDT LS installation root");
        return new JdtLsProcessConfiguration(toolingJava, root, findLauncher(root), platformConfiguration(root),
                dataDirectory, workspaceDirectory, Map.of());
    }

    public List<String> command() {
        return List.of(
                toolingJava.executable().toString(),
                "-Declipse.application=org.eclipse.jdt.ls.core.id1",
                "-Dosgi.bundles.defaultStartLevel=4",
                "-Declipse.product=org.eclipse.jdt.ls.core.product",
                "-Dlog.level=WARN",
                "-Xmx512m",
                "--add-modules=ALL-SYSTEM",
                "--add-opens", "java.base/java.util=ALL-UNNAMED",
                "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                "-jar", launcherJar.toString(),
                "-configuration", configurationDirectory.toString(),
                "-data", dataDirectory.toString());
    }

    private static Path findLauncher(Path root) {
        Path plugins = root.resolve("plugins");
        try (Stream<Path> files = Files.list(plugins)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("org.eclipse.equinox.launcher_"))
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .min(Comparator.comparing(path -> path.getFileName().toString()))
                    .orElseThrow(() -> new IllegalArgumentException("JDT LS Equinox launcher was not found in " + plugins));
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to inspect JDT LS plugins directory: " + plugins, exception);
        }
    }

    private static Path platformConfiguration(Path root) {
        String name = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win")
                ? "config_win" : System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("mac")
                ? "config_mac" : "config_linux";
        return requireDirectory(root.resolve(name), "JDT LS platform configuration");
    }

    private static Path requireDirectory(Path path, String label) {
        if (path == null || !Files.isDirectory(path)) {
            throw new IllegalArgumentException(label + " does not exist: " + path);
        }
        return path.toAbsolutePath().normalize();
    }

    private static Path requireFile(Path path, String label) {
        if (path == null || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException(label + " does not exist: " + path);
        }
        return path.toAbsolutePath().normalize();
    }
}
