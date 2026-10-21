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
        BuildToolExecutableResolver resolver = resolver(Map.of(), "Windows 11",
                Files.createTempDirectory("eyecode-no-tools"), Map.of());

        List<String> command = resolver.mavenCommand(project, "test");

        assertEquals(List.of("cmd", "/c", wrapper.toString(), "test"), command);
    }

    @Test
    void prefersGradleWrapperWhenPresent() throws Exception {
        Path project = Files.createTempDirectory("eyecode-gradle-wrapper");
        Path wrapper = Files.createFile(project.resolve("gradlew.bat"));
        BuildToolExecutableResolver resolver = resolver(Map.of(), "Windows 11",
                Files.createTempDirectory("eyecode-no-tools"), Map.of());

        List<String> command = resolver.gradleCommand(project, "test");

        assertEquals(List.of("cmd", "/c", wrapper.toString(), "test"), command);
    }

    @Test
    void fallsBackToMavenOnPathWhenWrapperIsAbsent() throws Exception {
        Path tools = Files.createTempDirectory("eyecode-maven-path");
        Path maven = Files.createFile(tools.resolve("mvn.cmd"));
        BuildToolExecutableResolver resolver = resolver(Map.of("PATH", tools.toString()), "Windows 11",
                Files.createTempDirectory("eyecode-no-tools"), Map.of());

        List<String> command = resolver.mavenCommand(Files.createTempDirectory("eyecode-maven-project"), "verify");

        assertEquals(List.of("cmd", "/c", maven.toString(), "verify"), command);
    }

    @Test
    void fallsBackToGradleOnPathWhenWrapperIsAbsent() throws Exception {
        Path tools = Files.createTempDirectory("eyecode-gradle-path");
        Path gradle = Files.createFile(tools.resolve("gradle.bat"));
        BuildToolExecutableResolver resolver = resolver(Map.of("PATH", tools.toString()), "Windows 11",
                Files.createTempDirectory("eyecode-no-tools"), Map.of());

        List<String> command = resolver.gradleCommand(Files.createTempDirectory("eyecode-gradle-project"), "build");

        assertEquals(List.of("cmd", "/c", gradle.toString(), "build"), command);
    }

    @Test
    void reportsUsefulErrorWhenBuildToolCannotBeResolved() throws Exception {
        BuildToolExecutableResolver resolver = resolver(Map.of(), "Windows 11",
                Files.createTempDirectory("eyecode-no-tools"), Map.of());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> resolver.mavenCommand(Files.createTempDirectory("eyecode-missing-maven"), "test"));

        assertTrue(error.getMessage().contains("Maven could not be found"));
        assertTrue(error.getMessage().contains("mvnw.cmd"));
    }

    @Test
    void prefersWrapperOverMavenHomeOverride() throws Exception {
        Path project = Files.createTempDirectory("eyecode-maven-wrapper");
        Path wrapper = Files.createFile(project.resolve("mvnw.cmd"));
        Path overrideHome = Files.createTempDirectory("eyecode-override-home");
        createMavenHome(overrideHome);
        BuildToolExecutableResolver resolver = resolver(Map.of(), "Windows 11", project,
                Map.of(BuildToolExecutableResolver.MAVEN_HOME_PROPERTY, overrideHome.toString()));

        List<String> command = resolver.mavenCommand(project, "test");

        assertEquals(List.of("cmd", "/c", wrapper.toString(), "test"), command);
    }

    @Test
    void usesMavenHomeOverridePropertyWhenPresent() throws Exception {
        Path overrideHome = Files.createTempDirectory("eyecode-override-home");
        Path mvn = createMavenHome(overrideHome);
        BuildToolExecutableResolver resolver = resolver(Map.of(), "Windows 11",
                Files.createTempDirectory("eyecode-userdir"),
                Map.of(BuildToolExecutableResolver.MAVEN_HOME_PROPERTY, overrideHome.toString()));

        List<String> command = resolver.mavenCommand(Files.createTempDirectory("eyecode-project"), "verify");

        assertEquals(List.of("cmd", "/c", mvn.toString(), "verify"), command);
    }

    @Test
    void usesMavenHomeOverrideEnvWhenPropertyAbsent() throws Exception {
        Path overrideHome = Files.createTempDirectory("eyecode-override-home");
        Path mvn = createMavenHome(overrideHome);
        BuildToolExecutableResolver resolver = resolver(
                Map.of(BuildToolExecutableResolver.MAVEN_HOME_ENV, overrideHome.toString()),
                "Windows 11", Files.createTempDirectory("eyecode-userdir"), Map.of());

        List<String> command = resolver.mavenCommand(Files.createTempDirectory("eyecode-project"), "test");

        assertEquals(List.of("cmd", "/c", mvn.toString(), "test"), command);
    }

    @Test
    void overrideBeatsBundledAndPath() throws Exception {
        Path overrideHome = Files.createTempDirectory("eyecode-override-home");
        Path overrideMvn = createMavenHome(overrideHome);
        Path userDirectory = Files.createTempDirectory("eyecode-userdir");
        createMavenHome(userDirectory.resolve(BuildToolExecutableResolver.STAGED_MAVEN_RELATIVE));
        Path pathTools = Files.createTempDirectory("eyecode-path-tools");
        Files.createFile(pathTools.resolve("mvn.cmd"));
        BuildToolExecutableResolver resolver = resolver(
                Map.of("PATH", pathTools.toString(),
                        BuildToolExecutableResolver.MAVEN_HOME_ENV, overrideHome.toString()),
                "Windows 11", userDirectory, Map.of());

        List<String> command = resolver.mavenCommand(Files.createTempDirectory("eyecode-project"), "test");

        assertEquals(List.of("cmd", "/c", overrideMvn.toString(), "test"), command);
    }

    @Test
    void usesBundledMavenWhenOverrideAndPathAreAbsent() throws Exception {
        Path userDirectory = Files.createTempDirectory("eyecode-userdir");
        Path bundledMvn = createMavenHome(userDirectory.resolve(BuildToolExecutableResolver.STAGED_MAVEN_RELATIVE));
        BuildToolExecutableResolver resolver = resolver(Map.of(), "Windows 11", userDirectory, Map.of());

        List<String> command = resolver.mavenCommand(Files.createTempDirectory("eyecode-project"), "test");

        assertEquals(List.of("cmd", "/c", bundledMvn.toString(), "test"), command);
    }

    @Test
    void bundledMavenBeatsSystemPath() throws Exception {
        Path userDirectory = Files.createTempDirectory("eyecode-userdir");
        Path bundledMvn = createMavenHome(userDirectory.resolve(BuildToolExecutableResolver.STAGED_MAVEN_RELATIVE));
        Path pathTools = Files.createTempDirectory("eyecode-path-tools");
        Files.createFile(pathTools.resolve("mvn.cmd"));
        BuildToolExecutableResolver resolver = resolver(Map.of("PATH", pathTools.toString()),
                "Windows 11", userDirectory, Map.of());

        List<String> command = resolver.mavenCommand(Files.createTempDirectory("eyecode-project"), "test");

        assertEquals(List.of("cmd", "/c", bundledMvn.toString(), "test"), command);
    }

    @Test
    void invalidBundledMavenHomeFallsThroughToPath() throws Exception {
        Path userDirectory = Files.createTempDirectory("eyecode-userdir");
        Files.createDirectories(userDirectory.resolve(BuildToolExecutableResolver.STAGED_MAVEN_RELATIVE));
        Path pathTools = Files.createTempDirectory("eyecode-path-tools");
        Path systemMvn = Files.createFile(pathTools.resolve("mvn.cmd"));
        BuildToolExecutableResolver resolver = resolver(Map.of("PATH", pathTools.toString()),
                "Windows 11", userDirectory, Map.of());

        List<String> command = resolver.mavenCommand(Files.createTempDirectory("eyecode-project"), "test");

        assertEquals(List.of("cmd", "/c", systemMvn.toString(), "test"), command);
    }

    @Test
    void invalidOverrideFallsThroughToBundled() throws Exception {
        Path userDirectory = Files.createTempDirectory("eyecode-userdir");
        Path bundledMvn = createMavenHome(userDirectory.resolve(BuildToolExecutableResolver.STAGED_MAVEN_RELATIVE));
        Path bogus = Files.createTempDirectory("eyecode-bogus-home");
        BuildToolExecutableResolver resolver = resolver(Map.of(), "Windows 11", userDirectory,
                Map.of(BuildToolExecutableResolver.MAVEN_HOME_PROPERTY, bogus.toString()));

        List<String> command = resolver.mavenCommand(Files.createTempDirectory("eyecode-project"), "test");

        assertEquals(List.of("cmd", "/c", bundledMvn.toString(), "test"), command);
    }

    @Test
    void gradleResolutionIsNotAffectedByTheBundledMavenTier() throws Exception {
        Path userDirectory = Files.createTempDirectory("eyecode-userdir");
        createMavenHome(userDirectory.resolve(BuildToolExecutableResolver.STAGED_MAVEN_RELATIVE));
        Path pathTools = Files.createTempDirectory("eyecode-path-tools");
        Path gradle = Files.createFile(pathTools.resolve("gradle.bat"));
        BuildToolExecutableResolver resolver = resolver(Map.of("PATH", pathTools.toString()),
                "Windows 11", userDirectory, Map.of());

        List<String> command = resolver.gradleCommand(Files.createTempDirectory("eyecode-project"), "build");

        assertEquals(List.of("cmd", "/c", gradle.toString(), "build"), command);
    }

    @Test
    void unresolvableMavenReportsUsefulErrorEvenWithBundledTier() throws Exception {
        Path userDirectory = Files.createTempDirectory("eyecode-userdir");
        BuildToolExecutableResolver resolver = resolver(Map.of(), "Windows 11", userDirectory, Map.of());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> resolver.mavenCommand(Files.createTempDirectory("eyecode-project"), "test"));

        assertTrue(error.getMessage().contains("Maven could not be found"));
    }

    private static Path createMavenHome(Path home) throws Exception {
        Path bin = Files.createDirectories(home.resolve("bin"));
        return Files.createFile(bin.resolve("mvn.cmd"));
    }

    private BuildToolExecutableResolver resolver(Map<String, String> environment, String operatingSystem,
                                                 Path userDirectory, Map<String, String> systemProperties) {
        return new BuildToolExecutableResolver(environment, operatingSystem, userDirectory, systemProperties);
    }
}