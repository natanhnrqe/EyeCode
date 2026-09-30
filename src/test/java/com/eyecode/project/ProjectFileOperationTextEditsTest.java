package com.eyecode.project;

import com.eyecode.language.refactor.RenamePlan;
import com.eyecode.project.model.ProjectModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectFileOperationTextEditsTest {

    @Test
    void appliesLineAndCharacterEditsWithoutClobberingSiblings() throws Exception {
        Path root = Files.createTempDirectory("eyecode-text-edits");
        Path file = Files.writeString(root.resolve("A.java"), "class Foo {\n    int foo = 1;\n}\n");
        ProjectFileOperationService service = new ProjectFileOperationService();

        String original = service.applyTextEdits(ProjectModel.fromDirectory(root.toFile()), file, List.of(
                new RenamePlan.Edit(0, 6, 0, 9, "Bar"),
                new RenamePlan.Edit(1, 8, 1, 11, "bar")));

        assertEquals("class Foo {\n    int foo = 1;\n}\n", original);
        assertEquals("class Bar {\n    int bar = 1;\n}\n", Files.readString(file));
    }

    @Test
    void rejectsOverlappingEditsAndTargetsOutsideTheProject() throws Exception {
        Path root = Files.createTempDirectory("eyecode-text-edits-scope");
        Path file = Files.writeString(root.resolve("A.java"), "class Foo {}\n");
        ProjectFileOperationService service = new ProjectFileOperationService();
        ProjectModel project = ProjectModel.fromDirectory(root.toFile());

        assertThrows(IllegalArgumentException.class, () -> service.applyTextEdits(project, file, List.of(
                new RenamePlan.Edit(0, 0, 0, 5, "x"),
                new RenamePlan.Edit(0, 4, 0, 6, "y"))));
        assertThrows(IllegalArgumentException.class, () -> service.applyTextEdits(project,
                root.getParent().resolve("ghost.java"), List.of(new RenamePlan.Edit(0, 0, 0, 1, "x"))));
        assertEquals("class Foo {}\n", Files.readString(file));
    }
}
