package com.eyecode.browser.preview;

import com.eyecode.browser.BrowserService;

public final class HtmlPreviewController {

    private static final String INITIAL_PAGE = """
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
                <p>Browser inicializado com sucesso.</p>
                <p>Aguardando Preview HTML...</p>
            </div>
            </body>
            </html>
            """;

    private final BrowserService browserService;

    public HtmlPreviewController(BrowserService browserService) {
        this.browserService = browserService;
        this.browserService.previewHtml(INITIAL_PAGE);
    }

    public void previewHtml(String html) {
        browserService.previewHtml(html);
    }

    public BrowserService getBrowserService() {
        return browserService;
    }
}
