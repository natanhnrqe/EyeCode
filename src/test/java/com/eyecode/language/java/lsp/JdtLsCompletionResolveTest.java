package com.eyecode.language.java.lsp;

import com.eyecode.language.completion.CompletionCandidate;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdtLsCompletionResolveTest {
    @Test
    void additionalEditsMapLspRanges() {
        CompletionItem item = new CompletionItem("List");
        item.setAdditionalTextEdits(List.of(
                new TextEdit(new Range(new Position(0, 0), new Position(0, 4)), "import java.util.List;\n")));

        List<JdtLsTextEdit> edits = JdtLsProjectCompletion.additionalEdits(item);

        assertEquals(1, edits.size());
        assertEquals(0, edits.getFirst().startLine());
        assertEquals(4, edits.getFirst().endCharacter());
        assertEquals("import java.util.List;\n", edits.getFirst().newText());
    }

    @Test
    void additionalEditsEmptyWhenAbsent() {
        assertTrue(JdtLsProjectCompletion.additionalEdits(new CompletionItem("x")).isEmpty());
    }

    @Test
    void sequenceValidationRejectsLabelMismatch() {
        CompletionItem original = new CompletionItem("List");
        CompletionItem resolved = new CompletionItem("ArrayList");
        assertThrows(IllegalStateException.class,
                () -> JdtLsProjectCompletion.validateSequence(resolved, original));
    }

    @Test
    void sequenceValidationRejectsInsertTextMismatch() {
        CompletionItem original = new CompletionItem("List");
        original.setInsertText("List");
        CompletionItem resolved = new CompletionItem("List");
        resolved.setInsertText("ArrayList");
        assertThrows(IllegalStateException.class,
                () -> JdtLsProjectCompletion.validateSequence(resolved, original));
    }

    @Test
    void sequenceValidationAcceptsMatchingItem() {
        CompletionItem original = new CompletionItem("List");
        original.setInsertText("List");
        original.setSortText("1");
        CompletionItem resolved = new CompletionItem("List");
        resolved.setInsertText("List");
        resolved.setSortText("1");
        resolved.setDetail("java.util.List");
        JdtLsProjectCompletion.validateSequence(resolved, original);
    }

    @Test
    void resolveCandidateRejectsMissingResolveId() throws Exception {
        JdtLsSession session = fakeSession();
        try {
            JdtLsProjectCompletion completion = new JdtLsProjectCompletion(session);
            CompletionCandidate candidate = new CompletionCandidate("List", "CLASS", "", "", "List", "List",
                    false, 0, 0, 0, "", "", "", "", "", List.of(), "");
            assertThrows(IllegalArgumentException.class,
                    () -> completion.resolveCandidate(candidate, Duration.ofSeconds(1)));
        } finally {
            session.close();
        }
    }

    @Test
    void clearStateIsIdempotent() throws Exception {
        JdtLsSession session = fakeSession();
        try {
            JdtLsProjectCompletion completion = new JdtLsProjectCompletion(session);
            completion.clearState();
            completion.clearState();
        } finally {
            session.close();
        }
    }

    @Test
    void resolveCandidateUnknownResolveIdFails() throws Exception {
        JdtLsSession session = fakeSession();
        try {
            JdtLsProjectCompletion completion = new JdtLsProjectCompletion(session);
            CompletionCandidate candidate = new CompletionCandidate("List", "CLASS", "", "", "List", "List",
                    false, 0, 0, 0, "", "", "", "", "", List.of(), "zz");
            assertTrue(completion.resolveCandidate(candidate, Duration.ofSeconds(1)).additionalTextEdits().isEmpty());
        } finally {
            session.close();
        }
    }

    private static JdtLsSession fakeSession() throws Exception {
        java.nio.file.Path root = java.nio.file.Files.createTempDirectory("jdtls-fake-install");
        java.nio.file.Path plugins = java.nio.file.Files.createDirectories(root.resolve("plugins"));
        java.nio.file.Path launcher = plugins.resolve("org.eclipse.equinox.launcher_1.0.0.jar");
        new java.util.jar.JarOutputStream(java.nio.file.Files.newOutputStream(launcher)).close();
        String platform = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win")
                ? "config_win" : System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("mac")
                ? "config_mac" : "config_linux";
        java.nio.file.Files.createDirectories(root.resolve(platform));
        java.nio.file.Path data = java.nio.file.Files.createTempDirectory("jdtls-fake-data");
        java.nio.file.Path workspace = java.nio.file.Files.createTempDirectory("jdtls-fake-ws");
        return new JdtLsSession(JdtLsProcessConfiguration.fromInstallation(
                JdtLsToolingJava.currentRuntime(Duration.ofSeconds(5)), root, data, workspace));
    }
}
