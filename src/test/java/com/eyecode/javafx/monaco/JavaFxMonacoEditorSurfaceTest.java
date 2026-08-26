package com.eyecode.javafx.monaco;

import javafx.application.Platform;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxMonacoEditorSurfaceTest {
    @BeforeAll
    static void startToolkit() {
        try { Platform.startup(() -> { }); } catch (IllegalStateException ignored) { }
    }

    @Test
    void keepsOneBridgeAndMultipleStableModels() throws Exception {
        FakeBridge bridge = new FakeBridge();
        runInFx(() -> {
            JavaFxMonacoEditorSurface surface = new JavaFxMonacoEditorSurface(bridge);
            surface.openModel("file:///workspace/Main.java", "java", "class Main {}", false);
            surface.openModel("file:///workspace/Person.java", "java", "class Person {}", false);
            surface.activateModel("file:///workspace/Person.java");
            surface.updateModelContent("file:///workspace/Main.java", "class Main { int value; }", 2, "host");

            assertEquals(2, surface.modelCount());
            assertEquals("file:///workspace/Person.java", surface.getActiveModel());
            assertEquals("class Main { int value; }", surface.modelContent("file:///workspace/Main.java"));
            assertEquals(5, bridge.commands.size());
            surface.dispose();
            surface.dispose();
            assertEquals(1, bridge.disposeCount);
        });
    }

    @Test
    void closeModelDisposesOnlyTheModelAndLeavesOtherModelsUsable() throws Exception {
        FakeBridge bridge = new FakeBridge();
        runInFx(() -> {
            JavaFxMonacoEditorSurface surface = new JavaFxMonacoEditorSurface(bridge);
            surface.openModel("eyecode://workspace/a.java", "java", "class A {}", false);
            surface.openModel("eyecode://workspace/b.java", "java", "class B {}", false);
            surface.closeModel("eyecode://workspace/a.java");
            assertFalse(surface.containsModel("eyecode://workspace/a.java"));
            assertTrue(surface.containsModel("eyecode://workspace/b.java"));
            surface.dispose();
        });
    }

    @Test
    void retainingActiveWorkspaceModelsClosesStaleProjectModels() throws Exception {
        FakeBridge bridge = new FakeBridge();
        runInFx(() -> {
            JavaFxMonacoEditorSurface surface = new JavaFxMonacoEditorSurface(bridge);
            surface.openModel("file:///project-a/Main.java", "java", "class Main {}", false);
            surface.openModel("file:///project-b/Application.java", "java", "class Application {}", false);

            surface.retainModels(java.util.Set.of("file:///project-b/Application.java"));

            assertFalse(surface.containsModel("file:///project-a/Main.java"));
            assertTrue(surface.containsModel("file:///project-b/Application.java"));
            surface.dispose();
        });
    }

    @Test
    void userSnapshotPreservesNewlinesWithoutHostRoundTrip() throws Exception {
        FakeBridge bridge = new FakeBridge();
        runInFx(() -> {
            JavaFxMonacoEditorSurface surface = new JavaFxMonacoEditorSurface(bridge);
            String content = "class Main {\n\n    void run() {\n        return;\n    }\n}";
            surface.openModel("file:///workspace/Main.java", "java", "class Main {}", false);
            int before = bridge.commands.size();
            surface.receiveEventForTest(MonacoEvent.contentChanged(
                    "file:///workspace/Main.java", content, 4));

            assertEquals(content, surface.modelContent("file:///workspace/Main.java"));
            assertEquals(before, bridge.commands.size());
            surface.dispose();
        });
    }

    @Test
    void browserJsonSnapshotDecodesLogicalNewlines() {
        MonacoEvent event = JavaFxMonacoEditorSurface.parseEventForTest(
                "{\"kind\":\"change\",\"id\":\"file:///Main.java\","
                        + "\"content\":\"class Main {\\n    void run() {}\\n}\",\"version\":4}");

        assertEquals("class Main {\n    void run() {}\n}", event.content());
    }

    @Test
    void browserEventsPreserveCoordinatesVersionAndCommand() {
        MonacoEvent caret = JavaFxMonacoEditorSurface.parseEventForTest(
                "{\"kind\":\"caret\",\"id\":\"file:///Main.java\",\"version\":11,\"line\":5,\"column\":3}");
        assertEquals(MonacoEvent.Type.CARET_CHANGED, caret.type());
        assertEquals(5, caret.line());
        assertEquals(3, caret.column());
        assertEquals(11, caret.version());

        MonacoEvent hover = JavaFxMonacoEditorSurface.parseEventForTest(
                "{\"kind\":\"hover\",\"id\":\"file:///Main.java\","
                        + "\"version\":7,\"line\":3,\"column\":5,\"x\":12.5,\"y\":48.25,"
                        + "\"start\":20,\"end\":26,\"word\":\"String\"}");
        assertEquals(MonacoEvent.Type.HOVER, hover.type());
        assertEquals(7, hover.version());
        assertEquals(3, hover.line());
        assertEquals(5, hover.column());
        assertEquals(12.5, hover.x());
        assertEquals(48.25, hover.y());
        assertEquals(20, hover.targetStartOffset());
        assertEquals(26, hover.targetEndOffset());
        assertEquals("String", hover.targetText());

        MonacoEvent command = JavaFxMonacoEditorSurface.parseEventForTest(
                "{\"kind\":\"command\",\"id\":\"file:///C:/work/My%20File.java\","
                        + "\"version\":7000000000,\"line\":8,\"column\":15,\"command\":\"documentation\"}");
        assertEquals(MonacoEvent.Type.COMMAND, command.type());
        assertEquals("file:///C:/work/My%20File.java", command.modelId());
        assertEquals(7000000000L, command.version());
        assertEquals(8, command.line());
        assertEquals(15, command.column());
        assertEquals(MonacoEvent.Command.DOCUMENTATION, command.command());
    }

    @Test
    void browserJsonDecodesEscapedWindowsUnicodeContent() {
        MonacoEvent event = JavaFxMonacoEditorSurface.parseEventForTest(
                "{\"kind\":\"change\",\"id\":\"file:///C:/work/My\\\\File.java\","
                        + "\"content\":\"class \uD83D\uDE00 {\\n  String value; }\",\"version\":2}");

        assertEquals("file:///C:/work/My\\File.java", event.modelId());
        assertEquals("class 😀 {\n  String value; }", event.content());
    }

    @Test
    void browserPaneFillsTheEntireSurfaceDuringLayout() throws Exception {
        FakeBridge bridge = new FakeBridge();
        runInFx(() -> {
            Label browserPane = new Label();
            JavaFxMonacoEditorSurface surface = new JavaFxMonacoEditorSurface(bridge, browserPane);
            surface.resize(640, 480);
            surface.layout();

            assertEquals(0, browserPane.getLayoutX());
            assertEquals(0, browserPane.getLayoutY());
            assertEquals(640, browserPane.getWidth());
            assertEquals(480, browserPane.getHeight());
            surface.dispose();
        });
    }

    private static void runInFx(ThrowingRunnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try { action.run(); } catch (Throwable throwable) { failure.set(throwable); }
            finally { done.countDown(); }
        });
        assertTrue(done.await(10, TimeUnit.SECONDS));
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    @FunctionalInterface
    private interface ThrowingRunnable { void run() throws Exception; }

    private static final class FakeBridge implements MonacoBridge {
        private final List<MonacoCommand> commands = new ArrayList<>();
        private int disposeCount;

        @Override public void send(MonacoCommand command) { commands.add(command); }
        @Override public void dispose() { disposeCount++; }
    }
}
