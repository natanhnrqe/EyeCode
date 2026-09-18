package com.eyecode.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureBoundaryTest {
    private static final Path MAIN_SOURCE = Path.of("src/main/java");
    private static final List<String> UI_IMPORTS = List.of(
            "import com.eyecode.swing.", "import com.eyecode.javafx.",
            "import com.eyecode.ui.", "import javafx.", "import javax.swing.",
            "import java.awt.", "import org.cef.", "import com.techsenger.ceffx.");
    private static final List<String> TOOLKIT_IMPORTS = List.of(
            "import com.eyecode.swing.", "import com.eyecode.javafx.",
            "import javafx.", "import javax.swing.", "import java.awt.",
            "import org.cef.", "import com.techsenger.ceffx.");

    @Test
    void coreAndLessonPackagesDoNotImportUiImplementations() throws IOException {
        assertNoImports(List.of(
                MAIN_SOURCE.resolve("com/eyecode/editor/intelligence"),
                MAIN_SOURCE.resolve("com/eyecode/language"),
                MAIN_SOURCE.resolve("com/eyecode/lessons")), UI_IMPORTS);
    }

    @Test
    void sharedWebContractsAndControllersDoNotImportToolkitPackages() throws IOException {
        assertNoImports(List.of(
                MAIN_SOURCE.resolve("com/eyecode/ui/web/monaco"),
                MAIN_SOURCE.resolve("com/eyecode/ui/web/learning")), TOOLKIT_IMPORTS);
        assertNoImports(List.of(
                MAIN_SOURCE.resolve("com/eyecode/ui/web/WebShellWorkspaceController.java"),
                MAIN_SOURCE.resolve("com/eyecode/ui/web/WebShellCompletionController.java"),
                MAIN_SOURCE.resolve("com/eyecode/ui/web/WebShellLearningController.java"),
                MAIN_SOURCE.resolve("com/eyecode/ui/web/WebShellLessonsController.java"),
                MAIN_SOURCE.resolve("com/eyecode/ui/web/WebShellDiagnosticsController.java")), TOOLKIT_IMPORTS);
    }

    @Test
    void primaryWebRuntimeDoesNotImportDesktopToolkits() throws IOException {
        assertNoImports(List.of(
                MAIN_SOURCE.resolve("com/eyecode/ui/web/LocalWebShellLauncher.java"),
                MAIN_SOURCE.resolve("com/eyecode/ui/web/LocalWebShellRuntime.java"),
                MAIN_SOURCE.resolve("com/eyecode/ui/web/LocalWebShellSurface.java")), TOOLKIT_IMPORTS);
    }

    @Test
    void swingAndJavaFxDoNotDirectlyImportEachOther() throws IOException {
        assertNoImports(List.of(MAIN_SOURCE.resolve("com/eyecode/swing")),
                List.of("import com.eyecode.javafx."));
        assertNoImports(List.of(MAIN_SOURCE.resolve("com/eyecode/javafx")),
                List.of("import com.eyecode.swing."));
    }

    private static void assertNoImports(List<Path> roots, List<String> forbiddenImports) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path root : roots) {
            if (Files.isDirectory(root)) {
                try (Stream<Path> files = Files.walk(root)) {
                    files.filter(path -> path.toString().endsWith(".java"))
                            .forEach(path -> inspect(path, forbiddenImports, violations));
                }
            } else {
                inspect(root, forbiddenImports, violations);
            }
        }
        assertTrue(violations.isEmpty(), () -> "Forbidden UI dependency:\n" + String.join("\n", violations));
    }

    private static void inspect(Path path, List<String> forbiddenImports, List<String> violations) {
        try {
            String source = Files.readString(path);
            forbiddenImports.stream().filter(source::contains)
                    .forEach(forbidden -> violations.add(path + " -> " + forbidden));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to inspect architecture source " + path, exception);
        }
    }
}
