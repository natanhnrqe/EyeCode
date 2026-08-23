package com.eyecode.project;

import com.eyecode.project.model.ProjectModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectCreationServiceTest {

    @Test
    void derivesPackagesOnlyBelowMainAndTestSourceRoots() throws Exception {
        Path root = Files.createTempDirectory("eyecode-create");
        Path main = root.resolve("src/main/java/com/example");
        Path test = root.resolve("src/test/java/com/example");
        Files.createDirectories(main);
        Files.createDirectories(test);
        ProjectCreationService service = new ProjectCreationService();
        ProjectModel project = ProjectModel.fromDirectory(root.toFile());

        assertEquals("com.example", service.packageNameFor(
                new ProjectCreationService.CreationContext(project, main), ""));
        assertEquals("", service.packageNameFor(
                new ProjectCreationService.CreationContext(project, root.resolve("src/main/java")), ""));
        assertEquals("com.example", service.packageNameFor(
                new ProjectCreationService.CreationContext(project, test), ""));
    }

    @Test
    void createsAllJavaTypeTemplatesWithUsefulCaret() throws Exception {
        Path root = Files.createTempDirectory("eyecode-templates");
        Path source = root.resolve("src/main/java/com/example");
        Files.createDirectories(source);
        ProjectModel project = ProjectModel.fromDirectory(root.toFile());
        ProjectCreationService service = new ProjectCreationService();
        ProjectCreationService.CreationContext context =
                new ProjectCreationService.CreationContext(project, source);

        int index = 0;
        for (ProjectCreationService.JavaTypeKind kind : ProjectCreationService.JavaTypeKind.values()) {
            String name = "Generated" + (++index);
            ProjectCreationService.CreationResult result = service.createJavaType(context, kind, name);
            String content = result.source();
            assertTrue(content.contains("package com.example;"));
            assertTrue(content.contains(name));
            assertTrue(result.caretOffset() <= content.length());
        }
        assertTrue(Files.exists(source.resolve("Generated5.java")));
    }

    @Test
    void createsNestedPackagesAndPlainJavaFilesWithoutInventingTypes() throws Exception {
        Path root = Files.createTempDirectory("eyecode-package");
        Path source = root.resolve("src/main/java");
        Files.createDirectories(source);
        ProjectModel project = ProjectModel.fromDirectory(root.toFile());
        ProjectCreationService service = new ProjectCreationService();
        ProjectCreationService.CreationContext context =
                new ProjectCreationService.CreationContext(project, source);

        service.createPackage(context, "com.example.service");
        assertTrue(Files.isDirectory(source.resolve("com/example/service")));
        ProjectCreationService.CreationResult file = service.createJavaFile(
                new ProjectCreationService.CreationContext(project, source.resolve("com/example/service")), "Helper.java");
        assertEquals("Helper.java", file.path().getFileName().toString());
        assertFalse(file.source().contains("class Helper"));
    }

    @Test
    void rejectsInvalidNamesAndNeverOverwritesExistingFiles() throws Exception {
        Path root = Files.createTempDirectory("eyecode-validation");
        Path source = root.resolve("src/main/java");
        Files.createDirectories(source);
        ProjectModel project = ProjectModel.fromDirectory(root.toFile());
        ProjectCreationService service = new ProjectCreationService();
        ProjectCreationService.CreationContext context =
                new ProjectCreationService.CreationContext(project, source);

        assertThrows(IllegalArgumentException.class, () -> service.createPackage(context, "com..example"));
        service.createJavaType(context, ProjectCreationService.JavaTypeKind.CLASS, "User");
        assertThrows(Exception.class, () -> service.createJavaType(context,
                ProjectCreationService.JavaTypeKind.CLASS, "User"));
        assertThrows(IllegalArgumentException.class, () -> service.createJavaType(context,
                ProjectCreationService.JavaTypeKind.CLASS, "123User"));
    }

    @Test
    void doesNotTreatResourcesAsAJavaPackageRoot() throws Exception {
        Path root = Files.createTempDirectory("eyecode-resources");
        Path resources = root.resolve("src/main/resources");
        Files.createDirectories(resources);
        ProjectModel project = ProjectModel.fromDirectory(root.toFile());
        ProjectCreationService service = new ProjectCreationService();

        assertThrows(IllegalArgumentException.class, () -> service.packageNameFor(
                new ProjectCreationService.CreationContext(project, resources), ""));
    }

    @Test
    void supportsAStandardSrcLayoutWithoutGeneratingAnEmptyPackage() throws Exception {
        Path root = Files.createTempDirectory("eyecode-standard-src");
        Path source = root.resolve("src");
        Files.createDirectories(source);
        ProjectModel project = ProjectModel.fromDirectory(root.toFile());
        ProjectCreationService service = new ProjectCreationService();

        assertEquals("", service.packageNameFor(
                new ProjectCreationService.CreationContext(project, source), ""));
        ProjectCreationService.CreationResult result = service.createJavaType(
                new ProjectCreationService.CreationContext(project, source),
                ProjectCreationService.JavaTypeKind.CLASS, "Main");
        assertEquals("Main.java", result.path().getFileName().toString());
        assertFalse(result.source().contains("package ;"));
    }
}
