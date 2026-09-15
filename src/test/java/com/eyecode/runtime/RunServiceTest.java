package com.eyecode.runtime;

import com.eyecode.project.ProjectLifecycleService;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunServiceTest {

    @Test
    void independentBrokenSourceDoesNotBlockSelectedMain() throws Exception {
        Path root = createJavaProject();
        Files.writeString(root.resolve("src/main/java/Main.java"),
                "public class Main { public static void main(String[] args) { System.out.println(\"MAIN_RAN\"); } }");
        Files.writeString(root.resolve("src/main/java/Broken.java"), "class Broken { this is not Java }");

        ExecutionResult result = execute(root, null);

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("MAIN_RAN"));
    }

    @Test
    void validSourceDependencyIsResolvedThroughSourcepath() throws Exception {
        Path root = createJavaProject();
        Files.writeString(root.resolve("src/main/java/Main.java"),
                "public class Main { public static void main(String[] args) { System.out.println(Helper.message()); } }");
        Files.writeString(root.resolve("src/main/java/Helper.java"),
                "class Helper { static String message() { return \"helper\"; } }");

        ExecutionResult result = execute(root, null);

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("helper"));
    }

    @Test
    void brokenSourceDependencyStillFailsCompilation() throws Exception {
        Path root = createJavaProject();
        Files.writeString(root.resolve("src/main/java/Main.java"),
                "public class Main { public static void main(String[] args) { System.out.println(Helper.message()); } }");
        Files.writeString(root.resolve("src/main/java/Helper.java"), "class Helper { static String message() { return ; }");

        ExecutionResult result = execute(root, null);

        assertTrue(result.exitCode() != 0);
        assertTrue(!result.output().contains("MAIN_RAN"));
    }

    @Test
    void packageEntryAndDependencyAreResolvedFromSourceRoot() throws Exception {
        Path root = createJavaProject();
        Path source = root.resolve("src/main/java/com/foo");
        Files.createDirectories(source);
        Files.writeString(source.resolve("Main.java"), """
                package com.foo;
                public class Main {
                    public static void main(String[] args) { System.out.println(Helper.message()); }
                }
                """);
        Files.writeString(source.resolve("Helper.java"), """
                package com.foo;
                class Helper { static String message() { return "package"; } }
                """);

        ExecutionResult result = execute(root, "java:com.foo.Main");

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("package"));
    }

    @Test
    void selectedMainIsNotBlockedByBrokenIndependentSecondMain() throws Exception {
        Path root = createJavaProject();
        Files.writeString(root.resolve("src/main/java/MainA.java"),
                "public class MainA { public static void main(String[] args) { System.out.println(\"main-a\"); } }");
        Files.writeString(root.resolve("src/main/java/MainB.java"),
                "class MainB { public static void main(String[] args) { this is not Java }");

        ExecutionResult result = execute(root, "java:MainA");

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("main-a"));
    }

    @Test
    void selectedMainKeepsLibrariesOnTheClasspath() throws Exception {
        Path root = createJavaProject();
        Path librarySource = Files.createTempDirectory("eyecode-library-source");
        Path libraryJava = librarySource.resolve("support/Library.java");
        Files.createDirectories(libraryJava.getParent());
        Files.writeString(libraryJava, "package support; public final class Library { public static String value() { return \"library\"; } }");
        Path libraryClasses = Files.createTempDirectory("eyecode-library-classes");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertTrue(compiler != null);
        assertEquals(0, compiler.run(null, null, null, "-d", libraryClasses.toString(), libraryJava.toString()));
        Path libs = root.resolve("libs");
        Files.createDirectories(libs);
        writeJar(libraryClasses, libs.resolve("support.jar"));
        Files.writeString(root.resolve("src/main/java/Main.java"),
                "import support.Library; public class Main { public static void main(String[] args) { System.out.println(Library.value()); } }");

        ExecutionResult result = execute(root, null);

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("library"));
    }

    @Test
    void streamsOutputAndRerunsTheSameRequest() throws Exception {
        Path root = Files.createTempDirectory("eyecode-run-service");
        Path source = root.resolve("src/main/java");
        Files.createDirectories(source);
        Files.writeString(source.resolve("Main.java"), "public class Main { public static void main(String[] a) { System.out.println(\"hello\"); System.err.println(\"warning\"); } }");
        ProjectLifecycleService lifecycle = new ProjectLifecycleService();
        var project = lifecycle.open(root);
        RunService service = new RunService(lifecycle);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<String> output = new AtomicReference<>("");
        AtomicInteger clears = new AtomicInteger();
        AtomicInteger runs = new AtomicInteger();
        service.addListener(new RunService.Listener() {
            @Override public void onStarted(RunRequest request) { runs.incrementAndGet(); }
            @Override public void onOutput(String text, boolean error) {
                if (text == null) {
                    clears.incrementAndGet();
                    output.set("");
                } else {
                    output.updateAndGet(value -> value + text);
                }
            }
            @Override public void onFinished(int exitCode, boolean stopped) { finished.countDown(); }
        });

        assertTrue(service.runCurrent());
        assertTrue(finished.await(20, TimeUnit.SECONDS));
        assertTrue(output.get().contains("hello"));
        assertTrue(output.get().contains("warning"));
        assertFalse(service.isRunning());

        CountDownLatch rerunFinished = new CountDownLatch(1);
        service.addListener(new RunService.Listener() {
            @Override public void onStarted(RunRequest request) { }
            @Override public void onOutput(String line, boolean error) { }
            @Override public void onFinished(int exitCode, boolean stopped) { rerunFinished.countDown(); }
        });
        assertTrue(service.rerun());
        assertTrue(rerunFinished.await(20, TimeUnit.SECONDS));
        assertEquals(2, runs.get());
        assertEquals(2, clears.get());
        service.dispose();
    }

    @Test
    void publishesCompilationAndRunningPhasesForJavaApplication() throws Exception {
        Path root = createJavaProject();
        Files.writeString(root.resolve("src/main/java/Main.java"),
                "public class Main { public static void main(String[] args) { System.out.println(\"PHASES\"); } }");
        ProjectLifecycleService lifecycle = new ProjectLifecycleService();
        lifecycle.open(root);
        RunService service = new RunService(lifecycle);
        CountDownLatch finished = new CountDownLatch(1);
        var phases = new java.util.concurrent.CopyOnWriteArrayList<RunPhase>();
        service.addListener(new RunService.Listener() {
            @Override public void onPhase(RunPhase phase) { phases.add(phase); }
            @Override public void onStarted(RunRequest request) { }
            @Override public void onOutput(String text, boolean error) { }
            @Override public void onFinished(int exitCode, boolean stopped) { finished.countDown(); }
        });

        assertTrue(service.runCurrent());
        assertTrue(finished.await(20, TimeUnit.SECONDS));
        assertTrue(phases.indexOf(RunPhase.PREPARING) >= 0);
        assertTrue(phases.indexOf(RunPhase.COMPILING) > phases.indexOf(RunPhase.PREPARING));
        assertTrue(phases.indexOf(RunPhase.RUNNING) > phases.indexOf(RunPhase.COMPILING));
        assertEquals(RunPhase.IDLE, service.phase());
        service.dispose();
    }

    @Test
    void compilationFailureDoesNotPublishRunningPhase() throws Exception {
        Path root = createJavaProject();
        Files.writeString(root.resolve("src/main/java/Main.java"),
                "public class Main { public static void main(String[] args) { Broken.value(); } }");
        Files.writeString(root.resolve("src/main/java/Broken.java"), "class Broken { this is not Java }");
        ProjectLifecycleService lifecycle = new ProjectLifecycleService();
        lifecycle.open(root);
        RunService service = new RunService(lifecycle);
        CountDownLatch finished = new CountDownLatch(1);
        var phases = new java.util.concurrent.CopyOnWriteArrayList<RunPhase>();
        service.addListener(new RunService.Listener() {
            @Override public void onPhase(RunPhase phase) { phases.add(phase); }
            @Override public void onStarted(RunRequest request) { }
            @Override public void onOutput(String text, boolean error) { }
            @Override public void onFinished(int exitCode, boolean stopped) { finished.countDown(); }
        });

        assertTrue(service.runCurrent());
        assertTrue(finished.await(20, TimeUnit.SECONDS));
        assertTrue(phases.contains(RunPhase.COMPILING));
        assertFalse(phases.contains(RunPhase.RUNNING));
        assertEquals(RunPhase.IDLE, service.phase());
        service.dispose();
    }

    @Test
    void acceptsRunBeforePreparationResolverCompletes() throws Exception {
        Path root = createJavaProject();
        Path source = root.resolve("src/main/java/Main.java");
        Files.writeString(source, "public class Main { public static void main(String[] args) { } }");
        Path output = root.resolve("out");
        CountDownLatch resolverEntered = new CountDownLatch(1);
        CountDownLatch releaseResolver = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        var phases = new java.util.concurrent.CopyOnWriteArrayList<RunPhase>();
        ProjectLifecycleService lifecycle = new ProjectLifecycleService();
        lifecycle.open(root);
        RunService service = new RunService(lifecycle, (project, configuration) -> {
            resolverEntered.countDown();
            try {
                releaseResolver.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            return new ResolvedExecution(ResolvedExecution.Kind.STANDARD_JAVA,
                    List.of(List.of("javac", "-d", output.toString(), source.toString()),
                            List.of("java", "-cp", output.toString(), "Main")),
                    "Main", List.of(RunPhase.COMPILING, RunPhase.RUNNING));
        });
        service.addListener(new RunService.Listener() {
            @Override public void onPhase(RunPhase phase) { phases.add(phase); }
            @Override public void onStarted(RunRequest request) { }
            @Override public void onOutput(String text, boolean error) { }
            @Override public void onFinished(int exitCode, boolean stopped) { finished.countDown(); }
        });
        var caller = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var accepted = caller.submit(service::runCurrent);
            assertTrue(resolverEntered.await(5, TimeUnit.SECONDS));
            assertTrue(accepted.isDone());
            assertTrue(accepted.get());
            assertEquals(RunPhase.PREPARING, service.phase());
            releaseResolver.countDown();
            assertTrue(finished.await(20, TimeUnit.SECONDS));
            assertTrue(phases.indexOf(RunPhase.COMPILING) > phases.indexOf(RunPhase.PREPARING));
            assertTrue(phases.indexOf(RunPhase.RUNNING) > phases.indexOf(RunPhase.COMPILING));
        } finally {
            releaseResolver.countDown();
            caller.shutdownNow();
            service.dispose();
        }
    }

    @Test
    void preparationFailureFinishesAfterStartWasAccepted() throws Exception {
        Path root = createJavaProject();
        Files.writeString(root.resolve("src/main/java/Main.java"),
                "public class Main { public static void main(String[] args) { } }");
        CountDownLatch resolverEntered = new CountDownLatch(1);
        CountDownLatch releaseResolver = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<String> output = new AtomicReference<>("");
        ProjectLifecycleService lifecycle = new ProjectLifecycleService();
        lifecycle.open(root);
        RunService service = new RunService(lifecycle, (project, configuration) -> {
            resolverEntered.countDown();
            try {
                releaseResolver.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalArgumentException("PREPARATION_FAILED");
        });
        service.addListener(new RunService.Listener() {
            @Override public void onStarted(RunRequest request) { }
            @Override public void onOutput(String text, boolean error) {
                if (text != null) output.updateAndGet(value -> value + text);
            }
            @Override public void onFinished(int exitCode, boolean stopped) { finished.countDown(); }
        });

        assertTrue(service.runCurrent());
        assertTrue(resolverEntered.await(5, TimeUnit.SECONDS));
        assertEquals(RunPhase.PREPARING, service.phase());
        releaseResolver.countDown();
        assertTrue(finished.await(10, TimeUnit.SECONDS));
        assertEquals(RunPhase.IDLE, service.phase());
        assertFalse(service.isRunning());
        assertTrue(output.get().contains("PREPARATION_FAILED"));
        service.dispose();
    }

    @Test
    void rejectsSecondRunWhilePreparationIsPending() throws Exception {
        Path root = createJavaProject();
        Files.writeString(root.resolve("src/main/java/Main.java"),
                "public class Main { public static void main(String[] args) { } }");
        CountDownLatch resolverEntered = new CountDownLatch(1);
        CountDownLatch releaseResolver = new CountDownLatch(1);
        AtomicInteger resolutions = new AtomicInteger();
        ProjectLifecycleService lifecycle = new ProjectLifecycleService();
        lifecycle.open(root);
        RunService service = new RunService(lifecycle, (project, configuration) -> {
            resolutions.incrementAndGet();
            resolverEntered.countDown();
            try {
                releaseResolver.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            return new ResolvedExecution(ResolvedExecution.Kind.STANDARD_JAVA,
                    List.of(List.of("java", "-version")), "Main", List.of(RunPhase.RUNNING));
        });
        assertTrue(service.runCurrent());
        assertTrue(resolverEntered.await(5, TimeUnit.SECONDS));
        assertFalse(service.runCurrent());
        assertEquals(1, resolutions.get());
        releaseResolver.countDown();
        service.dispose();
    }

    @Test
    void stopDuringPreparationPreventsLaterCommands() throws Exception {
        Path root = createJavaProject();
        Files.writeString(root.resolve("src/main/java/Main.java"),
                "public class Main { public static void main(String[] args) { } }");
        CountDownLatch resolverEntered = new CountDownLatch(1);
        CountDownLatch releaseResolver = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        var phases = new java.util.concurrent.CopyOnWriteArrayList<RunPhase>();
        ProjectLifecycleService lifecycle = new ProjectLifecycleService();
        lifecycle.open(root);
        RunService service = new RunService(lifecycle, (project, configuration) -> {
            resolverEntered.countDown();
            try {
                releaseResolver.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            return new ResolvedExecution(ResolvedExecution.Kind.STANDARD_JAVA,
                    List.of(List.of("java", "-version")), "Main", List.of(RunPhase.RUNNING));
        });
        service.addListener(new RunService.Listener() {
            @Override public void onPhase(RunPhase phase) { phases.add(phase); }
            @Override public void onStarted(RunRequest request) { }
            @Override public void onOutput(String text, boolean error) { }
            @Override public void onFinished(int exitCode, boolean stopped) { finished.countDown(); }
        });

        assertTrue(service.runCurrent());
        assertTrue(resolverEntered.await(5, TimeUnit.SECONDS));
        service.stop();
        assertTrue(finished.await(5, TimeUnit.SECONDS));
        releaseResolver.countDown();
        assertFalse(phases.contains(RunPhase.COMPILING));
        assertFalse(phases.contains(RunPhase.RUNNING));
        assertEquals(RunPhase.IDLE, service.phase());
        service.dispose();
    }

    @Test
    void preservesOutputChunksBlankLinesAndMissingFinalNewline() throws Exception {
        Path root = Files.createTempDirectory("eyecode-run-chunks");
        Path source = root.resolve("src/main/java");
        Files.createDirectories(source);
        Files.writeString(source.resolve("Main.java"), "public class Main { public static void main(String[] a) { System.out.print(\"first\"); System.out.print(\"\\n\\n\"); System.out.print(\"last\"); System.err.print(\"error\"); } }");
        ProjectLifecycleService lifecycle = new ProjectLifecycleService();
        lifecycle.open(root);
        RunService service = new RunService(lifecycle);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<String> standardOutput = new AtomicReference<>("");
        AtomicReference<String> errorOutput = new AtomicReference<>("");
        AtomicReference<Integer> exitCode = new AtomicReference<>();
        service.addListener(new RunService.Listener() {
            @Override public void onStarted(RunRequest request) { }
            @Override public void onOutput(String text, boolean error) {
                if (text == null) return;
                (error ? errorOutput : standardOutput).updateAndGet(value -> value + text);
            }
            @Override public void onFinished(int code, boolean stopped) {
                exitCode.set(code);
                finished.countDown();
            }
        });

        assertTrue(service.runCurrent());
        assertTrue(finished.await(20, TimeUnit.SECONDS));
        assertEquals("first\n\nlast", standardOutput.get());
        assertEquals("error", errorOutput.get());
        assertEquals(0, exitCode.get());
        assertTrue(service.hasCompletion());
        assertEquals(0, service.lastExitCode());
        assertFalse(service.lastStopped());
        service.dispose();
    }

    @Test
    void reportsNonZeroExitCodeAsFinishedError() throws Exception {
        Path root = Files.createTempDirectory("eyecode-run-exit-code");
        Path source = root.resolve("src/main/java");
        Files.createDirectories(source);
        Files.writeString(source.resolve("Main.java"), "public class Main { public static void main(String[] a) { System.exit(3); } }");
        ProjectLifecycleService lifecycle = new ProjectLifecycleService();
        lifecycle.open(root);
        RunService service = new RunService(lifecycle);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Integer> exitCode = new AtomicReference<>();
        AtomicReference<Boolean> stopped = new AtomicReference<>();
        service.addListener(new RunService.Listener() {
            @Override public void onStarted(RunRequest request) { }
            @Override public void onOutput(String text, boolean error) { }
            @Override public void onFinished(int code, boolean wasStopped) {
                exitCode.set(code);
                stopped.set(wasStopped);
                finished.countDown();
            }
        });

        assertTrue(service.runCurrent());
        assertTrue(finished.await(20, TimeUnit.SECONDS));
        assertEquals(3, exitCode.get());
        assertFalse(stopped.get());
        assertEquals(3, service.lastExitCode());
        service.dispose();
    }

    @Test
    void stopsAnActiveProcessAndReportsStopped() throws Exception {
        Path root = Files.createTempDirectory("eyecode-run-stop");
        Path source = root.resolve("src/main/java");
        Files.createDirectories(source);
        Files.writeString(source.resolve("Main.java"),
                "public class Main { public static void main(String[] a) throws Exception { while (true) Thread.sleep(1000); } }");
        ProjectLifecycleService lifecycle = new ProjectLifecycleService();
        lifecycle.open(root);
        RunService service = new RunService(lifecycle);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch runningPhase = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Boolean> stopped = new AtomicReference<>(false);
        var phases = new java.util.concurrent.CopyOnWriteArrayList<RunPhase>();
        service.addListener(new RunService.Listener() {
            @Override public void onPhase(RunPhase phase) {
                phases.add(phase);
                if (phase == RunPhase.RUNNING) runningPhase.countDown();
            }
            @Override public void onStarted(RunRequest request) { started.countDown(); }
            @Override public void onOutput(String line, boolean error) { }
            @Override public void onFinished(int exitCode, boolean wasStopped) {
                stopped.set(wasStopped);
                finished.countDown();
            }
        });

        assertTrue(service.runCurrent());
        assertTrue(started.await(20, TimeUnit.SECONDS));
        assertTrue(runningPhase.await(20, TimeUnit.SECONDS));
        service.stop();
        assertTrue(finished.await(20, TimeUnit.SECONDS));
        assertTrue(stopped.get());
        assertTrue(phases.contains(RunPhase.RUNNING));
        assertEquals(RunPhase.IDLE, service.phase());
        assertFalse(service.isRunning());
        service.dispose();
    }
    @Test
    void projectChangeInvalidatesThePreviousRerunRequest() throws Exception {
        Path firstRoot = Files.createTempDirectory("eyecode-run-first");
        Path firstSource = firstRoot.resolve("src/main/java");
        Files.createDirectories(firstSource);
        Files.writeString(firstSource.resolve("Main.java"),
                "public class Main { public static void main(String[] a) { } }");
        Path secondRoot = Files.createTempDirectory("eyecode-run-second");
        Path secondSource = secondRoot.resolve("src/main/java");
        Files.createDirectories(secondSource);
        Files.writeString(secondSource.resolve("Other.java"),
                "public class Other { public static void main(String[] a) { } }");

        ProjectLifecycleService lifecycle = new ProjectLifecycleService();
        lifecycle.open(firstRoot);
        RunService service = new RunService(lifecycle);
        CountDownLatch finished = new CountDownLatch(1);
        service.addListener(new RunService.Listener() {
            @Override public void onStarted(RunRequest request) { }
            @Override public void onOutput(String line, boolean error) { }
            @Override public void onFinished(int exitCode, boolean stopped) { finished.countDown(); }
        });

        assertTrue(service.runCurrent());
        assertTrue(finished.await(20, TimeUnit.SECONDS));
        assertTrue(service.hasLastRequest());

        lifecycle.open(secondRoot);

        assertFalse(service.hasLastRequest());
        assertFalse(service.rerun());
        service.dispose();
    }

    @Test
    void mavenMainRunsWithoutCompilingAnIndependentBrokenSource() throws Exception {
        Path root = createMavenProject("");
        Files.writeString(root.resolve("src/main/java/Main.java"),
                "public class Main { public static void main(String[] args) { System.out.println(\"MAVEN_MAIN\"); } }");
        Files.writeString(root.resolve("src/main/java/Broken.java"), "class Broken { this is not Java }");

        ExecutionResult result = execute(root, "java:Main");

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("MAVEN_MAIN"));
    }

    @Test
    void mavenMainCompilesARequiredHelper() throws Exception {
        Path root = createMavenProject("");
        Files.writeString(root.resolve("src/main/java/Main.java"),
                "public class Main { public static void main(String[] args) { System.out.println(Helper.value()); } }");
        Files.writeString(root.resolve("src/main/java/Helper.java"),
                "class Helper { static String value() { return \"HELPER\"; } }");

        ExecutionResult result = execute(root, "java:Main");

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("HELPER"));
    }

    @Test
    void mavenMainFailsWhenARequiredHelperIsBroken() throws Exception {
        Path root = createMavenProject("");
        Files.writeString(root.resolve("src/main/java/Main.java"),
                "public class Main { public static void main(String[] args) { System.out.println(Helper.value()); } }");
        Files.writeString(root.resolve("src/main/java/Helper.java"), "class Helper { this is not Java }");

        ExecutionResult result = execute(root, "java:Main");

        assertTrue(result.exitCode() != 0);
        assertTrue(!result.output().contains("MAVEN_MAIN"));
    }

    @Test
    void mavenMainFindsResourcesWithoutRunningTheResourcesPlugin() throws Exception {
        Path root = createMavenProject("");
        Files.createDirectories(root.resolve("src/main/resources"));
        Files.writeString(root.resolve("src/main/resources/example.txt"), "RESOURCE_OK");
        Files.writeString(root.resolve("src/main/java/Main.java"), """
                import java.io.InputStream;
                import java.nio.charset.StandardCharsets;
                public class Main {
                    public static void main(String[] args) throws Exception {
                        try (InputStream input = Main.class.getResourceAsStream("/example.txt")) {
                            System.out.println(new String(input.readAllBytes(), StandardCharsets.UTF_8));
                        }
                    }
                }
                """);

        ExecutionResult result = execute(root, "java:Main");

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("RESOURCE_OK"));
    }

    @Test
    void mavenConfigurationRunsTheSelectedMainWhenAnotherMainIsBroken() throws Exception {
        Path root = createMavenProject("");
        Files.writeString(root.resolve("src/main/java/MainA.java"),
                "public class MainA { public static void main(String[] args) { System.out.println(\"MAIN_A\"); } }");
        Files.writeString(root.resolve("src/main/java/MainB.java"),
                "public class MainB { public static void main(String[] args) { this is not Java } }");

        ExecutionResult result = execute(root, "java:MainA");

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("MAIN_A"));
    }

    @Test
    void mavenClasspathFixtureMakesADependencyAvailableToCompileAndRun() throws Exception {
        Path root = createMavenProject("");
        Path librarySource = Files.createTempDirectory("eyecode-maven-library-source");
        Path libraryJava = librarySource.resolve("support/Library.java");
        Files.createDirectories(libraryJava.getParent());
        Files.writeString(libraryJava,
                "package support; public final class Library { public static String value() { return \"DEPENDENCY_OK\"; } }");
        Path libraryClasses = Files.createTempDirectory("eyecode-maven-library-classes");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertTrue(compiler != null);
        assertEquals(0, compiler.run(null, null, null, "-d", libraryClasses.toString(), libraryJava.toString()));
        Path libraryJar = root.resolve("support.jar");
        writeJar(libraryClasses, libraryJar);
        writeMavenClasspathWrapper(root, libraryJar.toString(), 0);
        Files.writeString(root.resolve("src/main/java/Main.java"),
                "import support.Library; public class Main { public static void main(String[] args) { System.out.println(Library.value()); } }");

        ExecutionResult result = execute(root, "java:Main");

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("DEPENDENCY_OK"));
    }

    @Test
    void mavenClasspathResolutionFailureDoesNotStartJavaWithAnIncompleteClasspath() throws Exception {
        Path root = createMavenProject("");
        writeMavenClasspathWrapper(root, "", 7);
        Files.writeString(root.resolve("src/main/java/Main.java"),
                "public class Main { public static void main(String[] args) { System.out.println(\"SHOULD_NOT_RUN\"); } }");

        ProjectLifecycleService lifecycle = new ProjectLifecycleService();
        lifecycle.open(root);
        RunService service = new RunService(lifecycle);
        AtomicReference<String> output = new AtomicReference<>("");
        CountDownLatch finished = new CountDownLatch(1);
        service.addListener(new RunService.Listener() {
            @Override public void onStarted(RunRequest request) { }
            @Override public void onOutput(String text, boolean error) {
                if (text != null) output.updateAndGet(value -> value + text);
            }
            @Override public void onFinished(int exitCode, boolean stopped) { finished.countDown(); }
        });

        assertTrue(service.runCurrent());
        assertTrue(finished.await(10, TimeUnit.SECONDS));
        assertTrue(output.get().contains("Unable to resolve Maven runtime classpath"));
        assertTrue(!output.get().contains("SHOULD_NOT_RUN"));
        service.dispose();
    }

    private Path createMavenProject(String classpath) throws Exception {
        Path root = Files.createTempDirectory("eyecode-maven-run");
        Files.createDirectories(root.resolve("src/main/java"));
        Files.writeString(root.resolve("pom.xml"), "<project/>");
        writeMavenClasspathWrapper(root, classpath, 0);
        return root;
    }

    private void writeMavenClasspathWrapper(Path root, String classpath, int exitCode) throws Exception {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        Path wrapper = root.resolve(windows ? "mvnw.cmd" : "mvnw");
        String script = windows
                ? "@echo off\r\nset \"OUTPUT=%~2\"\r\nset \"OUTPUT=%OUTPUT:~18%\"\r\n>\"%OUTPUT%\" echo " + classpath + "\r\nexit /b " + exitCode + "\r\n"
                : "#!/bin/sh\nfor arg in \"$@\"; do case \"$arg\" in -Dmdep.outputFile=*) printf '%s\\n' '" + classpath + "' > \"${arg#-Dmdep.outputFile=}\" ;; esac; done\nexit " + exitCode + "\n";
        Files.writeString(wrapper, script);
        if (!windows) wrapper.toFile().setExecutable(true);
    }

    private Path createJavaProject() throws IOException {
        Path root = Files.createTempDirectory("eyecode-run-isolated");
        Files.createDirectories(root.resolve("src/main/java"));
        return root;
    }

    private ExecutionResult execute(Path root, String configurationId) throws Exception {
        ProjectLifecycleService lifecycle = new ProjectLifecycleService();
        lifecycle.open(root);
        RunService service = new RunService(lifecycle);
        if (configurationId != null) {
            assertTrue(service.selectConfiguration(configurationId));
        }
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<String> output = new AtomicReference<>("");
        AtomicInteger exitCode = new AtomicInteger(Integer.MIN_VALUE);
        AtomicReference<Boolean> stopped = new AtomicReference<>(false);
        service.addListener(new RunService.Listener() {
            @Override public void onStarted(RunRequest request) { }
            @Override public void onOutput(String line, boolean error) {
                if (line != null) output.updateAndGet(value -> value + line + "\n");
            }
            @Override public void onFinished(int code, boolean wasStopped) {
                exitCode.set(code);
                stopped.set(wasStopped);
                finished.countDown();
            }
        });
        assertTrue(service.runCurrent());
        assertTrue(finished.await(20, TimeUnit.SECONDS));
        service.dispose();
        return new ExecutionResult(output.get(), exitCode.get(), stopped.get());
    }

    private void writeJar(Path classes, Path jar) throws IOException {
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar));
             var files = Files.walk(classes)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String entryName = classes.relativize(file).toString().replace('\\', '/');
                output.putNextEntry(new JarEntry(entryName));
                Files.copy(file, output);
                output.closeEntry();
            }
        }
    }

    private record ExecutionResult(String output, int exitCode, boolean stopped) { }
}
