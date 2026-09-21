package com.eyecode.language.java.lsp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JdtLsSessionTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void closeIsIdempotentBeforeTheProcessStarts() throws Exception {
        JdtLsSession session = new JdtLsSession(configuration(temporaryDirectory.resolve("workspace"), temporaryDirectory.resolve("data")));

        session.close();
        session.close();

        assertEquals(JdtLsLifecycleState.STOPPED, session.state());
    }

    @Test
    void rejectsDataDirectoryInsideTheControlledWorkspace() throws Exception {
        Path workspace = Files.createDirectories(temporaryDirectory.resolve("workspace"));
        JdtLsSession session = new JdtLsSession(configuration(workspace, workspace.resolve("data")));

        assertThrows(IllegalStateException.class, session::start);
        assertEquals(JdtLsLifecycleState.STOPPED, session.state());
    }

    private JdtLsProcessConfiguration configuration(Path workspace, Path data) throws Exception {
        Path installation = Files.createDirectories(temporaryDirectory.resolve("jdtls"));
        Path plugins = Files.createDirectories(installation.resolve("plugins"));
        Files.createFile(plugins.resolve("org.eclipse.equinox.launcher_1.jar"));
        String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        Files.createDirectories(installation.resolve(os.contains("win") ? "config_win" : os.contains("mac") ? "config_mac" : "config_linux"));
        return JdtLsProcessConfiguration.fromInstallation(JdtLsToolingJava.currentRuntime(Duration.ofSeconds(5)),
                installation, data, Files.createDirectories(workspace));
    }
}
