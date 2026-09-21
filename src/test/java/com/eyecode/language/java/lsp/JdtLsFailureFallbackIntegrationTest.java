package com.eyecode.language.java.lsp;

import com.eyecode.language.LanguageDocument;
import com.eyecode.language.LanguageId;
import com.eyecode.language.completion.CompletionRequest;
import com.eyecode.language.completion.CompletionResult;
import com.eyecode.language.java.completion.JavaCompletionProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "eyecode.jdtls.integration", matches = "true")
final class JdtLsFailureFallbackIntegrationTest {
    @TempDir Path temporary;

    @Test
    void killedJdtFailsBoundedlyAndLocalProviderStillAnswers() throws Exception {
        Path home = Path.of(System.getProperty("eyecode.jdtls.home"));
        Path workspace = Files.createDirectories(temporary.resolve("workspace"));
        Path file = Files.writeString(workspace.resolve("Main.java"),
                "class Main { void run() { String value = \"EyeCode\"; value.sub } }");
        JdtLsSession session = new JdtLsSession(JdtLsProcessConfiguration.fromInstallation(
                JdtLsToolingJava.currentRuntime(Duration.ofSeconds(5)), home, temporary.resolve("data"), workspace));
        try {
            session.start();
            session.initialize(Duration.ofSeconds(60));
            String source = Files.readString(file);
            session.didOpen(file.toUri().toString(), source, 1);
            int semanticOffset = source.indexOf("value.sub") + "value.sub".length();
            assertFalse(session.completion(file.toUri().toString(), 0, semanticOffset, Duration.ofSeconds(10)).isEmpty());
            killProcess(session);
            waitForState(session, JdtLsLifecycleState.FAILED, Duration.ofSeconds(5));
            long started = System.nanoTime();
            JdtLsProjectCompletion adapter = new JdtLsProjectCompletion(session);
            assertTrue(adapter.complete(request(file, source), Duration.ofMillis(300)).isEmpty());
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 2000);
            assertEquals(JdtLsLifecycleState.FAILED, session.state());
            String fallbackSource = "cla";
            CompletionResult fallback = new JavaCompletionProvider().complete(new CompletionRequest(
                    new LanguageDocument(file.toUri().toString(), file, "Main.java", LanguageId.JAVA),
                    1, fallbackSource, fallbackSource.length(), true, 0, fallbackSource.length()));
            assertTrue(fallback.candidates().stream().anyMatch(item -> item.label().equals("class")));
        } finally {
            session.close();
        }
    }

    private static CompletionRequest request(Path file, String source) {
        int offset = source.indexOf("pub") + 3;
        return new CompletionRequest(new LanguageDocument(file.toUri().toString(), file, "Main.java", LanguageId.JAVA),
                1, source, offset, true, offset - 3, offset);
    }

    private static void killProcess(JdtLsSession session) throws Exception {
        Field field = JdtLsSession.class.getDeclaredField("process");
        field.setAccessible(true);
        ((Process) field.get(session)).destroyForcibly();
    }

    private static void waitForState(JdtLsSession session, JdtLsLifecycleState expected, Duration timeout)
            throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (session.state() != expected && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertEquals(expected, session.state());
    }
}
