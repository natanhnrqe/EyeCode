package com.eyecode.javafx.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebShellRunOutputIntegrationTest {

    @Test
    void publishesRealRunOutputChunksWithTheFrontendTextField() throws Exception {
        Path root = Files.createTempDirectory("eyecode-webshell-run");
        Path source = root.resolve("src/main/java/Main.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "public class Main { public static void main(String[] args) { System.out.print(\"Hello \"); System.out.print(\"World!\\n\"); } }");

        CapturingSurface surface = new CapturingSurface();
        WebShellWorkspaceController controller = new WebShellWorkspaceController(
                surface, target -> { }, WebShellNativeUi.unavailable());
        try {
            WebShellEnvelope opened = surface.handler("workspace", "openProject").handle(
                    WebShellEnvelope.request("workspace", "openProject", "open-1",
                            Map.of("path", root.toString())));
            assertNotNull(opened);
            assertTrue(opened.error() == null);

            WebShellEnvelope started = surface.handler("run", "run").handle(
                    WebShellEnvelope.request("run", "run", "run-1", Map.of()));
            assertNotNull(started);
            assertEquals(false, started.error() != null);

            assertTrue(surface.outputLatch.await(20, TimeUnit.SECONDS));
            StringBuilder output = new StringBuilder();
            for (WebShellEnvelope event : surface.events("run", "output")) {
                Object text = event.payload().get("text");
                if (text instanceof String value) {
                    output.append(value);
                }
            }
            assertEquals("Hello World!\n", output.toString());
            assertTrue(surface.events("run", "output").stream()
                    .anyMatch(event -> event.payload().get("text") instanceof String
                            && event.payload().containsKey("error")));
        } finally {
            controller.dispose();
        }
    }

    @Test
    void runRequestReturnsBeforeMavenPreparationCompletes() throws Exception {
        Path root = Files.createTempDirectory("eyecode-webshell-maven-run");
        Path source = root.resolve("src/main/java/Main.java");
        Path release = root.resolve("release");
        Files.createDirectories(source.getParent());
        Files.writeString(root.resolve("pom.xml"), "<project/>");
        Files.writeString(source, "public class Main { public static void main(String[] args) { } }");
        Files.writeString(root.resolve("mvnw.cmd"), "@echo off\r\n"
                + ":wait\r\n"
                + "if exist \"" + release + "\" goto complete\r\n"
                + "ping -n 2 127.0.0.1 > nul\r\n"
                + "goto wait\r\n"
                + ":complete\r\n"
                + "set \"OUTPUT=%~2\"\r\n"
                + "set \"OUTPUT=%OUTPUT:~18%\"\r\n"
                + ">\"%OUTPUT%\" echo\r\n"
                + "exit /b 0\r\n");

        CapturingSurface surface = new CapturingSurface();
        WebShellWorkspaceController controller = new WebShellWorkspaceController(
                surface, target -> { }, WebShellNativeUi.unavailable());
        try {
            WebShellEnvelope opened = surface.handler("workspace", "openProject").handle(
                    WebShellEnvelope.request("workspace", "openProject", "open-maven-1",
                            Map.of("path", root.toString())));
            assertNotNull(opened);
            assertTrue(opened.error() == null);

            CompletableFuture<WebShellEnvelope> request = CompletableFuture.supplyAsync(() ->
                    surface.handler("run", "run").handle(
                            WebShellEnvelope.request("run", "run", "run-maven-1", Map.of())));
            WebShellEnvelope started;
            boolean returnedBeforePreparation = true;
            try {
                started = request.get(2, TimeUnit.SECONDS);
            } catch (TimeoutException timeout) {
                returnedBeforePreparation = false;
                started = request.get(10, TimeUnit.SECONDS);
            }
            assertTrue(returnedBeforePreparation);
            assertEquals(Boolean.TRUE, started.payload().get("started"));
            assertTrue(surface.preparingState.await(2, TimeUnit.SECONDS));

            Files.createFile(release);
            assertTrue(surface.finishedState.await(10, TimeUnit.SECONDS));
        } finally {
            if (!Files.exists(release)) Files.createFile(release);
            controller.dispose();
        }
    }

    private static final class CapturingSurface implements WebShellSurface {
        private final Map<String, WebShellMessageHandler> handlers = new java.util.concurrent.ConcurrentHashMap<>();
        private final List<WebShellEnvelope> sent = new CopyOnWriteArrayList<>();
        private final java.util.concurrent.CountDownLatch outputLatch = new java.util.concurrent.CountDownLatch(1);
        private final java.util.concurrent.CountDownLatch preparingState = new java.util.concurrent.CountDownLatch(1);
        private final java.util.concurrent.CountDownLatch finishedState = new java.util.concurrent.CountDownLatch(1);

        @Override
        public void send(WebShellEnvelope message) {
            sent.add(message);
            if (message != null && "run".equals(message.channel()) && "output".equals(message.name())
                    && message.payload().get("text") instanceof String) {
                outputLatch.countDown();
            }
            if (message != null && "run".equals(message.channel()) && "state".equals(message.name())) {
                if ("PREPARING".equals(message.payload().get("phase"))) preparingState.countDown();
                if ("IDLE".equals(message.payload().get("phase"))
                        && Boolean.TRUE.equals(message.payload().get("finished"))) finishedState.countDown();
            }
        }

        @Override
        public void registerHandler(String channel, String name, WebShellMessageHandler handler) {
            handlers.put(channel + "/" + name, handler);
        }

        private WebShellMessageHandler handler(String channel, String name) {
            return handlers.get(channel + "/" + name);
        }

        private List<WebShellEnvelope> events(String channel, String name) {
            return sent.stream()
                    .filter(message -> message.kind() == WebShellEnvelope.Kind.EVENT)
                    .filter(message -> channel.equals(message.channel()) && name.equals(message.name()))
                    .toList();
        }
    }
}
