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
