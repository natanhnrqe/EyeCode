package com.eyecode.learning.browser;

import com.eyecode.browser.BrowserManager;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefRequestContext;

import java.awt.Component;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

public final class LearningBrowserService {

    private final CefBrowser browser;

    public LearningBrowserService() {
        var client = BrowserManager.getInstance().createClient();
        browser = client.createBrowser("about:blank", false, false);
    }

    public LearningBrowserService(CefBrowser browser) {
        this.browser = browser;
    }

    public void loadHtml(String html) {
        String encoded = Base64.getEncoder().encodeToString(
                normalizeHtml(html).getBytes(StandardCharsets.UTF_8));
        browser.loadURL("data:text/html;charset=UTF-8;base64," + encoded);
    }

    public void loadUrl(String url) {
        if (url != null) {
            browser.loadURL(url);
        }
    }

    public void reload() {
        browser.reload();
    }

    public void executeJs(String script) {
        if (script == null || script.isBlank()) return;
        browser.executeJavaScript(script, browser.getURL(), 0);
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
        return browser.getUIComponent();
    }

    public void dispose() {
        browser.stopLoad();
        browser.close(true);
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
