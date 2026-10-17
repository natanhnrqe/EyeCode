package com.eyecode.ui.web;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebShellRunStabilityIntegrationTest {

    private static final String FLOOD_LOOP = """
            public class Main {
                public static void main(String[] args) {
                    int contador = 0;
                    while (true) {
                        System.out.println("Contagem: " + contador);
                        contador++;
                    }
                }
            }
            """;

    @Test
    void floodOutputArrivesProgressivelyWhileRunningAndStopKeepsWorking() throws Exception {
        CapturingSurface surface = new CapturingSurface();
        WebShellWorkspaceRuntime controller = WebShellWorkspaceComposition.create(
                surface, target -> { }, WebShellNativeUi.unavailable());
        try {
            surface.handler("run", "trace").handle(
                    WebShellEnvelope.request("run", "trace", "trace-1", lessonPayload(FLOOD_LOOP)));
            WebShellEnvelope started = surface.handler("run", "run").handle(
                    WebShellEnvelope.request("run", "run", "run-1", lessonPayload(FLOOD_LOOP)));
            assertEquals(Boolean.TRUE, started.payload().get("started"));

            assertTrue(surface.progressiveOutput.await(15, TimeUnit.SECONDS),
                    "the console must receive several flushes before the program ends");
            assertTrue(surface.traceEvent.await(15, TimeUnit.SECONDS),
                    "the trace must publish its observed values during the same execution");
            assertTrue(surface.lastTraceValue.get() > 0,
                    "the trace must show the counter really advancing inside the loop");
            assertTrue(surface.outputEvents.size() <= 400,
                    "a 15s flood must stay coalesced, not one event per chunk: "
                            + surface.outputEvents.size());

            surface.handler("run", "stop").handle(
                    WebShellEnvelope.request("run", "stop", "stop-1", Map.of()));
            assertTrue(surface.finishedStopped.await(20, TimeUnit.SECONDS),
                    "stop must terminate both the program and the trace");
        } finally {
            controller.close();
        }
    }

    @Test
    void secondExecutionShowsItsOwnOutputIncrementally() throws Exception {
        CapturingSurface surface = new CapturingSurface();
        WebShellWorkspaceRuntime controller = WebShellWorkspaceComposition.create(
                surface, target -> { }, WebShellNativeUi.unavailable());
        try {
            surface.handler("run", "run").handle(
                    WebShellEnvelope.request("run", "run", "run-1", lessonPayload(FLOOD_LOOP)));
            assertTrue(surface.firstOutput.await(15, TimeUnit.SECONDS));
            surface.handler("run", "stop").handle(
                    WebShellEnvelope.request("run", "stop", "stop-1", Map.of()));
            assertTrue(surface.awaitFinishCount(1, 20, TimeUnit.SECONDS),
                    "stop must complete the first execution");

            surface.outputEvents.clear();
            surface.handler("run", "run").handle(
                    WebShellEnvelope.request("run", "run", "run-2", lessonPayload("""
                            public class Main {
                                public static void main(String[] args) {
                                    System.out.println("SEGUNDA_EXECUCAO");
                                }
                            }
                            """)));
            assertTrue(surface.awaitFinishCount(2, 20, TimeUnit.SECONDS), "the rerun must complete");
            assertEquals(0, surface.finishLog.get(1).exitCode, "the rerun must finish cleanly");
            String joined = String.join("", surface.outputEvents).replace("\r", "");
            assertTrue(joined.contains("SEGUNDA_EXECUCAO\n"),
                    "the second execution must reach the console (regression: persistent truncation)");
            assertTrue(surface.clearEvents.get() >= 2,
                    "each execution must start with a clean console boundary");
        } finally {
            controller.close();
        }
    }

    @Test
    void runWithoutPrintingStartsWithAnEmptyConsole() throws Exception {
        CapturingSurface surface = new CapturingSurface();
        WebShellWorkspaceRuntime controller = WebShellWorkspaceComposition.create(
                surface, target -> { }, WebShellNativeUi.unavailable());
        try {
            surface.handler("run", "run").handle(
                    WebShellEnvelope.request("run", "run", "run-1", lessonPayload("""
                            public class Main {
                                public static void main(String[] args) {
                                    System.out.println("PRIMEIRA_EXECUCAO");
                                }
                            }
                            """)));
            assertTrue(surface.awaitFinishCount(1, 20, TimeUnit.SECONDS));

            surface.outputEvents.clear();
            surface.handler("run", "run").handle(
                    WebShellEnvelope.request("run", "run", "run-2", lessonPayload("""
                            public class Main {
                                public static void main(String[] args) {
                                    int total = 10;
                                    total = total + 5;
                                }
                            }
                            """)));
            assertTrue(surface.awaitFinishCount(2, 20, TimeUnit.SECONDS));
            assertEquals(0, surface.outputEvents.size(),
                    "a silent execution must leave the console empty — no residual output");
        } finally {
            controller.close();
        }
    }

    private static Map<String, Object> lessonPayload(String source) {
        return Map.of(
                "context", "lesson",
                "lessonWorkspace", Map.of(
                        "files", List.of(Map.of("name", "Main.java", "source", source)),
                        "mainClass", "Main"));
    }

    private record Finish(int exitCode, boolean stopped) {
    }

    private static final class CapturingSurface implements WebShellSurface {
        private final Map<String, WebShellMessageHandler> handlers = new java.util.concurrent.ConcurrentHashMap<>();
        private final CopyOnWriteArrayList<String> outputEvents = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<Finish> finishLog = new CopyOnWriteArrayList<>();
        private final CountDownLatch firstOutput = new CountDownLatch(1);
        private final CountDownLatch progressiveOutput = new CountDownLatch(5);
        private final CountDownLatch traceEvent = new CountDownLatch(1);
        private final CountDownLatch finishedStopped = new CountDownLatch(1);
        private final AtomicInteger clearEvents = new AtomicInteger();
        private final AtomicInteger lastTraceValue = new AtomicInteger(-1);

        @Override
        public void send(WebShellEnvelope message) {
            if (message == null || message.kind() != WebShellEnvelope.Kind.EVENT) return;
            if ("run".equals(message.channel()) && "output".equals(message.name())) {
                if (Boolean.TRUE.equals(message.payload().get("clear"))) {
                    clearEvents.incrementAndGet();
                    return;
                }
                if (message.payload().get("text") instanceof String value) {
                    outputEvents.add(value);
                    firstOutput.countDown();
                    progressiveOutput.countDown();
                }
            }
            if ("run".equals(message.channel()) && "trace".equals(message.name())) {
                List<?> steps = (List<?>) message.payload().get("steps");
                if (steps != null) {
                    for (Object step : steps) {
                        if (step instanceof Map<?, ?> stepPayload
                                && stepPayload.get("variables") instanceof List<?> variables) {
                            for (Object variable : variables) {
                                if (variable instanceof Map<?, ?> variablePayload
                                        && "contador".equals(variablePayload.get("name"))) {
                                    try {
                                        int value = Integer.parseInt(String.valueOf(variablePayload.get("value")));
                                        lastTraceValue.accumulateAndGet(value, Math::max);
                                    } catch (NumberFormatException ignored) {
                                    }
                                }
                            }
                        }
                    }
                    traceEvent.countDown();                }
            }
            if ("run".equals(message.channel()) && "state".equals(message.name())) {
                if (!"IDLE".equals(message.payload().get("phase"))) return;
                if (!Boolean.TRUE.equals(message.payload().get("finished"))) return;
                boolean stopped = Boolean.TRUE.equals(message.payload().get("stopped"));
                int exitCode = message.payload().get("exitCode") instanceof Number number ? number.intValue() : -1;
                finishLog.add(new Finish(exitCode, stopped));
                if (stopped) finishedStopped.countDown();
            }
        }

        boolean awaitFinishCount(int count, long timeout, TimeUnit unit) throws InterruptedException {
            long deadline = System.nanoTime() + unit.toNanos(timeout);
            while (System.nanoTime() < deadline) {
                if (finishLog.size() >= count) return true;
                Thread.sleep(20);
            }
            return finishLog.size() >= count;
        }

        @Override
        public void registerHandler(String channel, String name, WebShellMessageHandler handler) {
            handlers.put(channel + "/" + name, handler);
        }

        private WebShellMessageHandler handler(String channel, String name) {
            return handlers.get(channel + "/" + name);
        }
    }
}
