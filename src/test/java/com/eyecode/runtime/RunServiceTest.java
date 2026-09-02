package com.eyecode.runtime;

import com.eyecode.project.ProjectLifecycleService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunServiceTest {

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
        AtomicInteger runs = new AtomicInteger();
        service.addListener(new RunService.Listener() {
            @Override public void onStarted(RunRequest request) { runs.incrementAndGet(); }
            @Override public void onOutput(String line, boolean error) { output.updateAndGet(value -> value + line + "\n"); }
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
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Boolean> stopped = new AtomicReference<>(false);
        service.addListener(new RunService.Listener() {
            @Override public void onStarted(RunRequest request) { started.countDown(); }
            @Override public void onOutput(String line, boolean error) { }
            @Override public void onFinished(int exitCode, boolean wasStopped) {
                stopped.set(wasStopped);
                finished.countDown();
            }
        });

        assertTrue(service.runCurrent());
        assertTrue(started.await(20, TimeUnit.SECONDS));
        service.stop();
        assertTrue(finished.await(20, TimeUnit.SECONDS));
        assertTrue(stopped.get());
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
}