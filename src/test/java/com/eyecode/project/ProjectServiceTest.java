package com.eyecode.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void removesKnownRunFixturesUnderTheSystemTempDirectory() throws Exception {
        Path storage = tempDir.resolve("recent.dat");
        Path stop = Files.createTempDirectory("eyecode-run-stop123");
        Path service = Files.createTempDirectory("eyecode-run-service123");
        ProjectService writer = new ProjectService(storage);
        writer.addRecent(info(stop));
        writer.addRecent(info(service));

        ProjectService loaded = new ProjectService(storage);

        assertTrue(loaded.getRecentProjects().isEmpty());
        assertFalse(Files.readAllBytes(storage).length == 0);
    }

    @Test
    void preservesRealProjectsOutsideTempAndArbitraryProjectsInsideTemp() throws Exception {
        Path storage = tempDir.resolve("recent.dat");
        Path outside = Path.of("target/recent-project-test/eyecode-run-service-example")
                .toAbsolutePath().normalize();
        Path arbitraryTempProject = Files.createTempDirectory("my-real-java-project");
        Files.createDirectories(outside);
        ProjectService writer = new ProjectService(storage);
        writer.addRecent(info(outside));
        writer.addRecent(info(arbitraryTempProject));

        ProjectService loaded = new ProjectService(storage);

        assertEquals(2, loaded.getRecentProjects().size());
        assertTrue(loaded.getRecentProjects().stream()
                .anyMatch(project -> project.getPath().equals(outside.toAbsolutePath().normalize().toString())));
        assertTrue(loaded.getRecentProjects().stream()
                .anyMatch(project -> project.getPath().equals(arbitraryTempProject.toAbsolutePath().normalize().toString())));
    }

    @Test
    void removesNonexistentEntriesAndPersistsTheCleanedList() throws Exception {
        Path storage = tempDir.resolve("recent.dat");
        Path valid = Files.createDirectory(tempDir.resolve("valid-project"));
        ProjectService writer = new ProjectService(storage);
        writer.addRecent(info(valid));
        writer.addRecent(info(tempDir.resolve("deleted-project")));

        ProjectService loaded = new ProjectService(storage);
        byte[] cleaned = Files.readAllBytes(storage);
        ProjectService reloaded = new ProjectService(storage);

        assertEquals(1, loaded.getRecentProjects().size());
        assertEquals(1, reloaded.getRecentProjects().size());
        assertTrue(java.util.Arrays.equals(cleaned, Files.readAllBytes(storage)));
    }

    @Test
    void validLoadDoesNotRewriteTheStorageFile() throws Exception {
        Path storage = tempDir.resolve("recent.dat");
        Path valid = Files.createDirectory(tempDir.resolve("valid"));
        ProjectService writer = new ProjectService(storage);
        writer.addRecent(info(valid));
        byte[] before = Files.readAllBytes(storage);

        new ProjectService(storage);

        assertTrue(java.util.Arrays.equals(before, Files.readAllBytes(storage)));
    }

    @Test
    void fixturePredicateDoesNotMatchARealProjectOutsideTemp() {
        assertFalse(ProjectService.isLegacyEyeCodeTestFixture(
                Path.of("C:/projects/eyecode-run-service-example")));
    }

    private ProjectInfo info(Path path) {
        return new ProjectInfo("fixture", path.toString(), ProjectType.JAVA, 1L);
    }
}
