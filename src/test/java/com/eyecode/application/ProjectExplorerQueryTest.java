package com.eyecode.application;

import com.eyecode.project.model.ProjectModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class ProjectExplorerQueryTest {
    @TempDir Path root;
    private final ProjectExplorerQuery query = new ProjectExplorerQuery();

    private ProjectModel project() { return ProjectModel.fromDirectory(root.toFile()); }

    @Test
    void childrenAreFilteredSortedAndLazilyDescribed() throws Exception {
        Files.createDirectories(root.resolve("target/hidden"));
        Files.createDirectories(root.resolve("src/nested"));
        Files.createDirectory(root.resolve("empty"));
        Files.writeString(root.resolve("Z.java"), "class Z {}");
        Files.writeString(root.resolve("a.txt"), "a");
        var children = query.children(project(), root);
        assertEquals(List.of("empty", "src", "a.txt", "Z.java"), children.stream().map(ProjectExplorerQuery.Entry::name).toList());
        assertFalse(children.get(0).hasChildren());
        assertTrue(children.get(1).hasChildren());
        assertFalse(children.get(2).directory());
        assertTrue(query.node(root).hasChildren());
    }

    @Test
    void hiddenDirectoriesDoNotCreateAnExpandPlaceholder() throws Exception {
        Files.createDirectory(root.resolve("target"));
        assertFalse(query.node(root).hasChildren());
        assertTrue(query.children(project(), root).isEmpty());
    }

    @Test
    void validatesTreePathsAndProjectFiles() throws Exception {
        Path file = Files.writeString(root.resolve("Main.java"), "class Main {}");
        assertThrows(IllegalArgumentException.class, () -> query.children(project(), root.getParent()));
        assertThrows(IllegalArgumentException.class, () -> query.children(project(), file));
        assertTrue(query.isProjectFile(project(), file));
        assertFalse(query.isProjectFile(project(), root));
        assertFalse(query.isProjectFile(null, file));
    }

    @Test
    void refreshKeepsOnlyExistingInProjectDirectoriesAndFallsBackToRoot() throws Exception {
        Path src = Files.createDirectory(root.resolve("src"));
        assertEquals(List.of(src), query.validExpandedDirectories(project(), List.of(src, root.getParent(), root.resolve("missing"))));
        assertEquals(List.of(root), query.validExpandedDirectories(project(), List.of(root.resolve("missing"))));
    }

    @Test
    void ancestorsAndChangeParentsStayWithinProject() throws Exception {
        Path directory = Files.createDirectories(root.resolve("src/main/java"));
        assertEquals(List.of(root.resolve("src"), root.resolve("src/main"), directory),
                query.ancestors(project(), directory.resolve("Main.java")));
        assertEquals(List.of(), query.ancestors(project(), root));
        assertThrows(IllegalArgumentException.class, () -> query.ancestors(project(), root.getParent()));
        assertEquals(Optional.of(directory), query.changedParent(project(), directory.resolve("Main.java")));
        assertTrue(query.changedParent(project(), root).isEmpty());
        assertTrue(query.changedParent(null, directory).isEmpty());
    }

    @Test
    void preferredSourceUsesConfigurationBeforeDeterministicFallback() throws Exception {
        Path src = Files.createDirectories(root.resolve("src/main/java/demo"));
        Path main = Files.writeString(src.resolve("Main.java"), "class Main {}");
        Path configured = Files.writeString(src.resolve("App.java"), "class App {}");
        assertEquals(Optional.of(configured), query.preferredEntryPoint(project(), Optional.of("demo.App")));
        assertEquals(Optional.of(main), query.preferredEntryPoint(project(), Optional.of("missing.App")));
    }

    @Test
    void fallbackIgnoresGeneratedSourcesAndSupportsPlainProjects() throws Exception {
        Files.createDirectories(root.resolve("src/target"));
        Files.writeString(root.resolve("src/target/Main.java"), "class Main {}");
        assertTrue(query.preferredEntryPoint(project(), Optional.empty()).isEmpty());
        Path main = Files.writeString(root.resolve("src/Main.java"), "class Main {}");
        assertEquals(Optional.of(main), query.preferredEntryPoint(project(), Optional.empty()));
    }
}
