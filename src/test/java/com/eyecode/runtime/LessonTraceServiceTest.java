package com.eyecode.runtime;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LessonTraceServiceTest {

    private static final String ASSIGNMENT_LESSON = """
            public class Main {
                public static void main(String[] args) {
                    int total = 10;
                    int inc = 5;
                    total = total + inc;
                    System.out.println(total);
                }
            }
            """;

    @Test
    void capturesRealValueEvolutionAcrossTheAssignment() throws Exception {
        LessonTrace trace = run(ASSIGNMENT_LESSON);

        assertEquals(LessonTraceStatus.COMPLETED, trace.status());
        assertEquals(0, trace.exitCode());
        assertNull(trace.runtimeError());

        LessonTrace.Step before = stepAtLine(trace, 5);
        assertNotNull(before, "deve existir um passo na linha da atribuição: " + trace.steps());
        assertEquals("10", variable(before, "total").value());
        assertEquals("5", variable(before, "inc").value());

        LessonTrace.Step after = stepAtLine(trace, 6);
        assertNotNull(after, "deve existir um passo na linha seguinte: " + trace.steps());
        assertEquals("15", variable(after, "total").value());
        assertEquals("5", variable(after, "inc").value());

        assertTrue(trace.output().stream().anyMatch(chunk -> chunk.text().contains("15")));
        assertTrue(trace.steps().stream().allMatch(step -> step.index() >= 0));
        assertTrue(trace.steps().stream().allMatch(step -> "Main".equals(step.className())));
    }

    @Test
    void capturesStringsBooleansAndDoubles() throws Exception {
        String source = """
                public class Main {
                    public static void main(String[] args) {
                        String nome = "Ana";
                        boolean ativo = true;
                        double preco = 12.5;
                        char inicial = 'A';
                        System.out.println(nome);
                    }
                }
                """;
        LessonTrace trace = run(source);

        assertEquals(LessonTraceStatus.COMPLETED, trace.status());
        LessonTrace.Step step = stepAtLine(trace, 6);
        assertNotNull(step);
        assertEquals("\"Ana\"", variable(step, "nome").value());
        assertEquals("true", variable(step, "ativo").value());
        assertEquals("12.5", variable(step, "preco").value());

        LessonTrace.Step next = stepAtLine(trace, 7);
        assertNotNull(next);
        assertEquals("'A'", variable(next, "inicial").value());
        assertEquals("\"Ana\"", variable(next, "nome").value());
    }

    @Test
    void capturesRealArrayElements() throws Exception {
        String source = """
                public class Main {
                    public static void main(String[] args) {
                        int[] numeros = {10, 20, 30};
                        numeros[1] = 25;
                        System.out.println(numeros[1]);
                    }
                }
                """;
        LessonTrace trace = run(source);

        assertEquals(LessonTraceStatus.COMPLETED, trace.status());
        LessonTrace.Step before = stepAtLine(trace, 4);
        LessonTrace.Variable array = variable(before, "numeros");
        assertEquals("array", array.kind());
        assertEquals(3, array.children().size());
        assertEquals("10", array.children().get(0).value());
        assertEquals("20", array.children().get(1).value());
        assertEquals("30", array.children().get(2).value());

        LessonTrace.Step after = stepAtLine(trace, 5);
        assertEquals("25", variable(after, "numeros").children().get(1).value());
    }

    @Test
    void capturesSimpleObjectFields() throws Exception {
        String source = """
                class Pessoa {
                    String nome;
                    int idade;
                }
                public class Main {
                    public static void main(String[] args) {
                        Pessoa p = new Pessoa();
                        p.nome = "Ana";
                        p.idade = 30;
                        System.out.println(p.nome);
                    }
                }
                """;
        LessonTrace trace = run(source);

        assertEquals(LessonTraceStatus.COMPLETED, trace.status());
        LessonTrace.Step step = stepAtLine(trace, 10);
        assertNotNull(step, "deve existir um passo na linha do println");
        LessonTrace.Variable pessoa = variable(step, "p");
        assertEquals("object", pessoa.kind());
        assertEquals("\"Ana\"", childField(pessoa, "nome").value());
        assertEquals("30", childField(pessoa, "idade").value());
    }

    @Test
    void compileErrorIsReportedWithoutSteps() throws Exception {
        String source = """
                public class Main {
                    public static void main(String[] args) {
                        int total = ;
                    }
                }
                """;
        LessonTrace trace = run(source);

        assertEquals(LessonTraceStatus.COMPILE_ERROR, trace.status());
        assertTrue(trace.steps().isEmpty());
        assertFalse(trace.output().isEmpty());
    }

    @Test
    void uncaughtExceptionKeepsCapturedStepsAndReportsRuntimeError() throws Exception {
        String source = """
                public class Main {
                    public static void main(String[] args) {
                        int total = 10;
                        int resultado = total / 0;
                        System.out.println(resultado);
                    }
                }
                """;
        LessonTrace trace = run(source);

        assertEquals(LessonTraceStatus.RUNTIME_ERROR, trace.status());
        assertNotNull(trace.runtimeError());
        assertTrue(trace.runtimeError().contains("ArithmeticException"));
        LessonTrace.Step before = stepAtLine(trace, 4);
        assertNotNull(before);
        assertEquals("10", variable(before, "total").value());
    }

    @Test
    void infiniteLoopIsTruncatedByTheStepLimit() throws Exception {
        String source = """
                public class Main {
                    public static void main(String[] args) {
                        int contador = 0;
                        while (contador >= 0) {
                            contador = contador + 1;
                        }
                    }
                }
                """;
        LessonTraceService service = new LessonTraceService(20_000, 120);
        LessonTrace trace;
        try {
            trace = traceAndAwait(service, source);
        } finally {
            service.close();
        }

        assertEquals(LessonTraceStatus.TRUNCATED, trace.status());
        assertEquals(120, trace.steps().size());
        assertTrue(trace.limitations().stream().anyMatch(message -> message.contains("limite de 120 passos")));
    }

    @Test
    void slowProgramIsStoppedByTheTimeout() throws Exception {
        String source = """
                public class Main {
                    public static void main(String[] args) throws Exception {
                        Thread.sleep(30_000);
                    }
                }
                """;
        LessonTraceService service = new LessonTraceService(1_500, 500);
        LessonTrace trace;
        try {
            trace = traceAndAwait(service, source);
        } finally {
            service.close();
        }

        assertEquals(LessonTraceStatus.TIMEOUT, trace.status());
    }

    @Test
    void stopInterruptsTheActiveTrace() throws Exception {
        String source = """
                public class Main {
                    public static void main(String[] args) throws Exception {
                        Thread.sleep(30_000);
                    }
                }
                """;
        LessonTraceService service = new LessonTraceService();
        try {
            LessonTrace trace = traceAndAwait(service, source, () -> {
                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException ignored) {
                }
                service.stop();
            });
            assertEquals(LessonTraceStatus.STOPPED, trace.status());
        } finally {
            service.close();
        }
    }

    @Test
    void concurrentTraceRequestIsRejectedWhileOneIsRunning() throws Exception {
        String source = """
                public class Main {
                    public static void main(String[] args) throws Exception {
                        Thread.sleep(30_000);
                    }
                }
                """;
        LessonTraceService service = new LessonTraceService();
        try {
            CompletableFuture<LessonTrace> first = new CompletableFuture<>();
            assertTrue(service.trace(lessonRequest(source), first::complete));
            CompletableFuture<LessonTrace> second = new CompletableFuture<>();
            assertFalse(service.trace(lessonRequest(source), second::complete));
            service.stop();
            first.get(30, TimeUnit.SECONDS);
        } finally {
            service.close();
        }
    }

    @Test
    void singleLineInfiniteLoopStillObservesRealCounterValuesBeforeTheTimeout() throws Exception {
        String source = """
                public class Main {
                    public static void main(String[] args) {
                        int contador = 0;
                        while (true) {
                            contador++;
                        }
                    }
                }
                """;
        long timeoutMs = 4_000;
        LessonTraceService service = new LessonTraceService(timeoutMs, 500);
        long startedAt = System.nanoTime();
        LessonTrace trace;
        try {
            trace = traceAndAwait(service, source);
        } finally {
            service.close();
        }
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        assertEquals(LessonTraceStatus.TIMEOUT, trace.status());
        assertTrue(elapsedMs < timeoutMs * 2,
                "a amostragem não pode estourar o próprio timeout: " + elapsedMs + "ms");
        List<String> counterValues = trace.steps().stream()
                .flatMap(step -> step.variables().stream())
                .filter(variable -> variable.name().equals("contador"))
                .map(LessonTrace.Variable::value)
                .toList();
        assertTrue(counterValues.size() >= 3,
                "o loop deve ser observado por amostragem real, não apenas no primeiro passo: " + counterValues);
        assertTrue(counterValues.stream().distinct().count() >= 3,
                "os valores observados de contador devem evoluir durante o loop: " + counterValues);
        assertTrue(counterValues.stream().anyMatch(value -> Integer.parseInt(value) > 0),
                "valores reais do increment devem aparecer antes do timeout: " + counterValues);
        long samples = trace.steps().stream().filter(LessonTrace.Step::sampled).count();
        assertTrue(samples >= 3 && samples <= 10,
                "a amostragem deve ser limitada (3..10 amostras), não uma por iteração: " + samples);
        assertTrue(trace.steps().size() < 20,
                "a amostragem deve ser limitada, não um snapshot por iteração: " + trace.steps().size());
        assertTrue(trace.limitations().stream().anyMatch(message ->
                        message.contains("Loop mais rápido que a captura")
                                && message.contains("µs de suspensão")
                                && message.contains("não registrados")),
                "o trace deve declarar honestamente que incrementos individuais não foram observados: "
                        + trace.limitations());
    }

    @Test
    void conditionalLoopKeepsStepBasedEvolutionWithoutSampling() throws Exception {
        String source = """
                public class Main {
                    public static void main(String[] args) {
                        int contador = 0;
                        while (contador < 3) {
                            contador++;
                        }
                        System.out.println(contador);
                    }
                }
                """;
        LessonTrace trace = run(source);

        assertEquals(LessonTraceStatus.COMPLETED, trace.status());
        assertTrue(trace.steps().stream().noneMatch(LessonTrace.Step::sampled),
                "programas normais continuam com evolução por passos, sem amostras");
        List<String> counterValues = trace.steps().stream()
                .flatMap(step -> step.variables().stream())
                .filter(variable -> variable.name().equals("contador"))
                .map(LessonTrace.Variable::value)
                .toList();
        assertTrue(counterValues.contains("0"), "estado inicial observado: " + counterValues);
        assertTrue(counterValues.contains("3"), "estado final observado: " + counterValues);
    }

    @Test
    void traceAfterATimeoutStillRunsNormally() throws Exception {
        LessonTraceService service = new LessonTraceService(1_500, 500);
        try {
            LessonTrace timedOut = traceAndAwait(service, """
                    public class Main {
                        public static void main(String[] args) throws Exception {
                            Thread.sleep(30_000);
                        }
                    }
                    """);
            assertEquals(LessonTraceStatus.TIMEOUT, timedOut.status());
            LessonTrace healthy = traceAndAwait(service, ASSIGNMENT_LESSON);
            assertEquals(LessonTraceStatus.COMPLETED, healthy.status());
            assertTrue(healthy.steps().stream().noneMatch(LessonTrace.Step::sampled));
        } finally {
            service.close();
        }
    }

    @Test
    void sampledLoopProgressionIsFaithfulToTheRealExecutionRate() throws Exception {
        String source = """
                public class Main {
                    public static void main(String[] args) {
                        int contador = 0;
                        while (true) {
                            contador++;
                        }
                    }
                }
                """;
        long timeoutMs = 6_000;
        LessonTraceService service = new LessonTraceService(timeoutMs, 500);
        LessonTrace trace;
        try {
            trace = traceAndAwait(service, source);
        } finally {
            service.close();
        }

        assertEquals(LessonTraceStatus.TIMEOUT, trace.status());
        List<LessonTrace.Step> samples = trace.steps().stream()
                .filter(LessonTrace.Step::sampled)
                .toList();
        assertTrue(samples.size() >= 3 && samples.size() <= 10,
                "amostragem limitada (3..10), nunca uma por iteração: " + samples.size());

        // duplicação ou atualização aplicada duas vezes produziriam valores
        // repetidos ou decrescentes; um contador real só cresce.
        long previousValue = Long.MIN_VALUE;
        long previousElapsed = Long.MIN_VALUE;
        List<Long> intervals = new java.util.ArrayList<>();
        for (LessonTrace.Step sample : samples) {
            long value = sample.variables().stream()
                    .filter(variable -> variable.name().equals("contador"))
                    .map(LessonTrace.Variable::value)
                    .mapToLong(Long::parseLong).findFirst().orElseThrow();
            assertTrue(value > previousValue,
                    "valores observados devem ser estritamente crescentes: " + value + " após " + previousValue);
            assertTrue(sample.elapsedMs() > previousElapsed,
                    "o tempo medido de cada observação deve avançar");
            if (previousElapsed > 0) {
                intervals.add(sample.elapsedMs() - previousElapsed);
            }
            previousValue = value;
            previousElapsed = sample.elapsedMs();
        }

        // agendamento uniforme: depois da primeira observação (que inclui o
        // stall), os intervalos não dobram. O backoff geométrico antigo fazia
        // ratio >= 4 e um loop linear parecer geométrico na interface.
        List<Long> steadyIntervals = intervals.size() > 1
                ? intervals.subList(1, intervals.size()) : intervals;
        long maxInterval = steadyIntervals.stream().mapToLong(Long::longValue).max().orElseThrow();
        long minInterval = steadyIntervals.stream().mapToLong(Long::longValue).min().orElseThrow();
        assertTrue(minInterval >= LessonTraceService.SAMPLE_INTERVAL_MS,
                "o piso de intervalo protege a responsividade: " + steadyIntervals);
        assertTrue(maxInterval <= minInterval * 2,
                "intervalos estáveis distinguem salto legítimo de backoff: " + steadyIntervals);

        // cálculo incorreto (ex.: valor dobrado) dobraria a taxa aparente;
        // a taxa real estabiliza com o JIT — banda larga, mas finita.
        List<Double> rates = samples.stream()
                .filter(sample -> sample.elapsedMs() > 1_500)
                .map(sample -> sample.variables().stream()
                        .filter(variable -> variable.name().equals("contador"))
                        .map(LessonTrace.Variable::value).mapToLong(Long::parseLong).findFirst().orElseThrow()
                        * 1000.0 / sample.elapsedMs())
                .toList();
        assertTrue(rates.size() >= 3, "amostras pós-warmup suficientes: " + rates.size());
        double minRate = rates.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
        double maxRate = rates.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
        assertTrue(maxRate < minRate * 1.5,
                "a taxa observada deve ser estável (sem saltos de duplicação): " + rates);

        assertTrue(trace.limitations().stream().anyMatch(message ->
                        message.contains("Loop mais rápido que a captura")),
                "a limitação honesta deve acompanhar o trace");
    }

    private static LessonRunRequest lessonRequest(String source) {
        return new LessonRunRequest(
                List.of(new LessonRunRequest.SourceFile("Main.java", source)), "Main");
    }

    private static LessonTrace traceAndAwait(LessonTraceService service, String source) throws Exception {
        return traceAndAwait(service, source, () -> { });
    }

    private static LessonTrace traceAndAwait(LessonTraceService service, String source, Runnable duringRun)
            throws Exception {
        CompletableFuture<LessonTrace> future = new CompletableFuture<>();
        assertTrue(service.trace(lessonRequest(source), future::complete), "o primeiro traço deve ser aceito");
        duringRun.run();
        return future.get(60, TimeUnit.SECONDS);
    }

    private static LessonTrace run(String source) throws Exception {
        LessonTraceService service = new LessonTraceService();
        try {
            return traceAndAwait(service, source);
        } finally {
            service.close();
        }
    }

    private static LessonTrace.Step stepAtLine(LessonTrace trace, int line) {
        return trace.steps().stream()
                .filter(step -> step.line() == line)
                .findFirst()
                .orElse(null);
    }

    private static LessonTrace.Variable variable(LessonTrace.Step step, String name) {
        LessonTrace.Variable found = step.variables().stream()
                .filter(variable -> variable.name().equals(name))
                .findFirst()
                .orElse(null);
        assertNotNull(found, "variável " + name + " deveria estar visível no passo: " + step.variables());
        return found;
    }

    private static LessonTrace.Variable childField(LessonTrace.Variable object, String field) {
        return object.children().stream()
                .filter(child -> child.name().equals(field))
                .findFirst()
                .orElseThrow(() -> new AssertionError("campo " + field + " ausente: " + object));
    }
}
