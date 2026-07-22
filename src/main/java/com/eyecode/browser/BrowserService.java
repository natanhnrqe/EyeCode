package com.eyecode.browser;

import org.cef.browser.CefBrowser;

import java.awt.Component;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

public final class BrowserService {

    private final CefBrowser browser;

    public BrowserService(CefBrowser browser) {
        this.browser = browser;
    }

    public void loadUrl(String url) {
        if (url != null) {
            browser.loadURL(url);
        }
    }

    public void loadHtml(String html) {
        String encoded = Base64.getEncoder().encodeToString(normalizeHtml(html).getBytes(StandardCharsets.UTF_8));
        browser.loadURL("data:text/html;charset=UTF-8;base64," + encoded);
    }

    public void reload() {
        browser.reload();
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
