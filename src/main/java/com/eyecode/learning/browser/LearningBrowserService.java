package com.eyecode.learning.browser;

import com.eyecode.browser.BrowserManager;

import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefLifeSpanHandlerAdapter;

import java.awt.Component;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public final class LearningBrowserService {

    private final CefClient client;
    private volatile CefBrowser browser;
    private volatile boolean browserReady;
    private final AtomicReference<String> pendingHtml = new AtomicReference<>();

    public LearningBrowserService() {
        this(BrowserManager.getInstance().createClient());
    }

    LearningBrowserService(CefClient injectedClient) {
        this.client = injectedClient;
        this.client.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override
            public void onAfterCreated(CefBrowser createdBrowser) {
                if (browser == null) {
                    browser = createdBrowser;
                }
                browserReady = true;
                String pending = pendingHtml.getAndSet(null);
                if (pending != null) {
                    loadUrlInternal(pending);
                }
            }

            @Override
            public void onBeforeClose(CefBrowser closingBrowser) {
                browserReady = false;
            }
        });
        this.browser = this.client.createBrowser("about:blank", false, false);
    }

    LearningBrowserService(boolean lazy) {
        this.client = null;
    }

    void simulateOnAfterCreated(CefBrowser createdBrowser) {
        if (browser == null) {
            browser = createdBrowser;
        }
        browserReady = true;
        String pending = pendingHtml.getAndSet(null);
        if (pending != null) {
            loadUrlInternal(pending);
        }
    }

    void simulateOnBeforeClose() {
        browserReady = false;
    }

    public LearningBrowserService(CefBrowser browser) {
        this.browser = browser;
        this.client = browser.getClient();
        this.browserReady = true;
    }

    public void loadHtml(String html) {
        String dataUrl = buildDataUrl(html);
        if (browserReady) {
            loadUrlInternal(dataUrl);
        } else {
            pendingHtml.set(dataUrl);
        }
    }

    public void loadUrl(String url) {
        if (url == null) return;
        if (browserReady) {
            loadUrlInternal(url);
        } else {
            pendingHtml.set(url);
        }
    }

    private void loadUrlInternal(String url) {
        CefBrowser b = browser;
        if (b == null || !browserReady) return;
        try {
            b.loadURL(url);
        } catch (UnsatisfiedLinkError ignored) {
        }
    }

    public void reload() {
        CefBrowser b = browser;
        if (b != null && browserReady) {
            try { b.reload(); } catch (UnsatisfiedLinkError ignored) {}
        }
    }

    public void executeJs(String script) {
        if (script == null || script.isBlank()) return;
        CefBrowser b = browser;
        if (b == null || !browserReady) return;
        try {
            b.executeJavaScript(script, b.getURL(), 0);
        } catch (UnsatisfiedLinkError ignored) {}
    }

    public void scrollToAnchor(String anchor) {
        if (anchor == null || anchor.isBlank()) return;
        String js = """
                (function() {
                    var el = document.getElementById('%s') ||
                             document.querySelector('[name="%s"]');
                    if (el) {
                        el.scrollIntoView({ behavior: 'smooth', block: 'start' });
                    } else {
                        location.hash = '%s';
                    }
                })();
                """.formatted(anchor, anchor, anchor);
        executeJs(js);
    }

    public CefBrowser getBrowser() {
        return browser;
    }

    public Component getComponent() {
        CefBrowser b = browser;
        return b != null ? b.getUIComponent() : null;
    }

    public boolean isBrowserReady() {
        return browserReady;
    }

    public String pendingHtml() {
        return pendingHtml.get();
    }

    public void dispose() {
        browserReady = false;
        pendingHtml.set(null);
        CefBrowser b = browser;
        if (b != null) {
            try { b.stopLoad(); } catch (Exception ignored) {}
            try { b.close(true); } catch (Exception ignored) {}
        }
        browser = null;
    }

    private static String buildDataUrl(String html) {
        String encoded = Base64.getEncoder().encodeToString(
                normalizeHtml(html).getBytes(StandardCharsets.UTF_8));
        return "data:text/html;charset=UTF-8;base64," + encoded;
    }

    private static String normalizeHtml(String html) {
        if (html == null || html.isBlank()) {
            return blankDocument();
        }

        String trimmed = html.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("<!doctype") || lower.contains("<html")) {
            return trimmed;
        }

        return """
                <!doctype html>
                <html>
                <head>
                <meta charset="UTF-8">
                </head>
                <body>
                %s
                </body>
                </html>
                """.formatted(trimmed);
    }

    private static String blankDocument() {
        return """
                <!doctype html>
                <html>
                <head>
                <meta charset="UTF-8">
                </head>
                <body>
                </body>
                </html>
                """;
    }
}
