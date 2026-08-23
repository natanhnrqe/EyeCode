package com.eyecode.project;

import com.eyecode.project.model.ProjectModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProjectFileOperationServiceTest {

    @Test
    void renamesTextFileAndRejectsDuplicateDestination() throws Exception {
        Path root = Files.createTempDirectory("eyecode-ops");
        Path source = Files.writeString(root.resolve("notes.txt"), "hello");
        Files.writeString(root.resolve("readme.txt"), "existing");
        ProjectFileOperationService service = new ProjectFileOperationService();
        ProjectModel project = ProjectModel.fromDirectory(root.toFile());

        assertThrows(IllegalArgumentException.class, () -> service.rename(project, source, "readme.txt"));
        ProjectFileOperationService.RenameResult result = service.rename(project, source, "readme.md");
        assertFalse(Files.exists(source));
        assertEquals("hello", Files.readString(result.newPath()));
    }

    @Test
    void renamesJavaDeclarationAndConstructorsWithoutCommentsOrStrings() throws Exception {
        Path root = Files.createTempDirectory("eyecode-java-rename");
        Path source = Files.writeString(root.resolve("Person.java"), """
                // Person
                class Person {
                    String text = "Person";
                    public Person() {}
                    Person(int age) {}
                }
                """);
        ProjectFileOperationService.RenameResult result = new ProjectFileOperationService()
                .rename(ProjectModel.fromDirectory(root.toFile()), source, "User.java");

        String content = Files.readString(result.newPath());
        assertTrue(content.contains("class User"));
        assertTrue(content.contains("public User()"));
        assertTrue(content.contains("User(int age)"));
        assertTrue(content.contains("// Person"));
        assertTrue(content.contains("\"Person\""));
    }

    @Test
    void supportsInterfaceEnumAndRecordDeclarations() throws Exception {
        for (String kind : new String[]{"interface", "enum", "record"}) {
            Path root = Files.createTempDirectory("eyecode-" + kind);
            Path source = Files.writeString(root.resolve("Person.java"),
                    kind.equals("record") ? "public record Person() {}" : "public " + kind + " Person {}" );
            ProjectFileOperationService.RenameResult result = new ProjectFileOperationService()
                    .rename(ProjectModel.fromDirectory(root.toFile()), source, "User.java");
            assertTrue(Files.readString(result.newPath()).contains(kind + " User"));
        }
    }

    @Test
    void protectsRootOutsidePathsAndRecursivelyDeletesDirectory() throws Exception {
        Path root = Files.createTempDirectory("eyecode-delete");
        Path nested = Files.createDirectories(root.resolve("pkg"));
        Files.writeString(nested.resolve("A.java"), "class A {}");
        ProjectModel project = ProjectModel.fromDirectory(root.toFile());
        ProjectFileOperationService service = new ProjectFileOperationService();

        assertThrows(IllegalArgumentException.class, () -> service.delete(project, root));
        assertThrows(IllegalArgumentException.class, () -> service.delete(project, root.getParent()));
        service.delete(project, nested);
        assertFalse(Files.exists(nested));
    }

    @Test
    void rejectsSymlinkEscape() throws Exception {
        Path root = Files.createTempDirectory("eyecode-safe");
        Path outside = Files.createTempDirectory("eyecode-outside");
        Files.writeString(outside.resolve("secret.txt"), "secret");
        Path link = root.resolve("link");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | java.nio.file.FileSystemException exception) {
            return;
        }
        assertThrows(IllegalArgumentException.class,
                () -> new ProjectFileOperationService().delete(ProjectModel.fromDirectory(root.toFile()), link.resolve("secret.txt")));
        assertTrue(Files.exists(outside.resolve("secret.txt")));
    }
}
