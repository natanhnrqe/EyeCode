package com.eyecode.javafx.ui.toolwindow.content;

import com.eyecode.javafx.ceffx.CeffxDataUrl;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxCeffxLearningSurfaceTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void replacesBrowserContentWithoutRecreatingItsNode() throws Exception {
        TestBrowser browser = new TestBrowser();
        runInFx(() -> {
            JavaFxCeffxLearningSurface surface = new JavaFxCeffxLearningSurface(() -> browser);
            surface.showHtml("<h1>Variables</h1>");

            assertSame(browser.node, surface.hostedNodeForTest());
            assertEquals("<h1>Variables</h1>", browser.html);
            assertEquals(0, browser.loadCount);
            assertEquals(1, browser.updateCount);
            surface.showHtml("<h1>Methods</h1>");
            assertSame(browser.node, surface.hostedNodeForTest());
            assertEquals("<h1>Methods</h1>", browser.html);
            assertEquals(2, browser.updateCount);
            surface.dispose();
        });
    }

    @Test
    void disposeIsIdempotent() throws Exception {
        TestBrowser browser = new TestBrowser();
        runInFx(() -> {
            JavaFxCeffxLearningSurface surface = new JavaFxCeffxLearningSurface(() -> browser);
            surface.dispose();
            surface.dispose();
            assertEquals(1, browser.disposeCount);
        });
    }

    @Test
    void latestContentBeforeAttachIsLoadedOnceWhenBrowserArrives() throws Exception {
        TestBrowser browser = new TestBrowser();
        runInFx(() -> {
            JavaFxCeffxLearningSurface surface =
                    new JavaFxCeffxLearningSurface(() -> browser, false);
            surface.showHtml("<h1>Old</h1>");
            surface.showHtml("<h1>Latest</h1>");

            surface.attachForTest(browser);

            assertEquals("<h1>Latest</h1>", browser.html);
            assertEquals(0, browser.loadCount);
            assertEquals(1, browser.updateCount);
            surface.dispose();
        });
    }

    @Test
    void disposeBeforeAttachDisposesALateBrowserImmediately() throws Exception {
        TestBrowser browser = new TestBrowser();
        runInFx(() -> {
            JavaFxCeffxLearningSurface surface =
                    new JavaFxCeffxLearningSurface(() -> browser, false);
            surface.dispose();
            surface.attachForTest(browser);

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
        assertTrue(done.await(10, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class TestBrowser implements JavaFxCeffxLearningSurface.BrowserAdapter {
        private final Label node = new Label();
        private String html;
        private int disposeCount;
        private int loadCount;
        private int updateCount;

        @Override
        public javafx.scene.Node node() {
            return node;
        }

        @Override
        public void loadHtml(String html) {
            this.html = html;
            loadCount++;
        }

        @Override
        public void updateHtml(String html) {
            this.html = html;
            updateCount++;
        }

        @Override
        public void dispose() {
            disposeCount++;
        }
    }

}
class CeffxDataUrlTest {

    @Test
    void encodesUtf8HtmlAsBase64DataUrl() {
        String result = CeffxDataUrl.html("<h1>Olá</h1>");

        assertTrue(result.startsWith("data:text/html;base64,"));

        String encoded = result.substring("data:text/html;base64,".length());

        String decoded = new String(
                Base64.getDecoder().decode(encoded),
                StandardCharsets.UTF_8
        );

        assertEquals("<h1>Olá</h1>", decoded);
    }

    @Test
    void nullBecomesEmptyHtmlContent() {
        assertEquals(
                "data:text/html;base64,",
                CeffxDataUrl.html(null)
        );
    }
}
