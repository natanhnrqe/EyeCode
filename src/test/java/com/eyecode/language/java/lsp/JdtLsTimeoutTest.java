package com.eyecode.language.java.lsp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTimeout;

final class JdtLsTimeoutTest {
    @TempDir Path temporary;

    @Test
    void unansweredRequestHasABoundedTimeout() throws Exception {
        Path installation = Files.createDirectories(temporary.resolve("jdtls"));
        Path plugins = Files.createDirectories(installation.resolve("plugins"));
        Files.createFile(plugins.resolve("org.eclipse.equinox.launcher_1.jar"));
        String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        Files.createDirectories(installation.resolve(os.contains("win") ? "config_win" : os.contains("mac") ? "config_mac" : "config_linux"));
        JdtLsSession session = new JdtLsSession(JdtLsProcessConfiguration.fromInstallation(
                JdtLsToolingJava.currentRuntime(Duration.ofSeconds(5)), installation,
                temporary.resolve("data"), Files.createDirectories(temporary.resolve("workspace"))));
        assertTimeout(Duration.ofSeconds(2), () -> {
            Method await = JdtLsSession.class.getDeclaredMethod("await", CompletableFuture.class,
                    Duration.class, String.class);
            await.setAccessible(true);
            InvocationTargetException failure = org.junit.jupiter.api.Assertions.assertThrows(
                    InvocationTargetException.class,
                    () -> await.invoke(session, new CompletableFuture<>(), Duration.ofMillis(50), "TEST"));
            assertInstanceOf(IllegalStateException.class, failure.getCause());
        });
    }
}
