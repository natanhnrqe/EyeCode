package com.eyecode.language.java.lsp;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.InitializeResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "eyecode.jdtls.integration", matches = "true")
class JdtLsSemanticCompletionIntegrationTest {
    private static final Duration INITIALIZE_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration COMPLETION_TIMEOUT = Duration.ofSeconds(30);

    @TempDir
    Path temporaryDirectory;

    @Test
    void completesStringMemberThroughThePinnedExternalJdtLanguageServer() throws Exception {
        Path installation = configuredInstallation();
        Path workspace = Files.createDirectories(temporaryDirectory.resolve("workspace"));
        Path sourceDirectory = Files.createDirectories(workspace.resolve("src"));
        Path sourceFile = sourceDirectory.resolve("Main.java");
        String source = """
                public class Main {
                    public static void main(String[] args) {
                        String value = \"EyeCode\";
                        value.sub
                    }
                }
                """;
        Files.writeString(sourceFile, source);
        JdtLsProcessConfiguration configuration = JdtLsProcessConfiguration.fromInstallation(
                JdtLsToolingJava.currentRuntime(Duration.ofSeconds(5)), installation,
                temporaryDirectory.resolve("jdt-data"), workspace);
        JdtLsSession session = new JdtLsSession(configuration);
        try {
            session.start();
            InitializeResult initialized = session.initialize(INITIALIZE_TIMEOUT);
            assertNotNull(initialized.getCapabilities().getCompletionProvider(), "JDT LS must advertise completion");

            String uri = sourceFile.toUri().toString();
            session.didOpen(uri, source, 1);
            List<CompletionItem> completion = session.completion(uri, 3, "        value.sub".length(), COMPLETION_TIMEOUT);

            assertTrue(completion.stream().map(CompletionItem::getLabel).anyMatch(label -> label.startsWith("substring(")),
                    () -> "Expected semantic String completion 'substring', received " + completion.stream()
                            .map(CompletionItem::getLabel).limit(20).toList());
        } finally {
            session.close();
        }
        assertEqualsStopped(session);
    }

    private static Path configuredInstallation() {
        String configured = System.getProperty("eyecode.jdtls.home", "").trim();
        assertTrue(!configured.isBlank(), "Set -Deyecode.jdtls.home to the pinned JDT LS 1.61.0 installation root");
        Path installation = Path.of(configured).toAbsolutePath().normalize();
        assertTrue(Files.isDirectory(installation), () -> "JDT LS installation does not exist: " + installation);
        return installation;
    }

    private static void assertEqualsStopped(JdtLsSession session) {
        assertTrue(session.state() == JdtLsLifecycleState.STOPPED, () -> "JDT LS did not stop: " + session.state());
        assertTrue(!session.isProcessAlive(), "JDT LS process remains alive after close");
    }
}
