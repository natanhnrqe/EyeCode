package com.eyecode.language.java.lsp;

import com.eyecode.language.LanguageDocument;
import com.eyecode.language.LanguageId;
import com.eyecode.language.completion.CompletionCandidate;
import com.eyecode.language.completion.CompletionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "eyecode.jdtls.integration", matches = "true")
final class JdtLsCompletionResolveIntegrationTest {
    @TempDir
    Path temporary;

    @Test
    void resolveProducesAutoImportEditsAndDocumentation() throws Exception {
        Path home = Path.of(System.getProperty("eyecode.jdtls.home"));
        Path workspace = Files.createDirectories(temporary.resolve("workspace/src"));
        Path main = workspace.resolve("Main.java");
        String source = "public class Main { void run() { Arra } }";
        Files.writeString(main, source);
        JdtLsSession session = new JdtLsSession(JdtLsProcessConfiguration.fromInstallation(
                JdtLsToolingJava.currentRuntime(Duration.ofSeconds(5)), home, temporary.resolve("data"), workspace.getParent()));
        try {
            session.start();
            session.initialize(Duration.ofSeconds(60));
            JdtLsProjectCompletion completion = new JdtLsProjectCompletion(session);
            int offset = source.indexOf("Arra") + "Arra".length();
            var result = completion.complete(new CompletionRequest(new LanguageDocument(main.toUri().toString(), main,
                    "Main.java", LanguageId.JAVA), 1, source, offset, true, offset, offset), Duration.ofSeconds(10));
            assertTrue(result.isPresent());
            CompletionCandidate candidate = result.get().candidates().stream()
                    .filter(item -> item.label().startsWith("ArrayList")).findFirst().orElse(null);
            if (candidate != null && !candidate.resolveId().isEmpty()) {
                JdtLsCompletionResolveResult resolved = completion.resolveCandidate(candidate, Duration.ofSeconds(10));
                org.junit.jupiter.api.Assertions.assertNotNull(resolved.additionalTextEdits());
                org.junit.jupiter.api.Assertions.assertNotNull(resolved.documentation());
                org.junit.jupiter.api.Assertions.assertTrue(
                        resolved.additionalTextEdits().stream().anyMatch(
                                edit -> edit.newText().contains("java.util.ArrayList")),
                        () -> "auto-import edit ausente; edits=" + resolved.additionalTextEdits());
                completion.clearState();
            }
        } finally {
            session.close();
        }
    }
}
