package com.eyecode.runtime;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LessonRunStabilityTest {

    private static final String SILENT_INFINITE_LOOP = """
            public class Main {
                public static void main(String[] args) {
                    while (true) { }
                }
            }
            """;

    private static final String PRINTING_INFINITE_LOOP = """
            public class Main {
                public static void main(String[] args) {
                    while (true) {
                        System.out.println("xxxxxxxxxx");
                    }
                }
            }
            """;

    private static LessonRunRequest request(String source) {
        return new LessonRunRequest(
                List.of(new LessonRunRequest.SourceFile("Main.java", source)), "Main");
    }

    @Test
    void silentInfiniteLoopIsStoppedByTheTimeoutWithATerminalState() throws Exception {
        RunService service = new RunService(null, 1_500);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Integer> exitCode = new AtomicReference<>();
        AtomicReference<Boolean> stopped = new AtomicReference<>();
        List<String> output = new CopyOnWriteArrayList<>();
        try {
            service.addListener(new RunService.Listener() {
                @Override public void onStarted(RunRequest request) { }
                @Override public void onOutput(String text, boolean error) {
                    if (text != null) output.add(text);
                }
                @Override public void onFinished(int code, boolean wasStopped) {
                    exitCode.set(code);
                    stopped.set(wasStopped);
                    finished.countDown();
                }
            });
            assertTrue(service.runLesson(request(SILENT_INFINITE_LOOP)));
            assertTrue(finished.await(8, TimeUnit.SECONDS),
                    "the watchdog must terminate a silent infinite loop");
            assertNotNull(exitCode.get());
            assertEquals(Boolean.TRUE, stopped.get());
            assertTrue(service.lastStopped());
            assertTrue(String.join("", output).contains("tempo limite"),
                    "the timeout must announce itself instead of dying silently");
        } finally {
            service.dispose();
        }
    }

    @Test
    void stopInterruptsAFloodingLoopAndARerunStillWorks() throws Exception {
        RunService service = new RunService(null, 60_000);
        CountDownLatch firstOutput = new CountDownLatch(1);
        CountDownLatch firstFinished = new CountDownLatch(1);
        CountDownLatch secondFinished = new CountDownLatch(2);
        AtomicInteger finishedRuns = new AtomicInteger();
        List<String> secondRunOutput = new CopyOnWriteArrayList<>();
        AtomicReference<Integer> secondExitCode = new AtomicReference<>();
        try {
            service.addListener(new RunService.Listener() {
                @Override public void onStarted(RunRequest request) { }
                @Override public void onOutput(String text, boolean error) {
                    if (text != null) {
                        firstOutput.countDown();
                        if (finishedRuns.get() == 1) {
                            secondRunOutput.add(text);
                        }
                    }
                }
                @Override public void onFinished(int code, boolean stopped) {
                    finishedRuns.incrementAndGet();
                    secondExitCode.set(code);
                    firstFinished.countDown();
                    secondFinished.countDown();
                }
            });

            assertTrue(service.runLesson(request(PRINTING_INFINITE_LOOP)));
            assertTrue(firstOutput.await(10, TimeUnit.SECONDS), "the flooding loop must produce output");
            service.stop();
            assertTrue(firstFinished.await(8, TimeUnit.SECONDS), "stop must terminate the process");

            assertTrue(service.runLesson(request("""
                    public class Main {
                        public static void main(String[] args) {
                            System.out.println("REINICIOU");
                        }
                    }
                    """)));
            assertTrue(secondFinished.await(15, TimeUnit.SECONDS), "a run after cancellation must complete");
            assertEquals(0, secondExitCode.get());
            assertTrue(String.join("", secondRunOutput).contains("REINICIOU"));
        } finally {
            service.dispose();
        }
    }

    @Test
    void outputHistoryStaysBoundedUnderHeavyOutput() throws Exception {
        RunService service = new RunService(null, 60_000);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Integer> exitCode = new AtomicReference<>();
        List<String> replayed = new CopyOnWriteArrayList<>();
        try {
            service.addListener(new RunService.Listener() {
                @Override public void onStarted(RunRequest request) { }
                @Override public void onOutput(String text, boolean error) { }
                @Override public void onFinished(int code, boolean stopped) {
                    exitCode.set(code);
                    finished.countDown();
                }
            });
            StringBuilder source = new StringBuilder();
            source.append("public class Main {\n");
            source.append("    public static void main(String[] args) {\n");
            source.append("        for (int i = 0; i < 40000; i++) {\n");
            source.append("            System.out.println(\"aaaaaaaaaaaaaaaaaaaaaa\");\n");
            source.append("        }\n");
            source.append("    }\n");
            source.append("}\n");
            assertTrue(service.runLesson(request(source.toString())));
            assertTrue(finished.await(30, TimeUnit.SECONDS));
            assertEquals(0, exitCode.get());

            service.addListener(new RunService.Listener() {
                @Override public void onStarted(RunRequest request) { }
                @Override public void onOutput(String text, boolean error) {
                    if (text != null) replayed.add(text);
                }
                @Override public void onFinished(int code, boolean stopped) { }
            });
            String history = String.join("", replayed);
            assertTrue(history.contains("saída truncada"), "the replayed history must announce its cap");
            assertTrue(history.length() <= 300_000, "history must be bounded: " + history.length());
        } finally {
            service.dispose();
        }
    }
}
