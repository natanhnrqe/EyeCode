package com.eyecode.runtime;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LessonExecutionResolverTest {
    @Test
    void materializesCurrentSourcesExecutesMultipleFilesAndCleansUp() throws Exception {
        ResolvedExecution execution = new LessonExecutionResolver().resolve(new LessonRunRequest(List.of(
                new LessonRunRequest.SourceFile("com/example/Main.java", """
                        package com.example;
                        public class Main { public static void main(String[] args) { System.out.println(Helper.message()); } }
                        """),
                new LessonRunRequest.SourceFile("com/example/Helper.java", """
                        package com.example;
                        class Helper { static String message() { return "CURRENT_SNAPSHOT"; } }
                        """)), "com.example.Main"));
        Path root = Path.of(execution.commands().getFirst().get(4)).getParent();
        Result result = execute(execution);

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("CURRENT_SNAPSHOT"));
        assertFalse(Files.exists(root));
    }

    @Test
    void compilerFailureStreamsErrorAndNeverRunsJava() throws Exception {
        ResolvedExecution execution = new LessonExecutionResolver().resolve(new LessonRunRequest(List.of(
                new LessonRunRequest.SourceFile("Main.java", "public class Main { public static void main(String[] args) { System.out.println(\"broken\") } }")), "Main"));
        Result result = execute(execution);

        assertTrue(result.exitCode() != 0);
        assertTrue(result.error());
        assertFalse(result.phases().contains(RunPhase.RUNNING));
    }

    @Test
    void rejectsEscapingLessonSourcePath() {
        assertTrue(org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
                new LessonExecutionResolver().resolve(new LessonRunRequest(List.of(
                        new LessonRunRequest.SourceFile("../Main.java", "class Main {}")), "Main"))).getMessage().contains("Invalid lesson source path"));
    }

    private Result execute(ResolvedExecution execution) throws Exception {
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<String> output = new AtomicReference<>("");
        AtomicInteger exitCode = new AtomicInteger();
        var phases = new java.util.concurrent.CopyOnWriteArrayList<RunPhase>();
        RunSession session = new RunSession(execution, Path.of(System.getProperty("java.io.tmpdir")), new RunSession.Listener() {
            @Override public void onPhase(RunPhase phase) { phases.add(phase); }
            @Override public void onOutput(String text, boolean error) { output.updateAndGet(value -> value + text); }
            @Override public void onFinished(int code, boolean stopped) { exitCode.set(code); finished.countDown(); }
        });
        session.start();
        assertTrue(finished.await(20, TimeUnit.SECONDS));
        return new Result(output.get(), exitCode.get(), output.get().contains("error:"), phases);
    }

    private record Result(String output, int exitCode, boolean error, List<RunPhase> phases) { }
}
