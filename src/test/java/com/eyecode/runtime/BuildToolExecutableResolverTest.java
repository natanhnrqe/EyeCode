package com.eyecode.runtime;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildToolExecutableResolverTest {

    @Test
    void prefersMavenWrapperWhenPresent() throws Exception {
        Path project = Files.createTempDirectory("eyecode-maven-wrapper");
        Path wrapper = Files.createFile(project.resolve("mvnw.cmd"));
        BuildToolExecutableResolver resolver = windowsResolver(Map.of());

        List<String> command = resolver.mavenCommand(project, "test");

        assertEquals(List.of("cmd", "/c", wrapper.toString(), "test"), command);
    }

    @Test
    void prefersGradleWrapperWhenPresent() throws Exception {
        Path project = Files.createTempDirectory("eyecode-gradle-wrapper");
        Path wrapper = Files.createFile(project.resolve("gradlew.bat"));
        BuildToolExecutableResolver resolver = windowsResolver(Map.of());

        List<String> command = resolver.gradleCommand(project, "test");

        assertEquals(List.of("cmd", "/c", wrapper.toString(), "test"), command);
    }

    @Test
    void fallsBackToMavenOnPathWhenWrapperIsAbsent() throws Exception {
        Path tools = Files.createTempDirectory("eyecode-maven-path");
        Path maven = Files.createFile(tools.resolve("mvn.cmd"));
        BuildToolExecutableResolver resolver = windowsResolver(Map.of("PATH", tools.toString()));

        List<String> command = resolver.mavenCommand(Files.createTempDirectory("eyecode-maven-project"), "verify");

        assertEquals(List.of("cmd", "/c", maven.toString(), "verify"), command);
    }

    @Test
    void fallsBackToGradleOnPathWhenWrapperIsAbsent() throws Exception {
        Path tools = Files.createTempDirectory("eyecode-gradle-path");
        Path gradle = Files.createFile(tools.resolve("gradle.bat"));
        BuildToolExecutableResolver resolver = windowsResolver(Map.of("PATH", tools.toString()));

        List<String> command = resolver.gradleCommand(Files.createTempDirectory("eyecode-gradle-project"), "build");

        assertEquals(List.of("cmd", "/c", gradle.toString(), "build"), command);
    }

    @Test
    void reportsUsefulErrorWhenBuildToolCannotBeResolved() throws Exception {
        BuildToolExecutableResolver resolver = windowsResolver(Map.of());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> resolver.mavenCommand(Files.createTempDirectory("eyecode-missing-maven"), "test"));

        assertTrue(error.getMessage().contains("Maven could not be found"));
        assertTrue(error.getMessage().contains("mvnw.cmd"));
    }

    private BuildToolExecutableResolver windowsResolver(Map<String, String> environment) {
        return new BuildToolExecutableResolver(environment, "Windows 11");
    }
}