package com.eyecode.language.java.lsp;

import com.eyecode.language.refactor.PrepareRenameResult;
import com.eyecode.language.refactor.RenamePlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "eyecode.jdtls.integration", matches = "true")
final class JdtLsRenameIntegrationTest {
    @TempDir Path temporary;

    @Test
    void prepareAndRenameAffectEveryOccurrenceOfTheSymbol() throws Exception {
        Path workspace = Files.createDirectories(temporary.resolve("workspace"));
        String source = "class Main {\n    int run() {\n        int value = 1;\n        return value + value;\n    }\n}";
        Path file = Files.writeString(workspace.resolve("Main.java"), source);
        JdtLsSession session = new JdtLsSession(JdtLsProcessConfiguration.fromInstallation(
                JdtLsToolingJava.currentRuntime(Duration.ofSeconds(5)),
                Path.of(System.getProperty("eyecode.jdtls.home")), temporary.resolve("data"), workspace));
        try {
            session.start();
            session.initialize(Duration.ofSeconds(60));
            assertTrue(session.supportsRename());
            JdtLsProjectRefactor refactor = new JdtLsProjectRefactor(session, workspace);
            int offset = source.indexOf("value") + 1;

            Optional<PrepareRenameResult> prepared = refactor.prepareRename(file, source, 1, offset, Duration.ofSeconds(20));
            assertTrue(prepared.isPresent());
            assertEquals("value", prepared.get().placeholder());

            Optional<RenamePlan> plan = refactor.rename(file, source, 1, offset, "total", Duration.ofSeconds(20));
            assertTrue(plan.isPresent());
            assertFalse(plan.get().isEmpty());
            var edits = plan.get().editsByFile().getOrDefault(file, java.util.List.of());
            assertEquals(3, edits.size(), () -> "edits=" + plan.get().editsByFile());
        } finally {
            session.close();
        }
    }
}
