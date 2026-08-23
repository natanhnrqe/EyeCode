package com.eyecode.runtime;

import com.eyecode.project.model.ProjectModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunConfigurationDiscoveryServiceTest {
    @Test
    void discoversPackageAwareMainAndIgnoresCommentsAndStrings() throws Exception {
        Path root = Files.createTempDirectory("eyecode-config-discovery");
        Path source = root.resolve("src/main/java/demo");
        Files.createDirectories(source);
        Files.writeString(source.resolve("Launcher.java"), """
                package demo;
                class Launcher {
                    String fake = \"public static void main(String[] args) {}\";
                    // public static void main(String[] args) {}
                    public static void main(String[] args) {}
                }
                """);

        var configurations = new RunConfigurationDiscoveryService().discover(ProjectModel.fromDirectory(root.toFile()));
        assertEquals(1, configurations.size());
        assertEquals("java:demo.Launcher", configurations.getFirst().id());
        assertEquals("demo.Launcher", configurations.getFirst().mainClass());
    }

    @Test
    void classifiesSpringApplicationWithoutDuplicatingGenericMain() throws Exception {
        Path root = Files.createTempDirectory("eyecode-config-spring");
        Path source = root.resolve("src/main/java/demo");
        Files.createDirectories(source);
        Files.writeString(root.resolve("pom.xml"), "spring-boot");
        Files.writeString(source.resolve("Application.java"), """
                package demo;
                @SpringBootApplication
                public class Application {
                    public static void main(String[] args) {}
                }
                """);

        var configurations = new RunConfigurationDiscoveryService().discover(ProjectModel.fromDirectory(root.toFile()));
        assertEquals(1, configurations.size());
        assertEquals(RunConfigurationKind.SPRING_BOOT, configurations.getFirst().kind());
        assertTrue(configurations.getFirst().id().startsWith("spring:"));
    }
}
