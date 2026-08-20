package com.eyecode.javafx.ui.toolwindow.content;

import javafx.application.Platform;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class JavaFxCeffxPreviewTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void forwardsPreviewHtmlToBrowserAdapter() throws Exception {
        TestBrowser browser = new TestBrowser();
        runInFx(() -> {
            JavaFxCeffxPreview preview = new JavaFxCeffxPreview(() -> browser);
            assertSame(browser.node, preview.hostedNodeForTest());
            assertEquals(JavaFxCeffxPreview.PREVIEW_HTML, browser.html);
            preview.dispose();
        });
    }

    @Test
    void disposeIsIdempotent() throws Exception {
        TestBrowser browser = new TestBrowser();
        runInFx(() -> {
            JavaFxCeffxPreview preview = new JavaFxCeffxPreview(() -> browser);
            preview.dispose();
            preview.dispose();
            assertEquals(1, browser.disposeCount);
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
        assertEquals(true, done.await(10, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class TestBrowser implements JavaFxCeffxPreview.BrowserAdapter {
        private final Label node = new Label();
        private String html;
        private int disposeCount;

        @Override
        public javafx.scene.Node node() {
            return node;
        }

        @Override
        public void loadHtml(String html) {
            this.html = html;
        }

        @Override
        public void dispose() {
            disposeCount++;
        }
    }
}
