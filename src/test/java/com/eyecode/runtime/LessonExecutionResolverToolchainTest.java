package com.eyecode.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LessonExecutionResolverToolchainTest {

    @TempDir
    Path jdkHome;

    @Test
    void lessonUsesAbsoluteToolchainExecutables() throws Exception {
        Files.createDirectories(jdkHome.resolve("bin"));
        Files.createFile(jdkHome.resolve("bin/java.exe"));
        Files.createFile(jdkHome.resolve("bin/javac.exe"));

        JavaToolchainResolver toolchain = new JavaToolchainResolver(
                Map.of(JavaToolchainResolver.JDK_HOME_ENV, jdkHome.toString()),
                Map.of(), jdkHome.getParent(), true);
        LessonExecutionResolver resolver = new LessonExecutionResolver(toolchain);

        ResolvedExecution execution = resolver.resolve(new LessonRunRequest(
                List.of(new LessonRunRequest.SourceFile("Main.java",
                        "public class Main { public static void main(String[] args) {} }")),
                "Main"));

        List<String> compile = execution.commands().getFirst();
        List<String> launch = execution.commands().get(1);
        assertEquals(jdkHome.resolve("bin/javac.exe").toAbsolutePath().normalize().toString(), compile.getFirst());
        assertEquals(jdkHome.resolve("bin/java.exe").toAbsolutePath().normalize().toString(), launch.getFirst());
    }

    @Test
    void missingToolchainProducesActionableDiagnostic() {
        JavaToolchainResolver toolchain = new JavaToolchainResolver(
                Map.of(), Map.of(), jdkHome.getParent(), true);
        LessonExecutionResolver resolver = new LessonExecutionResolver(toolchain);

        LessonRunRequest request = new LessonRunRequest(
                List.of(new LessonRunRequest.SourceFile("Main.java",
                        "public class Main { public static void main(String[] args) {} }")),
                "Main");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> resolver.resolve(request));
        assertTrue(error.getMessage().contains("No usable Java development toolchain"));
    }
}
