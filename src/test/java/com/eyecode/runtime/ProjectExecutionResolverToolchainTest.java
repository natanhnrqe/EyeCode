package com.eyecode.runtime;

import com.eyecode.project.model.ProjectModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectExecutionResolverToolchainTest {

    @TempDir
    Path projectRoot;

    @TempDir
    Path jdkHome;

    @Test
    void standardJavaUsesAbsoluteToolchainExecutables() throws Exception {
        Path source = projectRoot.resolve("src");
        Files.createDirectories(source);
        fakeJdk();
        Files.writeString(source.resolve("Main.java"),
                "public class Main { public static void main(String[] args) { System.out.println(\"ok\"); } }");

        ProjectModel project = ProjectModel.fromDirectory(projectRoot.toFile());
        RunConfiguration configuration = new RunConfiguration(
                "main", "Main", RunConfigurationKind.JAVA_APPLICATION, projectRoot, "Main");

        JavaToolchainResolver toolchain = new JavaToolchainResolver(
                Map.of(JavaToolchainResolver.JDK_HOME_ENV, jdkHome.toString()),
                Map.of(), projectRoot, true);
        ProjectExecutionResolver resolver = new ProjectExecutionResolver(new BuildToolExecutableResolver(), toolchain);

        ResolvedExecution execution = resolver.resolve(project, configuration);

        List<String> compile = execution.commands().getFirst();
        List<String> launch = execution.commands().get(1);
        assertEquals(jdkHome.resolve("bin/javac.exe").toAbsolutePath().normalize().toString(), compile.getFirst());
        assertEquals(jdkHome.resolve("bin/java.exe").toAbsolutePath().normalize().toString(), launch.getFirst());
        assertTrue(execution.environment().isEmpty());
    }

    @Test
    void gradleExecutionCarriesToolchainEnvironment() throws Exception {
        Path wrapper = projectRoot.resolve("gradlew.bat");
        Files.writeString(wrapper, "@echo off\r\nexit /b 0\r\n");
        Files.writeString(projectRoot.resolve("build.gradle"),
                "plugins { id 'java' }\n");
        fakeJdk();

        ProjectModel project = ProjectModel.fromDirectory(projectRoot.toFile());
        RunConfiguration configuration = new RunConfiguration(
                "main", "Main", RunConfigurationKind.JAVA_APPLICATION, projectRoot, "Main");

        JavaToolchainResolver toolchain = new JavaToolchainResolver(
                Map.of(JavaToolchainResolver.JDK_HOME_ENV, jdkHome.toString()),
                Map.of(), projectRoot, true);
        ProjectExecutionResolver resolver = new ProjectExecutionResolver(new BuildToolExecutableResolver(), toolchain);

        ResolvedExecution execution = resolver.resolve(project, configuration);

        assertEquals(ResolvedExecution.Kind.GRADLE, execution.kind());
        assertEquals(jdkHome.toAbsolutePath().normalize().toString(), execution.environment().get("JAVA_HOME"));
        assertTrue(execution.environment().get("PATH")
                .startsWith(jdkHome.resolve("bin").toAbsolutePath().normalize().toString()));
    }

    @Test
    void springMavenExecutionCarriesToolchainEnvironment() throws Exception {
        Files.writeString(projectRoot.resolve("pom.xml"),
                "<project><artifactId>spring-boot-starter</artifactId></project>");
        fakeJdk();

        ProjectModel project = ProjectModel.fromDirectory(projectRoot.toFile());
        RunConfiguration configuration = new RunConfiguration(
                "main", "Main", RunConfigurationKind.SPRING_BOOT, projectRoot, "Main");

        JavaToolchainResolver toolchain = new JavaToolchainResolver(
                Map.of(JavaToolchainResolver.JDK_HOME_ENV, jdkHome.toString()),
                Map.of(), projectRoot, true);
        ProjectExecutionResolver resolver = new ProjectExecutionResolver(new BuildToolExecutableResolver(), toolchain);

        ResolvedExecution execution = resolver.resolve(project, configuration);

        assertEquals(ResolvedExecution.Kind.SPRING_MAVEN, execution.kind());
        assertEquals(jdkHome.toAbsolutePath().normalize().toString(), execution.environment().get("JAVA_HOME"));
    }

    private void fakeJdk() throws Exception {
        Files.createDirectories(jdkHome.resolve("bin"));
        Files.createFile(jdkHome.resolve("bin/java.exe"));
        Files.createFile(jdkHome.resolve("bin/javac.exe"));
    }
}
