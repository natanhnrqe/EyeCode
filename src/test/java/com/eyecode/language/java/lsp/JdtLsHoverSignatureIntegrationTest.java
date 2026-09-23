package com.eyecode.language.java.lsp;

import com.eyecode.language.LanguageDocument;
import com.eyecode.language.LanguageFeatureRequest;
import com.eyecode.language.LanguageId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "eyecode.jdtls.integration", matches = "true")
final class JdtLsHoverSignatureIntegrationTest {
    @TempDir Path temporary;

    @Test
    void advertisedCapabilitiesProduceNeutralHoverAndSignatureResults() throws Exception {
        Path workspace = Files.createDirectories(temporary.resolve("workspace"));
        String source = "class Main {\n    void run() {\n        String value = \"EyeCode\";\n        value.substring(0, 3);\n    }\n}";
        String signatureSource = source.replace("value.substring(0, 3);", "value.substring(");
        Path file = Files.writeString(workspace.resolve("Main.java"), source);
        JdtLsSession session = new JdtLsSession(JdtLsProcessConfiguration.fromInstallation(
                JdtLsToolingJava.currentRuntime(Duration.ofSeconds(5)),
                Path.of(System.getProperty("eyecode.jdtls.home")), temporary.resolve("data"), workspace));
        try {
            session.start();
            session.initialize(Duration.ofSeconds(60));
            assertTrue(session.supportsHover());
            assertTrue(session.supportsSignatureHelp());
            JdtLsProjectCompletion adapter = new JdtLsProjectCompletion(session);
            LanguageDocument document = new LanguageDocument(file.toUri().toString(), file, "Main.java", LanguageId.JAVA);
            int hoverOffset = source.indexOf("substring") + 3;
            var hover = adapter.hover(new LanguageFeatureRequest(document, 1, source, hoverOffset), Duration.ofSeconds(10));
            assertTrue(hover.isPresent());
            assertFalse(hover.get().contents().isEmpty());
            assertTrue(hover.get().contents().stream().map(content -> content.value().toLowerCase())
                    .anyMatch(value -> value.contains("substring")));
            assertTrue(hover.get().rangeEnd() >= hover.get().rangeStart());

            int signatureOffset = signatureSource.indexOf("substring(") + "substring(".length();
            var signature = adapter.signatureHelp(new LanguageFeatureRequest(document, 2, signatureSource, signatureOffset),
                    Duration.ofSeconds(10));
            assertTrue(signature.isPresent());
            assertFalse(signature.get().signatures().isEmpty(), () -> "signature result=" + signature.get());
            assertTrue(signature.get().signatures().stream().anyMatch(value -> value.label().contains("substring")));
            assertTrue(signature.get().signatures().stream().anyMatch(value -> !value.parameters().isEmpty()));
            assertNotNull(signature.get().activeParameter());
        } finally {
            session.close();
        }
    }
}
