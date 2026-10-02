package com.eyecode.language.java.lsp;

import com.eyecode.language.diagnostics.QuickFix;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfSystemProperty(named = "eyecode.jdtls.integration", matches = "true")
final class JdtLsQuickFixIntegrationTest {
    private static final String SOURCE = "public class Main {\n"
            + "    void run() throws Exception {\n"
            + "        FileReader reader = null;\n"
            + "    }\n"
            + "}";

    @TempDir
    Path temporary;

    @Test
    void quickFixesForUnresolvedFileReaderIncludeImportAction() throws Exception {
        Path home = Path.of(System.getProperty("eyecode.jdtls.home"));
        Path workspace = Files.createDirectories(temporary.resolve("workspace/src"));
        Path main = workspace.resolve("Main.java");
        Files.writeString(main, SOURCE);
        JdtLsSession session = new JdtLsSession(JdtLsProcessConfiguration.fromInstallation(
                JdtLsToolingJava.currentRuntime(Duration.ofSeconds(5)), home, temporary.resolve("data"), workspace.getParent()));
        JdtLsDocumentSync documents = new JdtLsDocumentSync();
        JdtLsProjectDiagnostics diagnostics = new JdtLsProjectDiagnostics(documents);
        try {
            session.start();
            session.initialize(Duration.ofSeconds(60));
            diagnostics.attach(session);
            String uri = documents.synchronize(session, main, SOURCE, 1);

            List<Diagnostic> published = waitForFileReaderDiagnostic(session, uri, Duration.ofSeconds(40));
            assertFalse(published.isEmpty(), () -> "FileReader diagnostic never published for " + uri);

            long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
            List<QuickFix> fixes = List.of();
            while (System.nanoTime() < deadline) {
                fixes = diagnostics.quickFixes(uri, 3, 9, 3, 17);
                if (hasImportFix(fixes)) break;
                LockSupport.parkNanos(1_000_000_000L);
            }
            if (!hasImportFix(fixes)) {
                List<Diagnostic> current = session.publishedDiagnostics(uri);
                var raw = session.codeAction(uri, JdtLsProjectDiagnostics.range(3, 9, 3, 17),
                        new CodeActionContext(current, List.of("quickfix")));
                fail("import quick fix missing; published=" + published + " raw=" + raw + " fixes=" + fixes);
            }
        } finally {
            session.close();
        }
    }

    private static boolean hasImportFix(List<QuickFix> fixes) {
        return fixes.stream().anyMatch(fix -> fix.title().contains("Import")
                && fix.title().contains("FileReader") && !fix.edits().isEmpty());
    }

    private static List<Diagnostic> waitForFileReaderDiagnostic(JdtLsSession session, String uri, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        List<Diagnostic> published = session.publishedDiagnostics(uri);
        while (System.nanoTime() < deadline
                && published.stream().noneMatch(item -> {
                    String text = JdtLsProjectDiagnostics.message(item.getMessage());
                    return text.contains("FileReader") && text.contains("cannot be resolved");
                })) {
            LockSupport.parkNanos(50_000_000L);
            published = session.publishedDiagnostics(uri);
        }
        return published;
    }
}
