package com.eyecode.language.java.lsp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdtLsProcessConfigurationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void discoversLauncherAndBuildsExplicitCommand() throws Exception {
        Path installation = Files.createDirectories(temporaryDirectory.resolve("jdtls"));
        Path plugins = Files.createDirectories(installation.resolve("plugins"));
        Files.createFile(plugins.resolve("org.eclipse.equinox.launcher_9.9.9.jar"));
        Files.createDirectories(installation.resolve(platformConfiguration()));
        Path workspace = Files.createDirectories(temporaryDirectory.resolve("workspace"));
        Path data = temporaryDirectory.resolve("data");

        JdtLsProcessConfiguration configuration = JdtLsProcessConfiguration.fromInstallation(
                JdtLsToolingJava.currentRuntime(Duration.ofSeconds(5)), installation, data, workspace);

        assertEquals(plugins.resolve("org.eclipse.equinox.launcher_9.9.9.jar").toAbsolutePath().normalize(), configuration.launcherJar());
        assertTrue(configuration.command().contains("-configuration"));
        assertTrue(configuration.command().contains("-data"));
        assertTrue(configuration.command().contains(data.toAbsolutePath().normalize().toString()));
        assertTrue(configuration.command().contains(workspace.toAbsolutePath().normalize().toString()) == false);
    }

    private static String platformConfiguration() {
        String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        return os.contains("win") ? "config_win" : os.contains("mac") ? "config_mac" : "config_linux";
    }
}
