package com.eyecode.ui.web;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebShellLessonTraceIntegrationTest {

    @Test
    void traceRequestPublishesRealCapturedStepsThroughTheRunChannel() throws Exception {
        CapturingSurface surface = new CapturingSurface();
        WebShellWorkspaceRuntime controller = WebShellWorkspaceComposition.create(
                surface, target -> { }, WebShellNativeUi.unavailable());
        try {
            WebShellEnvelope started = surface.handler("run", "trace").handle(
                    WebShellEnvelope.request("run", "trace", "trace-1", Map.of(
                            "context", "lesson",
                            "lessonWorkspace", Map.of(
                                    "files", List.of(Map.of(
                                            "name", "Main.java",
                                            "source", """
                                                    public class Main {
                                                        public static void main(String[] args) {
                                                            int total = 10;
                                                            int inc = 5;
                                                            total = total + inc;
                                                            System.out.println(total);
                                                        }
                                                    }
                                                    """)),
                                    "mainClass", "Main"))));
            assertNotNull(started);
            assertTrue(started.error() == null);
            assertEquals(Boolean.TRUE, started.payload().get("started"));

            assertTrue(surface.traceLatch.await(60, TimeUnit.SECONDS));
            WebShellEnvelope trace = surface.traceEvents.get(0);
            assertEquals("COMPLETED", trace.payload().get("status"));
            assertEquals(0, trace.payload().get("exitCode"));

            List<?> steps = (List<?>) trace.payload().get("steps");
            assertNotNull(steps);
            assertTrue(steps.size() >= 2, "o traço deve conter os passos reais da execução");
            long previousElapsed = -1;
            for (Object step : steps) {
                Map<?, ?> stepPayload = (Map<?, ?>) step;
                Object elapsed = stepPayload.get("elapsedMs");
                assertNotNull(elapsed, "cada passo publicado deve carregar o tempo medido");
                long elapsedMs = ((Number) elapsed).longValue();
                assertTrue(elapsedMs >= 0, "o tempo medido nunca é negativo");
                assertTrue(elapsedMs > previousElapsed, "os tempos publicados avançam em ordem");
                previousElapsed = elapsedMs;
            }
            assertTrue(trace.payload().get("limitations") instanceof List<?>);

            String allText = trace.payload().get("output").toString();
            assertTrue(allText.contains("15"), "a saída real do programa deve viajar no traço");
        } finally {
            controller.close();
        }
    }

    private static final class CapturingSurface implements WebShellSurface {
        private final Map<String, WebShellMessageHandler> handlers = new java.util.concurrent.ConcurrentHashMap<>();
        private final CopyOnWriteArrayList<WebShellEnvelope> traceEvents = new CopyOnWriteArrayList<>();
        private final CountDownLatch traceLatch = new CountDownLatch(1);

        @Override
        public void send(WebShellEnvelope message) {
            if (message != null && "run".equals(message.channel()) && "trace".equals(message.name())
                    && message.kind() == WebShellEnvelope.Kind.EVENT) {
                traceEvents.add(message);
                traceLatch.countDown();
            }
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
