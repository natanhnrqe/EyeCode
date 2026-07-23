package com.eyecode.browser.preview;

import com.eyecode.browser.BrowserManager;

import org.cef.browser.CefBrowser;

import java.awt.Component;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

public final class PreviewBrowserService {

    private final CefBrowser browser;

    public PreviewBrowserService(CefBrowser browser) {
        this.browser = browser;
    }

    public static PreviewBrowserService create() {
        var client = BrowserManager.getInstance().getClient();
        var browser = client.createBrowser("about:blank", false, false);
        return new PreviewBrowserService(browser);
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

    public void previewHtml(String html) {
        loadHtml(html);
    }

    @Deprecated(forRemoval = true)
    public void showLearningContent(String html) {
        previewHtml(html);
    }

    public void showWelcomePage() {
        loadHtml("""
                <!doctype html>
                <html>
                <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                           background: #1e1e1e; color: #ccc; display: flex; align-items: center;
                           justify-content: center; height: 100vh; margin: 0; text-align: center; }
                    h1 { color: #4ec9b0; font-weight: 300; font-size: 2rem; margin-bottom: .5rem; }
                    p  { color: #888; font-size: 1rem; }
                </style>
                </head>
                <body>
                <div>
                    <h1>EyeCode Browser</h1>
                    <p>Nenhum conteudo HTML disponivel.</p>
                </div>
                </body>
                </html>
                """);
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
