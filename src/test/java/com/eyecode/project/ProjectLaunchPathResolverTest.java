package com.eyecode.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectLaunchPathResolverTest {
    @TempDir Path temp;

    @Test
    void launchDirectoryIsUsedAsStartupProject() throws Exception {
        Path root = Files.createDirectory(temp.resolve("workspace"));
        assertEquals(root.toAbsolutePath().normalize(),
                ProjectLaunchPathResolver.resolve(new String[]{root.toString()}));
    }

    @Test
    void launchOfProjectFileResolvesToRecognizedProjectRoot() throws Exception {
        Path root = Files.createDirectory(temp.resolve("project"));
        Path source = Files.createDirectories(root.resolve("src/main/java/example")).resolve("Main.java");
        Files.writeString(root.resolve("pom.xml"), "<project/>");
        Files.writeString(source, "class Main {}");

        assertEquals(root.toAbsolutePath().normalize(),
                ProjectLaunchPathResolver.resolve(new String[]{source.toString()}));
    }

    @Test
    void launchWithoutPathHasNoStartupProject() {
        assertEquals(null, ProjectLaunchPathResolver.resolve(new String[0]));
    }
}
