package com.eyecode.ui.web;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebShellLessonRunDiagnosticsIntegrationTest {

    private static final String TYPE_MISMATCH = """
            public class Main {
                public static void main(String[] args) {
                    int idade = "vinte";
                    System.out.println(idade);
                }
            }
            """;

    private static final String DIVISION_BY_ZERO = """
            public class Main {
                public static void main(String[] args) {
                    int total = 10;
                    int resultado = total / 0;
                    System.out.println(resultado);
                }
            }
            """;

    private static final String VALID = """
            public class Main {
                public static void main(String[] args) {
                    int total = 10;
                    System.out.println(total);
                }
            }
            """;

    @Test
    void failedLessonCompilationPublishesRealDiagnosticsWithEducation() throws Exception {
        CapturingSurface surface = new CapturingSurface();
        WebShellWorkspaceRuntime controller = WebShellWorkspaceComposition.create(
                surface, target -> { }, WebShellNativeUi.unavailable());
        try {
            surface.handler("run", "run").handle(request("run-1", TYPE_MISMATCH));
            assertTrue(surface.lessonRunLatch.await(30, TimeUnit.SECONDS));
            List<Map<String, Object>> diagnostics = surface.lessonRunEvents.get(0);
            assertEquals(1, diagnostics.size());
            Map<String, Object> diagnostic = diagnostics.get(0);
            assertEquals("lesson://fundamentals/s1/main", diagnostic.get("uri"));
            assertEquals("ERROR", diagnostic.get("severity"));
            assertEquals(3, diagnostic.get("startLine"));
            assertTrue(String.valueOf(diagnostic.get("message")).contains("incompatible types"));
            Map<String, Object> education = (Map<String, Object>) diagnostic.get("education");
            assertNotNull(education);
            assertEquals("Tipos incompatíveis", education.get("title"));
        } finally {
            controller.close();
        }
    }

    @Test
    void missingSemicolonReachesTheProblemsFlowWithItsEducation() throws Exception {
        CapturingSurface surface = new CapturingSurface();
        WebShellWorkspaceRuntime controller = WebShellWorkspaceComposition.create(
                surface, target -> { }, WebShellNativeUi.unavailable());
        try {
            String source = """
                    public class Main {
                        public static void main(String[] args) {
                            int total = 10
                            System.out.println(total);
                        }
                    }
                    """;
            surface.handler("run", "run").handle(request("run-2", source));
            assertTrue(surface.lessonRunLatch.await(30, TimeUnit.SECONDS));
            Map<String, Object> diagnostic = surface.lessonRunEvents.get(0).get(0);
            assertEquals(3, diagnostic.get("startLine"));
            Map<String, Object> education = (Map<String, Object>) diagnostic.get("education");
            assertEquals("Falta o ponto e vírgula", education.get("title"));
        } finally {
            controller.close();
        }
    }

    @Test
    void runtimeExceptionFromTheTraceIsAssociatedWithTheRealLineAndExplained() throws Exception {
        CapturingSurface surface = new CapturingSurface();
        WebShellWorkspaceRuntime controller = WebShellWorkspaceComposition.create(
                surface, target -> { }, WebShellNativeUi.unavailable());
        try {
            WebShellEnvelope started = surface.handler("run", "trace").handle(request("trace-1", DIVISION_BY_ZERO));
            assertEquals(Boolean.TRUE, started.payload().get("started"));
            assertTrue(surface.traceLatch.await(60, TimeUnit.SECONDS));
            assertEquals("RUNTIME_ERROR", surface.traceEvent.get().payload().get("status"));
            assertEquals(4, surface.traceEvent.get().payload().get("runtimeErrorLine"));

            assertTrue(surface.lessonRunLatch.await(10, TimeUnit.SECONDS));
            Map<String, Object> diagnostic = surface.lessonRunEvents.get(0).get(0);
            assertEquals("lesson://fundamentals/s1/main", diagnostic.get("uri"));
            assertEquals(4, diagnostic.get("startLine"));
            assertTrue(String.valueOf(diagnostic.get("message")).contains("ArithmeticException"));
            Map<String, Object> education = (Map<String, Object>) diagnostic.get("education");
            assertEquals("Divisão por zero", education.get("title"));
        } finally {
            controller.close();
        }
    }

    @Test
    void successfulLessonRunDoesNotPublishRunDiagnostics() throws Exception {
        CapturingSurface surface = new CapturingSurface();
        WebShellWorkspaceRuntime controller = WebShellWorkspaceComposition.create(
                surface, target -> { }, WebShellNativeUi.unavailable());
        try {
            surface.handler("run", "run").handle(request("run-3", VALID));
            assertTrue(surface.finishedLatch.await(30, TimeUnit.SECONDS));
            assertEquals(0, surface.lessonRunEvents.size());
        } finally {
            controller.close();
        }
    }

    private static WebShellEnvelope request(String requestId, String source) {
        return WebShellEnvelope.request("run", "run", requestId, Map.of(
                "context", "lesson",
                "lessonWorkspace", Map.of(
                        "files", List.of(Map.of(
                                "name", "Main.java",
                                "source", source,
                                "uri", "lesson://fundamentals/s1/main")),
                        "mainClass", "Main")));
    }

    private static final class CapturingSurface implements WebShellSurface {
        private final Map<String, WebShellMessageHandler> handlers = new java.util.concurrent.ConcurrentHashMap<>();
        private final CopyOnWriteArrayList<List<Map<String, Object>>> lessonRunEvents = new CopyOnWriteArrayList<>();
        private final CountDownLatch lessonRunLatch = new CountDownLatch(1);
        private final AtomicReference<WebShellEnvelope> traceEvent = new AtomicReference<>();
        private final CountDownLatch traceLatch = new CountDownLatch(1);
        private final CountDownLatch finishedLatch = new CountDownLatch(1);

        @Override
        public void send(WebShellEnvelope message) {
            if (message == null || message.kind() != WebShellEnvelope.Kind.EVENT) return;
            if ("run".equals(message.channel()) && "trace".equals(message.name())) {
                traceEvent.set(message);
                traceLatch.countDown();
            }
            if ("diagnostics".equals(message.channel()) && "lessonRun".equals(message.name())) {
                lessonRunEvents.add((List<Map<String, Object>>) message.payload().get("diagnostics"));
                lessonRunLatch.countDown();
            }
            if ("run".equals(message.channel()) && "state".equals(message.name())) {
                if ("IDLE".equals(message.payload().get("phase"))
                        && Boolean.TRUE.equals(message.payload().get("finished"))
                        && !Boolean.TRUE.equals(message.payload().get("running"))) {
                    finishedLatch.countDown();
                }
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
