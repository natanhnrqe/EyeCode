package com.eyecode.runtime;

import com.eyecode.project.model.ProjectModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

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
        Files.writeString(maven.resolve("pom.xml"), "<project><artifactId>spring-boot-starter</artifactId></project>");
        ResolvedExecution mavenExecution = new ProjectExecutionResolver().resolve(ProjectModel.fromDirectory(maven.toFile()));
        assertEquals(ResolvedExecution.Kind.SPRING_MAVEN, mavenExecution.kind());
        assertTrue(mavenExecution.commands().getFirst().stream().anyMatch(value -> value.contains("spring-boot:run")));

        Path gradle = Files.createTempDirectory("eyecode-run-gradle");
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
        IllegalArgumentException ambiguous = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> new ProjectExecutionResolver().resolve(ProjectModel.fromDirectory(root.toFile())));
        assertTrue(ambiguous.getMessage().contains("Multiple main classes"));
    }
}
