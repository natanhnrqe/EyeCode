package com.eyecode.learning.browser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LearningBrowserServiceLifecycleTest {

    @Test
    void loadHtmlBeforeBrowserReady_staysPending() {
        LearningBrowserService svc = new LearningBrowserService(true);
        FakeCefBrowser fake = new FakeCefBrowser();

        svc.loadHtml("<p>hello</p>");

        assertFalse(svc.isBrowserReady(), "browser should not be ready yet");
        assertNotNull(svc.pendingHtml(), "HTML must be pending while browser is not ready");
        assertTrue(fake.getLoadedUrls().isEmpty(),
                "no loadURL should be issued before onAfterCreated");
    }

    @Test
    void onAfterCreated_loadsPendingHtml() {
        LearningBrowserService svc = new LearningBrowserService(true);
        FakeCefBrowser fake = new FakeCefBrowser();

        svc.loadHtml("<p>hello world</p>");
        svc.simulateOnAfterCreated(fake);

        assertTrue(svc.isBrowserReady());
        assertEquals(1, fake.getLoadedUrls().size(),
                "pending HTML must be loaded exactly once after onAfterCreated");
        assertNull(svc.pendingHtml(), "pendingHtml should be cleared after loading");
        assertTrue(fake.getLoadedUrls().get(0).startsWith("data:text/html"),
                "loaded URL must be a data URL");
    }

    @Test
    void loadHtmlAfterBrowserReady_loadsImmediately() {
        LearningBrowserService svc = new LearningBrowserService(true);
        FakeCefBrowser fake = new FakeCefBrowser();
        svc.simulateOnAfterCreated(fake);
        assertTrue(fake.getLoadedUrls().isEmpty());

        svc.loadHtml("<p>second</p>");

        assertEquals(1, fake.getLoadedUrls().size(),
                "loadHtml after ready must loadURL immediately");
        assertNull(svc.pendingHtml(), "nothing should be pending after ready load");
    }

    @Test
    void onAfterCreated_withoutPendingHtml_doesNotArtificiallyLoad() {
        LearningBrowserService svc = new LearningBrowserService(true);
        FakeCefBrowser fake = new FakeCefBrowser();

        svc.simulateOnAfterCreated(fake);

        assertEquals(0, fake.getLoadedUrls().size(),
                "onAfterCreated with no pending HTML must NOT trigger any loadURL");
        assertNull(svc.pendingHtml());
    }

    @Test
    void dispose_marksBrowserNotReady() {
        LearningBrowserService svc = new LearningBrowserService(true);
        FakeCefBrowser fake = new FakeCefBrowser();
        svc.simulateOnAfterCreated(fake);
        assertTrue(svc.isBrowserReady());

        svc.dispose();

        assertFalse(svc.isBrowserReady(), "after dispose browserReady must be false");
        assertNull(svc.getBrowser(), "browser reference must be invalidated after dispose");
    }

    @Test
    void dispose_clearsPendingHtml() {
        LearningBrowserService svc = new LearningBrowserService(true);
        svc.loadHtml("<p>pending</p>");
        assertNotNull(svc.pendingHtml());

        svc.dispose();

        assertNull(svc.pendingHtml(), "dispose must clear pendingHtml");
        assertFalse(svc.isBrowserReady());
    }

    @Test
    void disposedBrowserReference_isNotReused() {
        LearningBrowserService svc = new LearningBrowserService(true);
        FakeCefBrowser first = new FakeCefBrowser();
        svc.simulateOnAfterCreated(first);
        svc.dispose();

        svc.loadHtml("<p>second life</p>");
        assertFalse(svc.isBrowserReady());
        assertNotNull(svc.pendingHtml());
        assertEquals(0, first.getLoadedUrls().size(),
                "the disposed browser must not receive any loadURL after re-open");
    }

    @Test
    void reopenAfterDispose_canRecreateAndLoad() {
        LearningBrowserService first = new LearningBrowserService(true);
        FakeCefBrowser firstFake = new FakeCefBrowser();
        first.simulateOnAfterCreated(firstFake);
        first.loadHtml("<p>first doc</p>");
        assertEquals(1, firstFake.getLoadedUrls().size());
        first.dispose();

        LearningBrowserService second = new LearningBrowserService(true);
        FakeCefBrowser secondFake = new FakeCefBrowser();
        second.loadHtml("<p>second doc</p>");
        assertFalse(second.isBrowserReady());
        second.simulateOnAfterCreated(secondFake);

        assertTrue(second.isBrowserReady());
        assertEquals(1, secondFake.getLoadedUrls().size(),
                "second browser must load the newly-pending HTML");
        assertNotSame(firstFake, secondFake);
    }

    @Test
    void multipleLoadHtmlBeforeReady_onlyLatestStaysPending() {
        LearningBrowserService svc = new LearningBrowserService(true);
        FakeCefBrowser fake = new FakeCefBrowser();

        svc.loadHtml("<p>v1</p>");
        svc.loadHtml("<p>v2</p>");
        svc.loadHtml("<p>v3</p>");
        svc.simulateOnAfterCreated(fake);

        assertEquals(1, fake.getLoadedUrls().size(),
                "only one loadURL must happen after the browser becomes ready");
        assertNull(svc.pendingHtml());
    }

    @Test
    void loadHtmlIsIdempotentAcrossOpenCloseOpenCycle() {
        LearningBrowserService svc = new LearningBrowserService(true);
        FakeCefBrowser fake = new FakeCefBrowser();

        svc.loadHtml("<p>A</p>");
        svc.simulateOnAfterCreated(fake);
        assertEquals(1, fake.getLoadedUrls().size());
        svc.dispose();

        FakeCefBrowser fake2 = new FakeCefBrowser();
        svc.loadHtml("<p>B</p>");
        svc.simulateOnAfterCreated(fake2);
        assertEquals(1, fake2.getLoadedUrls().size());
        assertNotSame(fake, fake2, "cycle must use a brand-new browser instance");
    }
}
