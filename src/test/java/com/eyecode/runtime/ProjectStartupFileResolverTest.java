package com.eyecode.runtime;

import com.eyecode.project.model.ProjectModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectStartupFileResolverTest {
    @TempDir
    Path tempDir;

    @Test
    void selectsSpringBootApplicationBeforeGenericMain() throws Exception {
        Path root = project("spring");
        Path application = java(root, "src/main/java/demo/Application.java", """
                package demo;
                @SpringBootApplication
                class Application { public static void main(String[] args) {} }
                """);
        java(root, "src/main/java/demo/ImportTool.java", """
                package demo;
                class ImportTool { public static void main(String[] args) {} }
                """);

        assertEquals(application, resolver(root).resolve(ProjectModel.fromDirectory(root.toFile())).orElseThrow());
    }

    @Test
    void selectsSingleMainClass() throws Exception {
        Path root = project("main");
        Path main = java(root, "src/main/java/Main.java", "class Main { public static void main(String[] args) {} }");
        java(root, "src/main/java/Person.java", "class Person {}");

        assertEquals(main, resolver(root).resolve(ProjectModel.fromDirectory(root.toFile())).orElseThrow());
    }

    @Test
    void selectedRunConfigurationOverridesDefaultCandidate() throws Exception {
        Path root = project("multiple");
        java(root, "src/main/java/demo/Main.java", """
                package demo;
                class Main { public static void main(String[] args) {} }
                """);
        Path importTool = java(root, "src/main/java/demo/ImportTool.java", """
                package demo;
                class ImportTool { public static void main(String[] args) {} }
                """);
        RunConfigurationSelectionStore store = new RunConfigurationSelectionStore(root.resolve("selection.properties"));
        store.select(root, "java:demo.ImportTool");
        ProjectStartupFileResolver resolver = new ProjectStartupFileResolver(new RunConfigurationDiscoveryService(), store);

        assertEquals(importTool, resolver.resolve(ProjectModel.fromDirectory(root.toFile())).orElseThrow());
    }

    @Test
    void usesTheSameDefaultConfigurationPreferenceAsRunService() throws Exception {
        Path root = project("default-configuration");
        Path main = java(root, "src/main/java/demo/Main.java", """
                package demo;
                class Main { public static void main(String[] args) {} }
                """);
        java(root, "src/main/java/demo/ImportTool.java", """
                package demo;
                class ImportTool { public static void main(String[] args) {} }
                """);

        assertEquals(main, resolver(root).resolve(ProjectModel.fromDirectory(root.toFile())).orElseThrow());
    }

    @Test
    void selectsDeterministicNonTestSourceWhenNoMainExists() throws Exception {
        Path root = project("sources");
        Path address = java(root, "src/main/java/demo/Address.java", "package demo; class Address {}");
        java(root, "src/main/java/demo/Person.java", "package demo; class Person {}");
        java(root, "src/test/java/demo/Fixture.java", "package demo; class Fixture {}");

        assertEquals(address, resolver(root).resolve(ProjectModel.fromDirectory(root.toFile())).orElseThrow());
    }

    @Test
    void leavesEmptyProjectWithoutCandidate() throws Exception {
        Path root = project("empty");

        assertTrue(resolver(root).resolve(ProjectModel.fromDirectory(root.toFile())).isEmpty());
    }

    private ProjectStartupFileResolver resolver(Path root) {
        return new ProjectStartupFileResolver(new RunConfigurationDiscoveryService(),
                new RunConfigurationSelectionStore(root.resolve("selection.properties")));
    }

    private Path project(String name) throws Exception {
        return Files.createDirectory(tempDir.resolve(name));
    }

    private Path java(Path root, String relative, String content) throws Exception {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file.toAbsolutePath().normalize();
    }
}
