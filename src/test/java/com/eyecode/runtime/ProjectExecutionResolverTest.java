package com.eyecode.runtime;

import com.eyecode.project.model.ProjectModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectExecutionResolverTest {

    @Test
    void resolvesStandardJava() throws Exception {
        Path root = Files.createTempDirectory("eyecode-run-java");
        Path source = root.resolve("src/main/java");
        Files.createDirectories(source);
        Files.writeString(source.resolve("Main.java"), "public class Main { public static void main(String[] a) {} }");

        ResolvedExecution execution = new ProjectExecutionResolver().resolve(ProjectModel.fromDirectory(root.toFile()));
        assertEquals(ResolvedExecution.Kind.STANDARD_JAVA, execution.kind());
        assertEquals("Main", execution.mainClass());
        assertEquals(2, execution.commands().size());
        assertTrue(execution.commands().getFirst().contains("-cp"));
    }

    @Test
    void prioritizesSpringMavenAndSpringGradle() throws Exception {
        Path maven = Files.createTempDirectory("eyecode-run-maven");
        Files.createFile(maven.resolve("mvnw.cmd"));
        Files.writeString(maven.resolve("pom.xml"), "<project><artifactId>spring-boot-starter</artifactId></project>");
        ResolvedExecution mavenExecution = new ProjectExecutionResolver().resolve(ProjectModel.fromDirectory(maven.toFile()));
        assertEquals(ResolvedExecution.Kind.SPRING_MAVEN, mavenExecution.kind());
        assertTrue(mavenExecution.commands().getFirst().stream().anyMatch(value -> value.contains("spring-boot:run")));

        Path gradle = Files.createTempDirectory("eyecode-run-gradle");
        Files.createFile(gradle.resolve("gradlew.bat"));
        Files.writeString(gradle.resolve("build.gradle"), "plugins { id 'org.springframework.boot' version '3.3.0' }");
        ResolvedExecution gradleExecution = new ProjectExecutionResolver().resolve(ProjectModel.fromDirectory(gradle.toFile()));
        assertEquals(ResolvedExecution.Kind.SPRING_GRADLE, gradleExecution.kind());
        assertTrue(gradleExecution.commands().getFirst().stream().anyMatch(value -> value.equals("bootRun")));
    }

    @Test
    void rejectsMissingOrAmbiguousMainClasses() throws Exception {
        Path root = Files.createTempDirectory("eyecode-run-main");
        Path source = root.resolve("src/main/java");
        Files.createDirectories(source);
        ProjectModel project = ProjectModel.fromDirectory(root.toFile());
        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class,
                () -> new ProjectExecutionResolver().resolve(project));
        assertTrue(missing.getMessage().contains("main class"));
        Files.writeString(source.resolve("A.java"), "class A { public static void main(String[] a) {} }");
        Files.writeString(source.resolve("B.java"), "class B { public static void main(String[] a) {} }");
        IllegalArgumentException ambiguous = assertThrows(IllegalArgumentException.class,
                () -> new ProjectExecutionResolver().resolve(ProjectModel.fromDirectory(root.toFile())));
        assertTrue(ambiguous.getMessage().contains("Multiple main classes"));
    }

    @Test
    void springExecutionsUseTheSameBuildToolResolution() throws Exception {
        BuildToolExecutableResolver toolsResolver = new BuildToolExecutableResolver(Map.of(), "Windows 11");
        ProjectExecutionResolver resolver = new ProjectExecutionResolver(toolsResolver);

        Path maven = Files.createTempDirectory("eyecode-spring-maven-wrapper");
        Files.createFile(maven.resolve("mvnw.cmd"));
        Files.writeString(maven.resolve("pom.xml"), "<project>spring-boot</project>");
        ResolvedExecution springMaven = resolver.resolve(ProjectModel.fromDirectory(maven.toFile()));
        assertEquals(List.of("cmd", "/c", maven.resolve("mvnw.cmd").toString(), "spring-boot:run"),
                springMaven.commands().getFirst());

        Path gradle = Files.createTempDirectory("eyecode-spring-gradle-wrapper");
        Files.createFile(gradle.resolve("gradlew.bat"));
        Files.writeString(gradle.resolve("build.gradle"), "plugins { id 'org.springframework.boot' }");
        ResolvedExecution springGradle = resolver.resolve(ProjectModel.fromDirectory(gradle.toFile()));
        assertEquals(List.of("cmd", "/c", gradle.resolve("gradlew.bat").toString(), "bootRun"),
                springGradle.commands().getFirst());
    }
}