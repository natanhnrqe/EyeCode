package com.eyecode.javafx.ui.editor;

import com.eyecode.language.documentation.JdkSourceTarget;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxJdkSourceTabTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void sourceTabsKeepIndependentReadOnlyContentAndRevealDeclaration() throws Exception {
        runInFx(() -> {
            JdkSourceTarget stringTarget = new JdkSourceTarget(
                    "java.lang.String", "java.base", "java.base/java/lang/String.java", "String.java");
            JdkSourceTarget objectTarget = new JdkSourceTarget(
                    "java.lang.Object", "java.base", "java.base/java/lang/Object.java", "Object.java");
            String stringSource = "package java.lang;\npublic final class String {}";
            String objectSource = "package java.lang;\npublic class Object {}";

            JavaFxJdkSourceTab stringTab = new JavaFxJdkSourceTab(stringTarget, stringSource);
            JavaFxJdkSourceTab objectTab = new JavaFxJdkSourceTab(objectTarget, objectSource);
            try {
                assertNotSame(stringTab.editor(), objectTab.editor());
                assertEquals(stringSource, stringTab.editor().getText());
                assertEquals(objectSource, objectTab.editor().getText());
                assertFalse(stringTab.editor().getCodeArea().isEditable());
                assertFalse(objectTab.editor().getCodeArea().isEditable());
                assertEquals(stringSource.indexOf("class String"),
                        stringTab.editor().getCodeArea().getCaretPosition());
            } finally {
                stringTab.dispose();
                objectTab.dispose();
            }
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
