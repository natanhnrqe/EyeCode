package com.eyecode.runtime;

import com.eyecode.project.model.ProjectModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestrictedPathToolchainSmokeTest {

    @TempDir
    Path projectRoot;

    @TempDir
    Path isolatedJdk;

    @Test
    void compilesAndRunsWithAbsoluteToolchainWhenHostEnvIsUnreliable() throws Exception {
        Path source = projectRoot.resolve("src");
        Files.createDirectories(source);
        Files.writeString(source.resolve("Hello.java"),
                "public class Hello { public static void main(String[] args) { System.out.println(\"toolchain-ok\"); } }");

        Path jdkSource = stagedOrHostJdk();
        copyTree(jdkSource, isolatedJdk);

        JavaToolchainResolver toolchain = new JavaToolchainResolver(
                Map.of(JavaToolchainResolver.JDK_HOME_ENV, isolatedJdk.toString()),
                Map.of(), projectRoot, true);
        ProjectExecutionResolver resolver = new ProjectExecutionResolver(new BuildToolExecutableResolver(), toolchain);
        ProjectModel project = ProjectModel.fromDirectory(projectRoot.toFile());
        RunConfiguration configuration = new RunConfiguration(
                "hello", "Hello", RunConfigurationKind.JAVA_APPLICATION, projectRoot, "Hello");
        ResolvedExecution execution = resolver.resolve(project, configuration);

        Path java = Path.of(execution.commands().get(1).getFirst());
        Path javac = Path.of(execution.commands().getFirst().getFirst());
        assertTrue(Files.isRegularFile(java), "absolute java expected: " + java);
        assertTrue(Files.isRegularFile(javac), "absolute javac expected: " + javac);
        assertTrue(java.startsWith(isolatedJdk.toAbsolutePath().normalize()), java.toString());
        assertTrue(javac.startsWith(isolatedJdk.toAbsolutePath().normalize()), javac.toString());

        StringBuilder output = new StringBuilder();
        AtomicInteger exit = new AtomicInteger(-1);
        CountDownLatch done = new CountDownLatch(1);
        RunSession session = new RunSession(execution, projectRoot, new RunSession.Listener() {
            @Override
            public void onOutput(String text, boolean error) {
                synchronized (output) {
                    output.append(text);
                }
            }

            @Override
            public void onFinished(int exitCode, boolean stopped) {
                exit.set(exitCode);
                done.countDown();
            }
        });
        session.start();
        assertTrue(done.await(90, TimeUnit.SECONDS), "session did not finish");
        assertEquals(0, exit.get(), "output: " + output);
        assertTrue(output.toString().contains("toolchain-ok"), "output: " + output);
    }

    private Path stagedOrHostJdk() {
        Path staged = Path.of(System.getProperty("user.dir", "."))
                .toAbsolutePath().normalize()
                .resolve("target/eyecode-desktop/runtime/jdk");
        if (Files.isRegularFile(staged.resolve("bin").resolve("java.exe"))
                || Files.isRegularFile(staged.resolve("bin").resolve("java"))) {
            return staged;
        }
        return Path.of(System.getProperty("java.home")).toAbsolutePath().normalize();
    }

    private static void copyTree(Path source, Path target) throws Exception {
        try (var paths = Files.walk(source)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination);
                }
            }
        }
    }
}
