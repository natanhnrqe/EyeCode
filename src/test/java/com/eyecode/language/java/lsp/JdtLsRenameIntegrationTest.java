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
            String renamed = applyEdits(source, edits);
            assertFalse(renamed.contains("value"), () -> "renamed=" + renamed);
            assertTrue(renamed.contains("total"), () -> "renamed=" + renamed);
        } finally {
            session.close();
        }
    }

    private static String applyEdits(String source, java.util.List<com.eyecode.language.refactor.RenamePlan.Edit> edits) {
        String[] lines = source.split("\n", -1);
        record AbsoluteEdit(int start, int end, String text) {}
        java.util.List<AbsoluteEdit> absolute = new java.util.ArrayList<>();
        for (com.eyecode.language.refactor.RenamePlan.Edit edit : edits) {
            int start = offsetOf(lines, edit.startLine(), edit.startCharacter());
            int end = offsetOf(lines, edit.endLine(), edit.endCharacter());
            absolute.add(new AbsoluteEdit(start, end, edit.newText()));
        }
        absolute.sort(java.util.Comparator.comparingInt(AbsoluteEdit::start).reversed());
        StringBuilder result = new StringBuilder(source);
        for (AbsoluteEdit edit : absolute) {
            result.replace(edit.start(), edit.end(), edit.text());
        }
        return result.toString();
    }

    private static int offsetOf(String[] lines, int line, int character) {
        int offset = 0;
        for (int index = 0; index < line; index++) {
            offset += lines[index].length() + 1;
        }
        return offset + character;
    }
}
