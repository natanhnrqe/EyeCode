package com.eyecode.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectServiceTest {

    @TempDir
    Path tempDir;
    private final List<Path> createdRoots = new ArrayList<>();

    @AfterEach
    void removeCreatedRoots() throws Exception {
        for (Path root : createdRoots) {
            if (Files.exists(root)) {
                try (var paths = Files.walk(root)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
                }
            }
        }
    }

    @Test
    void removesKnownRunFixturesUnderTheSystemTempDirectory() throws Exception {
        Path storage = tempDir.resolve("recent.dat");
        Path stop = Files.createTempDirectory("eyecode-run-stop123");
        Path service = Files.createTempDirectory("eyecode-run-service123");
        ProjectService writer = new ProjectService(storage);
        writer.addRecent(info(stop));
        writer.addRecent(info(service));
        writer.save();

        ProjectService loaded = new ProjectService(storage);

        assertTrue(loaded.getRecentProjects().isEmpty());
        assertFalse(Files.readAllBytes(storage).length == 0);
    }

    @Test
    void preservesRealProjectsOutsideTempAndArbitraryProjectsInsideTemp() throws Exception {
        Path storage = tempDir.resolve("recent.dat");
        Path outside = createProjectRoot("eyecode-run-service-example");
        Path arbitraryTempProject = Files.createTempDirectory("my-real-java-project");
        Files.createDirectories(outside);
        ProjectService writer = new ProjectService(storage);
        writer.addRecent(info(outside));
        writer.addRecent(info(arbitraryTempProject));

        ProjectService loaded = new ProjectService(storage);

        assertEquals(1, loaded.getRecentProjects().size());
        assertTrue(loaded.getRecentProjects().stream()
                .anyMatch(project -> project.getPath().equals(outside.toAbsolutePath().normalize().toString())));
        assertTrue(loaded.getRecentProjects().stream()
                .noneMatch(project -> project.getPath().equals(arbitraryTempProject.toAbsolutePath().normalize().toString())));
    }

    @Test
    void retainsRecentNonemptyWorkspaceEvenWhenProjectDetectorDoesNotRecognizeIt() throws Exception {
        Path root = Path.of("target").toAbsolutePath().normalize()
                .resolve("recent-unmarked-" + UUID.randomUUID());
        Files.createDirectories(root);
        Files.writeString(root.resolve("README.md"), "workspace");
        createdRoots.add(root);
        ProjectService service = new ProjectService(tempDir.resolve("recent.dat"));

        service.addRecent(info(root));

        assertEquals(root.toAbsolutePath().normalize().toString(),
                service.getRecentProjects().getFirst().getPath());
    }

    @Test
    void removesNonexistentEntriesAndPersistsTheCleanedList() throws Exception {
        Path storage = tempDir.resolve("recent.dat");
        Path valid = createProjectRoot("valid-project");
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
        Path valid = createProjectRoot("valid");
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

    @Test
    void keepsRecentProjectsBoundedAfterRepeatedAdds() throws Exception {
        ProjectService service = new ProjectService(tempDir.resolve("recent.dat"));

        for (int index = 0; index < 11; index++) {
            Path project = createProjectRoot("project-" + index);
            service.addRecent(info(project));
        }

        assertEquals(10, service.getRecentProjects().size());
    }

    private Path createProjectRoot(String name) throws Exception {
        Path target = Path.of("target").toAbsolutePath().normalize();
        Files.createDirectories(target);
        Path root = Files.createDirectory(target.resolve("recent-project-" + name + "-" + UUID.randomUUID()));
        Files.writeString(root.resolve("pom.xml"), "<project><modelVersion>4.0.0</modelVersion></project>");
        createdRoots.add(root);
        return root;
    }

    private ProjectInfo info(Path path) {
        return new ProjectInfo("fixture", path.toString(), ProjectType.JAVA, 1L);
    }
}
