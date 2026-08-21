package com.eyecode.javafx.ui.editor;

import com.eyecode.learning.content.DocumentationTarget;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxDocumentationWorkspaceTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void reusesOneTabAndBrowserSurfaceAcrossTargets() throws Exception {
        runInFx(() -> {
            AtomicInteger creates = new AtomicInteger();
            AtomicInteger navigations = new AtomicInteger();
            JavaFxDocumentationSurface surface = new JavaFxDocumentationSurface((url, policy) -> {
                creates.incrementAndGet();
                return new FakeBrowserAdapter(navigations);
            });
            JavaFxDocumentationWorkspace workspace = new JavaFxDocumentationWorkspace(surface);
            JavaFxDocumentationTab tab = workspace.tabForTest();

            workspace.open(new DocumentationTarget("String", "https://docs.oracle.com/string"));
            workspace.open(new DocumentationTarget("ArrayList", "https://docs.oracle.com/list"));

            assertSame(tab, workspace.tabForTest());
            assertSame(surface, workspace.tabForTest().surfaceForTest());
            assertEquals(1, creates.get());
            assertEquals("ArrayList", tab.titleForTest());
            assertEquals("https://docs.oracle.com/list", surface.currentUrlForTest());
            assertEquals(2, navigations.get());
            workspace.dispose();
        });
    }

    @Test
    void closeDisposesTheCurrentSurfaceAndReopenCreatesFreshResources() throws Exception {
        runInFx(() -> {
            AtomicInteger creates = new AtomicInteger();
            AtomicInteger disposals = new AtomicInteger();
            JavaFxDocumentationWorkspace workspace = new JavaFxDocumentationWorkspace(
                    () -> new JavaFxDocumentationSurface((url, policy) -> {
                        creates.incrementAndGet();
                        return new FakeBrowserAdapter(new AtomicInteger(), disposals);
                    }));

            workspace.open(new DocumentationTarget("String", "https://docs.oracle.com/string"));
            JavaFxDocumentationTab first = workspace.tabForTest();
            workspace.closeTab();

            assertEquals(1, disposals.get());
            assertTrue(!workspace.hasTabForTest());

            workspace.open(new DocumentationTarget("List", "https://docs.oracle.com/list"));
            assertTrue(workspace.hasTabForTest());
            assertTrue(first != workspace.tabForTest());
            assertEquals(2, creates.get());
            workspace.dispose();
            assertEquals(2, disposals.get());
        });
    }

    private static void runInFx(Runnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(10, TimeUnit.SECONDS));
    }

    private static final class FakeBrowserAdapter implements JavaFxDocumentationSurface.BrowserAdapter {
        private final AtomicInteger navigations;
        private final AtomicInteger disposals;

        private FakeBrowserAdapter(AtomicInteger navigations) {
            this(navigations, new AtomicInteger());
        }

        private FakeBrowserAdapter(AtomicInteger navigations, AtomicInteger disposals) {
            this.navigations = navigations;
            this.disposals = disposals;
        }

        @Override
        public javafx.scene.Node node() {
            return new Label("Documentation");
        }

        @Override
        public void navigate(String url) {
            navigations.incrementAndGet();
        }

        @Override
        public void reload() {
        }

        @Override
        public void dispose() {
            disposals.incrementAndGet();
        }
    }
}
