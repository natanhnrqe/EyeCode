package com.eyecode.ui.web;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebShellRunStabilityIntegrationTest {

    @Test
    void infinitePrintingLoopStopsThroughTheBridgeAndARerunWorks() throws Exception {
        CapturingSurface surface = new CapturingSurface();
        WebShellWorkspaceRuntime controller = WebShellWorkspaceComposition.create(
                surface, target -> { }, WebShellNativeUi.unavailable());
        try {
            WebShellEnvelope started = surface.handler("run", "run").handle(
                    WebShellEnvelope.request("run", "run", "run-1", lessonPayload("""
                            public class Main {
                                public static void main(String[] args) {
                                    while (true) {
                                        System.out.println("xxxxxxxxxxxxxxxxxxxx");
                                    }
                                }
                            }
                            """)));
            assertEquals(Boolean.TRUE, started.payload().get("started"));
            assertTrue(surface.firstOutput.await(20, TimeUnit.SECONDS), "the loop must produce output");

            surface.handler("run", "stop").handle(
                    WebShellEnvelope.request("run", "stop", "stop-1", Map.of()));
            assertTrue(surface.finishedStopped.await(20, TimeUnit.SECONDS),
                    "stop must reach a terminal state");

            surface.outputEvents.clear();
            WebShellEnvelope rerun = surface.handler("run", "rerun").handle(
                    WebShellEnvelope.request("run", "rerun", "run-2", lessonPayload("""
                            public class Main {
                                public static void main(String[] args) {
                                    System.out.println("REINICIOU");
                                }
                            }
                            """)));
            assertEquals(Boolean.TRUE, rerun.payload().get("started"));
            assertTrue(surface.finishedOk.await(20, TimeUnit.SECONDS), "a rerun after stop must complete");

            String joined = String.join("", surface.outputEvents);
            assertTrue(joined.contains("REINICIOU"));
            assertTrue(surface.outputEvents.size() <= 4,
                    "a short program must still be delivered as few coalesced events: "
                            + surface.outputEvents.size());
        } finally {
            controller.close();
        }
    }

    @Test
    void floodingOutputArrivesAsCoalescedEventsNotOneEventPerChunk() throws Exception {
        CapturingSurface surface = new CapturingSurface();
        WebShellWorkspaceRuntime controller = WebShellWorkspaceComposition.create(
                surface, target -> { }, WebShellNativeUi.unavailable());
        try {
            surface.handler("run", "run").handle(
                    WebShellEnvelope.request("run", "run", "run-3", lessonPayload("""
                            public class Main {
                                public static void main(String[] args) throws Exception {
                                    for (int i = 0; i < 300; i++) {
                                        System.out.println("linha-" + i);
                                        Thread.sleep(2);
                                    }
                                }
                            }
                            """)));
            assertTrue(surface.finishedOk.await(30, TimeUnit.SECONDS));
            String joined = String.join("", surface.outputEvents).replace("\r", "");
            assertTrue(joined.contains("linha-0\n"));
            assertTrue(joined.contains("linha-299\n"));
            assertTrue(joined.endsWith("linha-299\n"));
            assertTrue(surface.outputEvents.size() <= 25,
                    "300 slow lines must be coalesced by the flush timer, not published per line: "
                            + surface.outputEvents.size());
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

    private static final class CapturingSurface implements WebShellSurface {
        private final Map<String, WebShellMessageHandler> handlers = new java.util.concurrent.ConcurrentHashMap<>();
        private final CopyOnWriteArrayList<String> outputEvents = new CopyOnWriteArrayList<>();
        private final CountDownLatch firstOutput = new CountDownLatch(1);
        private final CountDownLatch finishedStopped = new CountDownLatch(1);
        private final CountDownLatch finishedOk = new CountDownLatch(1);

        @Override
        public void send(WebShellEnvelope message) {
            if (message == null || message.kind() != WebShellEnvelope.Kind.EVENT) return;
            if ("run".equals(message.channel()) && "output".equals(message.name())) {
                Object text = message.payload().get("text");
                if (Boolean.TRUE.equals(message.payload().get("clear"))) return;
                if (text instanceof String value) {
                    outputEvents.add(value);
                    firstOutput.countDown();
                }
            }
            if ("run".equals(message.channel()) && "state".equals(message.name())) {
                if (!"IDLE".equals(message.payload().get("phase"))) return;
                if (!Boolean.TRUE.equals(message.payload().get("finished"))) return;
                if (Boolean.TRUE.equals(message.payload().get("stopped"))) finishedStopped.countDown();
                else if (Integer.valueOf(0).equals(message.payload().get("exitCode"))) finishedOk.countDown();
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
