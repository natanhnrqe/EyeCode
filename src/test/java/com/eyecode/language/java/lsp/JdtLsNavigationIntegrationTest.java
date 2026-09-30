package com.eyecode.language.java.lsp;

import com.eyecode.editor.intelligence.document.LineMap;
import com.eyecode.language.navigation.JdtLsNavigationService;
import com.eyecode.language.navigation.NavigationTarget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "eyecode.jdtls.integration", matches = "true")
final class JdtLsNavigationIntegrationTest {
    private static final String SERVICO_SOURCE =
            "public class Servico {\n    public String saudacao() {\n        return \"EyeCode\";\n    }\n}";
    private static final String MAIN_SOURCE =
            "public class Main {\n    void run() {\n        Servico servico = new Servico();\n        String texto = servico.saudacao();\n    }\n}";

    @TempDir
    Path temporary;

    @Test
    void definitionAndReferencesResolveThroughTheNavigationService() throws Exception {
        Path home = Path.of(System.getProperty("eyecode.jdtls.home"));
        Path sourceRoot = Files.createDirectories(temporary.resolve("workspace/src"));
        Path servico = sourceRoot.resolve("Servico.java");
        Files.writeString(servico, SERVICO_SOURCE);
        Path main = sourceRoot.resolve("Main.java");
        Files.writeString(main, MAIN_SOURCE);
        JdtLsSession session = new JdtLsSession(JdtLsProcessConfiguration.fromInstallation(
                JdtLsToolingJava.currentRuntime(Duration.ofSeconds(5)), home, temporary.resolve("data"),
                sourceRoot.getParent()));
        try {
            session.start();
            session.initialize(Duration.ofSeconds(60));
            assertEquals(JdtLsLifecycleState.READY, session.state());
            assertTrue(session.supportsDefinition(), "JDT LS must advertise definitionProvider");
            assertTrue(session.supportsReferences(), "JDT LS must advertise referencesProvider");
            JdtLsNavigationService navigation = new JdtLsNavigationService(session, new JdtLsDocumentSync());

            List<NavigationTarget> definition = awaitDefinition(navigation, main, MAIN_SOURCE);
            assertFalse(definition.isEmpty(), "definition of the saudacao() call must resolve");
            assertTrue(definition.stream().anyMatch(target -> target.uri().endsWith("Servico.java")),
                    "definition targets must include Servico.java: " + definition);
            String servicoContent = Files.readString(servico);
            for (NavigationTarget target : definition) {
                if (!target.uri().endsWith("Servico.java")) continue;
                String excerpt = servicoContent.substring(target.selectionRange().startOffset(),
                        target.selectionRange().endOffset());
                assertTrue(excerpt.contains("saudacao"),
                        "selection range must select the declaration: \"" + excerpt + "\"");
            }

            LineMap servicoLines = LineMap.of(SERVICO_SOURCE);
            int declarationOffset = SERVICO_SOURCE.indexOf("saudacao");
            List<NavigationTarget> references = awaitReferences(navigation, servico, SERVICO_SOURCE,
                    servicoLines.lineOfOffset(declarationOffset), servicoLines.columnOfOffset(declarationOffset));
            assertFalse(references.isEmpty(), "references of saudacao() must resolve");
            assertTrue(references.size() >= 2,
                    "references with includeDeclaration must contain at least the declaration and the call: "
                            + references);
            assertTrue(references.stream().anyMatch(target -> target.uri().endsWith("Servico.java")),
                    "declaration reference must point back to Servico.java");
            assertTrue(references.stream().anyMatch(target -> target.uri().endsWith("Main.java")),
                    "call reference must point to Main.java");
        } finally {
            session.close();
        }
    }

    private static List<NavigationTarget> awaitDefinition(JdtLsNavigationService navigation, Path file, String source) {
        LineMap lines = LineMap.of(source);
        int callOffset = source.indexOf("saudacao");
        return await(() -> navigation.definition(file, source, 1,
                lines.lineOfOffset(callOffset), lines.columnOfOffset(callOffset)));
    }

    private static List<NavigationTarget> awaitReferences(JdtLsNavigationService navigation, Path file, String source,
                                                           int line, int column) {
        return await(() -> navigation.references(file, source, 1, line, column, true));
    }

    private static List<NavigationTarget> await(java.util.function.Supplier<List<NavigationTarget>> request) {
        List<NavigationTarget> latest = List.of();
        long deadline = System.nanoTime() + Duration.ofSeconds(45).toNanos();
        while (System.nanoTime() < deadline) {
            latest = request.get();
            if (!latest.isEmpty()) return latest;
            Thread.onSpinWait();
        }
        return latest;
    }
}
