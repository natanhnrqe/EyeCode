package com.eyecode.project;

import com.eyecode.project.model.ProjectModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectLifecycleServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void opensAnyDirectoryAndReportsProjectChanges() throws Exception {
        Path storage = tempDir.resolve("recent.dat");
        Path project = Files.createDirectory(tempDir.resolve("PlainJava"));
        Files.writeString(project.resolve("Main.java"), "class Main {}");

        ProjectLifecycleService service = new ProjectLifecycleService(new ProjectService(storage));
        AtomicReference<ProjectModel> changed = new AtomicReference<>();
        service.addListener(changed::set);

        ProjectModel opened = service.open(project);

        assertEquals(project.toAbsolutePath().normalize(), opened.getRootDir());
        assertTrue(opened.isValid());
        assertEquals(com.eyecode.project.model.ProjectType.JAVA, opened.getType());
        assertEquals(opened, changed.get());
        assertTrue(service.recentProjects().isEmpty());
        service.recordRecent(opened);
        assertEquals(1, service.recentProjects().size());
    }

    @Test
    void detectsMavenAndGradleWithoutScanningSources() throws Exception {
        Path maven = Files.createDirectory(tempDir.resolve("maven"));
        Files.writeString(maven.resolve("pom.xml"), "<project/>");
        Path gradle = Files.createDirectory(tempDir.resolve("gradle"));
        Files.writeString(gradle.resolve("build.gradle.kts"), "plugins {}");

        ProjectLifecycleService service = new ProjectLifecycleService(
                new ProjectService(tempDir.resolve("recent.dat")));

        assertEquals(com.eyecode.project.model.ProjectType.MAVEN,
                service.open(maven).getType());
        assertEquals(com.eyecode.project.model.ProjectType.GRADLE,
                service.open(gradle).getType());
    }

    @Test
    void rejectsMissingDirectoriesAndFiles() throws Exception {
        ProjectLifecycleService service = new ProjectLifecycleService(
                new ProjectService(tempDir.resolve("recent.dat")));

        assertThrows(IllegalArgumentException.class,
                () -> service.open(tempDir.resolve("missing")));
        Path file = Files.createFile(tempDir.resolve("not-a-project-dir"));
        assertThrows(IllegalArgumentException.class, () -> service.open(file));
    }

    @Test
    void closeClearsCurrentProjectWithoutRemovingRecentProject() throws Exception {
        Path project = Files.createDirectory(tempDir.resolve("project"));
        ProjectLifecycleService service = new ProjectLifecycleService(
                new ProjectService(tempDir.resolve("recent.dat")));

        service.open(project);
        service.close();

        assertNull(service.currentProject());
        assertTrue(service.recentProjects().isEmpty());
    }

    @Test
    void recentProjectsPersistAndReopenWithNormalizedPath() throws Exception {
        Path storage = tempDir.resolve("recent.dat");
        Path project = Files.createDirectory(tempDir.resolve("project"));
        ProjectLifecycleService first = new ProjectLifecycleService(new ProjectService(storage));
        first.open(project.resolve("."));
        assertTrue(first.recentProjects().isEmpty());
        first.recordRecent(first.currentProject());

        ProjectLifecycleService second = new ProjectLifecycleService(new ProjectService(storage));
        List<ProjectInfo> recent = second.recentProjects();

        assertEquals(1, recent.size());
        assertEquals(project.toAbsolutePath().normalize().toString(), recent.getFirst().getPath());
        assertEquals(project.toAbsolutePath().normalize(), second.openRecent(recent.getFirst()).getRootDir());
    }

    @Test
    void internalFixtureOpeningNeverMutatesRecentProjects() throws Exception {
        Path project = Files.createDirectory(tempDir.resolve("fixture"));
        ProjectLifecycleService service = new ProjectLifecycleService(
                new ProjectService(tempDir.resolve("recent.dat")));

        service.open(project);

        assertTrue(service.recentProjects().isEmpty());
    }

    @Test
    void explicitRecordingIsIdempotentForTheSameProject() throws Exception {
        Path project = Files.createDirectory(tempDir.resolve("user-project"));
        ProjectLifecycleService service = new ProjectLifecycleService(
                new ProjectService(tempDir.resolve("recent.dat")));

        ProjectModel opened = service.open(project);
        service.recordRecent(opened);
        service.recordRecent(opened);

        assertEquals(1, service.recentProjects().size());
    }
}
