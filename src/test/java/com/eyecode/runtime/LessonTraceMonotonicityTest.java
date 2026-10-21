package com.eyecode.runtime;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reproduces the reported scenario: a counter that appears to decrease during
 * tracing. Runs two consecutive traces of the same infinite counter loop and
 * verifies (a) within each trace values are strictly increasing, and (b) the
 * second trace is independent — no sample from the first execution leaks
 * into the second.
 */
class LessonTraceMonotonicityTest {

    private static final String COUNTER_LOOP = """
            public class Main {
                public static void main(String[] args) {
                    int contador = 0;
                    while (true) {
                        contador++;
                    }
                }
            }
            """;

    @Test
    void consecutiveTracesAreIndependentAndInternallyMonotonic() throws Exception {
        LessonTraceService service = new LessonTraceService(4_000, 500);
        try {
            // === TRACE 1 ===
            CompletableFuture<LessonTrace> trace1Future = new CompletableFuture<>();
            assertTrue(service.trace(request(COUNTER_LOOP), trace1Future::complete));
            LessonTrace trace1 = trace1Future.get(60, TimeUnit.SECONDS);
            assertEquals(LessonTraceStatus.TIMEOUT, trace1.status());

            List<Long> trace1Values = extractContador(trace1);
            assertTrue(trace1Values.size() >= 3, "trace 1 deve ter >=3 amostras: " + trace1Values);
            assertStrictlyIncreasing(trace1Values, "trace 1");

            // === TRACE 2 (immediately after trace 1 completed) ===
            CompletableFuture<LessonTrace> trace2Future = new CompletableFuture<>();
            assertTrue(service.trace(request(COUNTER_LOOP), trace2Future::complete));
            LessonTrace trace2 = trace2Future.get(60, TimeUnit.SECONDS);
            assertEquals(LessonTraceStatus.TIMEOUT, trace2.status());

            List<Long> trace2Values = extractContador(trace2);
            assertTrue(trace2Values.size() >= 3, "trace 2 deve ter >=3 amostras: " + trace2Values);
            assertStrictlyIncreasing(trace2Values, "trace 2");

            // trace 2 starts fresh — its first value must be lower than trace 1's last
            long trace1Last = trace1Values.get(trace1Values.size() - 1);
            long trace2First = trace2Values.get(0);
            assertTrue(trace2First < trace1Last,
                    "trace 2 deve recomeçar (primeiro=" + trace2First + " < último da trace 1=" + trace1Last + ")");

            // no value from trace 2 can exceed trace 1's last value by accident
            // (different JVM, same speed roughly — but they are independent)
            System.out.println("[mono] trace1: " + trace1Values.size() + " amostras, última=" + trace1Last);
            System.out.println("[mono] trace2: " + trace2Values.size() + " amostras, primeira=" + trace2First);
            trace1Values.forEach(v -> System.out.println("[mono] t1: " + v));
            trace2Values.forEach(v -> System.out.println("[mono] t2: " + v));
        } finally {
            service.close();
        }
    }

    @Test
    void rejectedTraceWhileActiveDoesNotContaminateTheActiveTrace() throws Exception {
        LessonTraceService service = new LessonTraceService(5_000, 500);
        try {
            // Start trace 1 (long-running counter)
            CompletableFuture<LessonTrace> trace1Future = new CompletableFuture<>();
            assertTrue(service.trace(request(COUNTER_LOOP), trace1Future::complete));

            // Try to start trace 2 while trace 1 is active — must be rejected
            CompletableFuture<LessonTrace> rejected = new CompletableFuture<>();
            assertEquals(false, service.trace(request(COUNTER_LOOP), rejected::complete),
                    "trace concorrente deve ser rejeitada");

            // Wait for trace 1 to complete
            LessonTrace trace1 = trace1Future.get(60, TimeUnit.SECONDS);
            List<Long> values = extractContador(trace1);
            assertStrictlyIncreasing(values, "trace ativa não pode ser contaminada");
            assertTrue(values.size() >= 3, "trace ativa deve ter amostras suficientes: " + values);
        } finally {
            service.close();
        }
    }

    private static LessonRunRequest request(String source) {
        return new LessonRunRequest(
                List.of(new LessonRunRequest.SourceFile("Main.java", source)), "Main");
    }

    private static List<Long> extractContador(LessonTrace trace) {
        return trace.steps().stream()
                .flatMap(step -> step.variables().stream())
                .filter(variable -> variable.name().equals("contador"))
                .map(LessonTrace.Variable::value)
                .map(Long::parseLong)
                .toList();
    }

    private static void assertStrictlyIncreasing(List<Long> values, String label) {
        for (int index = 1; index < values.size(); index++) {
            assertTrue(values.get(index) > values.get(index - 1),
                    label + ": valor DIMINUIU na amostra " + index + ": "
                            + values.get(index) + " após " + values.get(index - 1)
                            + " — sequência completa: " + values);
        }
    }
}
