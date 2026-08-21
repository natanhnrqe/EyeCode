package com.eyecode.javafx.learning;

import javafx.application.Platform;
import org.fxmisc.richtext.CodeArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxLearningWorkspaceTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void editorBindingsShareOneCardRendererAndSurface() throws Exception {
        runInFx(() -> {
            JavaFxLearningWorkspace workspace = new JavaFxLearningWorkspace();
            var first = workspace.createHoverController(new CodeArea(), () -> null);
            var second = workspace.createHoverController(new CodeArea(), () -> null);

            assertSame(workspace.rendererForTest(), workspace.rendererForTest());
            assertSame(workspace.surfaceForTest(), workspace.surfaceForTest());
            assertEquals(600, workspace.rendererForTest().widthForTest());
            assertEquals(500, workspace.rendererForTest().heightForTest());
            assertEquals(Priority.ALWAYS, VBox.getVgrow(workspace.surfaceForTest()));

            first.dispose();
            second.dispose();
            workspace.dispose();
        });
    }

    private static void runInFx(ThrowingRunnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(10, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
