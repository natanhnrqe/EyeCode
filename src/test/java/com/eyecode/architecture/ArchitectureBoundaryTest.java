package com.eyecode.architecture;

import com.eyecode.application.WorkspaceApplication;
import com.eyecode.language.java.LexerEventBridge;
import com.eyecode.project.ProjectFileOperationService;
import com.eyecode.ui.web.LocalWebShellLauncher;
import com.eyecode.ui.web.LocalWebShellSurface;
import com.eyecode.ui.web.WebShellCompletionController;
import com.eyecode.ui.web.WebShellDiagnosticsController;
import com.eyecode.ui.web.WebShellLearningController;
import com.eyecode.ui.web.WebShellLessonsController;
import com.eyecode.ui.web.WebShellWorkspaceComposition;
import com.eyecode.ui.web.WebShellWorkspaceController;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureBoundaryTest {
    private static final Path MAIN_SOURCE = Path.of("src/main/java");
    private static final List<String> UI_IMPORTS = List.of(
            "import com.eyecode.swing.", "import com.eyecode.javafx.",
            "import com.eyecode.ui.", "import javafx.", "import javax.swing.",
            "import java.awt.", "import org.cef.", "import com.techsenger.ceffx.");
    private static final List<String> TOOLKIT_REFERENCES = List.of(
            "com/eyecode/swing/", "com/eyecode/javafx/", "javafx/", "javax/swing/",
            "java/awt/", "org/cef/", "com/techsenger/ceffx/");

    @Test
    void coreAndLessonPackagesDoNotImportUiImplementations() throws IOException {
        assertNoSourceImports(List.of(
                MAIN_SOURCE.resolve("com/eyecode/editor/intelligence"),
                MAIN_SOURCE.resolve("com/eyecode/application"),
                MAIN_SOURCE.resolve("com/eyecode/language"),
                MAIN_SOURCE.resolve("com/eyecode/lessons")), UI_IMPORTS);
        assertNoClassReferences(LexerEventBridge.class, TOOLKIT_REFERENCES);
    }

    @Test
    void applicationHasNoAdapterReferencesOrHiddenFactory() {
        assertNoClassReferences(WorkspaceApplication.class, TOOLKIT_REFERENCES);
        assertNoClassReferences(WorkspaceApplication.class, List.of("com/eyecode/ui/web/"));
        assertTrue(Arrays.stream(WorkspaceApplication.class.getDeclaredMethods())
                        .map(Method::getName)
                        .noneMatch("create"::equals),
                "WorkspaceApplication must not construct concrete runtime services");
        assertTrue(Arrays.stream(WorkspaceApplication.class.getDeclaredMethods())
                        .map(Method::getName)
                        .allMatch("close"::equals),
                "WorkspaceApplication must own lifecycle only, not expose service lookup getters");
    }

    @Test
    void webControllersDeclareDependenciesWithoutApplicationLookup() {
        assertNoClassReferences(WebShellWorkspaceController.class, List.of(
                "com/eyecode/application/WorkspaceApplication",
                "com/eyecode/runtime/RunService",
                "com/eyecode/terminal/TerminalService"));
        assertNoClassReferences(webClass("WebShellExecutionController"),
                List.of("com/eyecode/application/WorkspaceApplication"));
        assertConstructorDependencies(WebShellWorkspaceController.class,
                "com.eyecode.workbench.editor.EditorManager",
                "com.eyecode.application.WorkspaceProjects",
                "com.eyecode.application.ProjectExplorerQuery",
                "com.eyecode.project.ProjectFileOperationService",
                "com.eyecode.ui.web.WebShellDocumentController",
                "com.eyecode.ui.web.WebShellExecutionController");
        assertConstructorDependencies(webClass("WebShellDocumentController"),
                "com.eyecode.workbench.editor.EditorManager",
                "com.eyecode.ui.web.WebShellDiagnosticsController");
        assertConstructorDependencies(webClass("WebShellExecutionController"),
                "com.eyecode.project.ProjectLifecycleService",
                "com.eyecode.runtime.RunService",
                "com.eyecode.terminal.TerminalService");
    }

    @Test
    void webCompositionIsConcreteButIndependentOfDesktopAdapters() {
        assertNoClassReferences(WebShellWorkspaceComposition.class, TOOLKIT_REFERENCES);
        assertClassReferences(WebShellWorkspaceComposition.class, "com/eyecode/eventbus/EventBus");
        assertNoClassReferences(LocalWebShellLauncher.class, TOOLKIT_REFERENCES);
        assertNoClassReferences(webClass("LocalWebShellRuntime"), TOOLKIT_REFERENCES);
        assertNoClassReferences(LocalWebShellSurface.class, TOOLKIT_REFERENCES);
    }

    @Test
    void projectFileOperationsRemainTransportIndependent() {
        assertNoClassReferences(ProjectFileOperationService.class, TOOLKIT_REFERENCES);
        assertNoClassReferences(ProjectFileOperationService.class, List.of("com/eyecode/ui/web/"));
    }

    @Test
    void sharedWebControllersDoNotReferenceToolkitTypes() {
        for (Class<?> controller : List.of(WebShellWorkspaceController.class, webClass("WebShellExecutionController"),
                webClass("WebShellDocumentController"),
                WebShellCompletionController.class, WebShellLearningController.class,
                WebShellLessonsController.class, WebShellDiagnosticsController.class)) {
            assertNoClassReferences(controller, TOOLKIT_REFERENCES);
        }
    }

    @Test
    void swingAndJavaFxDoNotDirectlyImportEachOther() throws IOException {
        assertNoSourceImports(List.of(MAIN_SOURCE.resolve("com/eyecode/swing")),
                List.of("import com.eyecode.javafx."));
        assertNoSourceImports(List.of(MAIN_SOURCE.resolve("com/eyecode/javafx")),
                List.of("import com.eyecode.swing."));
    }

    @Test
    void workspaceCapabilitiesAndTheirResultsAreTransportIndependent() {
        for (Class<?> type : List.of(com.eyecode.application.WorkspaceProjects.class,
                com.eyecode.application.ProjectExplorerQuery.class,
                com.eyecode.application.ProjectExplorerQuery.Entry.class)) {
            assertNoClassReferences(type, TOOLKIT_REFERENCES);
            assertNoClassReferences(type, List.of("com/eyecode/ui/web/"));
        }
        assertNoClassReferences(WebShellWorkspaceController.class, List.of("java/nio/file/Files"));
        assertNoClassReferences(webClass("WebShellDocumentController"), List.of("java/nio/file/Files"));
    }

    @Test
    void documentAndWorkspaceAdaptersDoNotConstructServicesOrSiblingControllers() throws IOException {
        for (String name : List.of("WebShellWorkspaceController", "WebShellDocumentController")) {
            String source = Files.readString(MAIN_SOURCE.resolve("com/eyecode/ui/web/" + name + ".java"));
            assertTrue(!java.util.regex.Pattern.compile(
                    "new\\s+(?:WebShell\\w*Controller|WorkspaceProjects|ProjectExplorerQuery|EditorManager|"
                            + "ProjectFileOperationService|ProjectLifecycleService|MavenProjectCreationService)\\s*\\(")
                    .matcher(source).find(), name + " must use explicitly composed dependencies");
        }
    }

    private static void assertConstructorDependencies(Class<?> type, String... expectedDependencies) {
        List<String> dependencies = Arrays.stream(type.getDeclaredConstructors())
                .flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
                .map(Class::getName)
                .toList();
        for (String dependency : expectedDependencies) {
            assertTrue(dependencies.contains(dependency),
                    () -> type.getSimpleName() + " must declare " + dependency + " in a constructor");
        }
    }

    private static Class<?> webClass(String simpleName) {
        try {
            return Class.forName("com.eyecode.ui.web." + simpleName);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Cannot load Web controller " + simpleName, exception);
        }
    }

    private static void assertNoClassReferences(Class<?> type, List<String> forbiddenReferences) {
        String bytecode = classBytecode(type);
        List<String> violations = forbiddenReferences.stream().filter(bytecode::contains).toList();
        assertTrue(violations.isEmpty(),
                () -> type.getName() + " references forbidden types: " + String.join(", ", violations));
    }

    private static void assertClassReferences(Class<?> type, String expectedReference) {
        assertTrue(classBytecode(type).contains(expectedReference),
                () -> type.getName() + " must reference " + expectedReference);
    }

    private static String classBytecode(Class<?> type) {
        String resource = '/' + type.getName().replace('.', '/') + ".class";
        try (InputStream input = type.getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("Cannot load " + resource);
            return new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot inspect bytecode for " + type.getName(), exception);
        }
    }

    private static void assertNoSourceImports(List<Path> roots, List<String> forbiddenImports) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path root : roots) {
            if (Files.isDirectory(root)) {
                try (Stream<Path> files = Files.walk(root)) {
                    files.filter(path -> path.toString().endsWith(".java"))
                            .forEach(path -> inspectSourceImports(path, forbiddenImports, violations));
                }
            } else {
                inspectSourceImports(root, forbiddenImports, violations);
            }
        }
        assertTrue(violations.isEmpty(), () -> "Forbidden imports:\n" + String.join("\n", violations));
    }

    private static void inspectSourceImports(Path path, List<String> forbiddenImports, List<String> violations) {
        try (Stream<String> lines = Files.lines(path)) {
            lines.map(String::trim).filter(line -> line.startsWith("import "))
                    .forEach(line -> forbiddenImports.stream().filter(line::startsWith)
                            .forEach(forbidden -> violations.add(path + " -> " + forbidden)));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to inspect architecture source " + path, exception);
        }
    }
}
