package com.eyecode.language.java.lsp;

import com.eyecode.language.LanguageDocument;
import com.eyecode.language.LanguageId;
import com.eyecode.language.completion.CompletionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfSystemProperty(named = "eyecode.jdtls.integration", matches = "true")
final class JdtLsProjectCompletionIntegrationTest {
    @TempDir
    Path temporary;

    @Test
    void mapsCrossFileCompletionThroughTheProjectAdapter() throws Exception {
        Path home = Path.of(System.getProperty("eyecode.jdtls.home"));
        Path workspace = Files.createDirectories(temporary.resolve("workspace/src"));
        Path usuario = workspace.resolve("Usuario.java");
        Files.writeString(usuario, "public class Usuario { public String getNome() { return \"EyeCode\"; } public void salvar() {} }");
        Path main = workspace.resolve("Main.java");
        String source = "public class Main { void run() { Usuario usuario = new Usuario(); usuario. } }";
        Files.writeString(main, source);
        JdtLsSession session = new JdtLsSession(JdtLsProcessConfiguration.fromInstallation(
                JdtLsToolingJava.currentRuntime(Duration.ofSeconds(5)), home, temporary.resolve("data"), workspace.getParent()));
        try {
            session.start();
            session.initialize(Duration.ofSeconds(60));
            JdtLsProjectCompletion completion = new JdtLsProjectCompletion(session);
            int offset = source.indexOf("usuario.") + "usuario.".length();
            var result = completion.complete(new CompletionRequest(new LanguageDocument(main.toUri().toString(), main,
                    "Main.java", LanguageId.JAVA), 1, source, offset, true, offset, offset), Duration.ofSeconds(10));
            assertTrue(result.isPresent());
            assertTrue(result.get().candidates().stream().map(candidate -> candidate.label())
                    .anyMatch(label -> label.startsWith("getNome(")));
            assertTrue(result.get().candidates().stream().filter(candidate -> candidate.label().startsWith("getNome("))
                    .allMatch(candidate -> candidate.insertText().equals("getNome()")));
            assertTrue(result.get().candidates().stream().map(candidate -> candidate.label())
                    .anyMatch(label -> label.startsWith("salvar(")));
        } finally {
            session.close();
        }
    }

    @Test
    void synchronizesUnsavedCrossFileChangesWithoutWritingTheFileAgain() throws Exception {
        Path home = Path.of(System.getProperty("eyecode.jdtls.home"));
        Path sourceRoot = Files.createDirectories(temporary.resolve("unsaved/workspace/src"));
        Path usuario = sourceRoot.resolve("Usuario.java");
        String original = "public class Usuario { public String getNome() { return \"EyeCode\"; } }";
        Files.writeString(usuario, original);
        Path main = sourceRoot.resolve("Main.java");
        String mainSource = "public class Main { void run() { Usuario usuario = new Usuario(); usuario. } }";
        Files.writeString(main, mainSource);
        JdtLsSession session = new JdtLsSession(JdtLsProcessConfiguration.fromInstallation(
                JdtLsToolingJava.currentRuntime(Duration.ofSeconds(5)), home, temporary.resolve("unsaved/data"), sourceRoot.getParent()));
        try {
            session.start();
            session.initialize(Duration.ofSeconds(60));
            JdtLsProjectCompletion completion = new JdtLsProjectCompletion(session);
            request(completion, usuario, original, 1);
            var before = request(completion, main, mainSource, 1);
            assertTrue(before.candidates().stream().noneMatch(candidate -> candidate.label().startsWith("salvar(")));
            String added = "public class Usuario { public String getNome() { return \"EyeCode\"; } public void salvar() {} }";
            request(completion, usuario, added, 2);
            assertEquals(original, Files.readString(usuario));
            assertTrue(awaitMember(completion, main, mainSource, "salvar(", true));
            request(completion, usuario, original, 3);
            assertTrue(awaitMember(completion, main, mainSource, "salvar(", false));
            completion.close(usuario);
            String reopened = "public class Usuario { public void reaberto() {} }";
            request(completion, usuario, reopened, 1);
            assertTrue(awaitMember(completion, main, mainSource, "reaberto(", true));
        } finally {
            session.close();
        }
    }

    @Test
    void synchronizationSurvivesNonMonotonicModelVersions() throws Exception {
        Path home = Path.of(System.getProperty("eyecode.jdtls.home"));
        Path sourceRoot = Files.createDirectories(temporary.resolve("undo/workspace/src"));
        Path usuario = sourceRoot.resolve("Usuario.java");
        String original = "public class Usuario { public String getNome() { return \"EyeCode\"; } }";
        Files.writeString(usuario, original);
        Path main = sourceRoot.resolve("Main.java");
        String mainSource = "public class Main { void run() { Usuario usuario = new Usuario(); usuario. } }";
        Files.writeString(main, mainSource);
        JdtLsSession session = new JdtLsSession(JdtLsProcessConfiguration.fromInstallation(
                JdtLsToolingJava.currentRuntime(Duration.ofSeconds(5)), home, temporary.resolve("undo/data"), sourceRoot.getParent()));
        try {
            session.start();
            session.initialize(Duration.ofSeconds(60));
            JdtLsProjectCompletion completion = new JdtLsProjectCompletion(session);
            request(completion, usuario, original, 5);
            request(completion, main, mainSource, 1);
            assertTrue(awaitMember(completion, main, mainSource, "salvar(", false));
            String added = "public class Usuario { public String getNome() { return \"EyeCode\"; } public void salvar() {} }";
            request(completion, usuario, added, 6);
            assertTrue(awaitMember(completion, main, mainSource, "salvar(", true));
            request(completion, usuario, original, 1);
            assertTrue(awaitMember(completion, main, mainSource, "salvar(", false));
        } finally {
            session.close();
        }
    }

    private static com.eyecode.language.completion.CompletionResult request(JdtLsProjectCompletion completion,
                                                                              Path file, String source, long version) {
        int receiver = source.indexOf("usuario.");
        int offset = receiver < 0 ? source.length() : receiver + "usuario.".length();
        return completion.complete(new CompletionRequest(new LanguageDocument(file.toUri().toString(), file,
                file.getFileName().toString(), LanguageId.JAVA), version, source, offset, true, offset, offset),
                Duration.ofSeconds(10)).orElseThrow();
    }

    private static boolean awaitMember(JdtLsProjectCompletion completion, Path file, String source,
                                       String label, boolean expected) {
        long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
        while (System.nanoTime() < deadline) {
            boolean found = request(completion, file, source, 1).candidates().stream()
                    .anyMatch(candidate -> candidate.label().startsWith(label));
            if (found == expected) return true;
            Thread.onSpinWait();
        }
        return false;
    }
}
