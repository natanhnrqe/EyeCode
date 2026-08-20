package com.eyecode.javafx.ui.toolwindow.content;

import com.eyecode.javafx.ceffx.CeffxRuntime;
import com.techsenger.ceffx.core.CefClient;
import com.techsenger.ceffx.core.browser.CefBrowser;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class JavaFxCeffxPreview extends Region {

    static final String PREVIEW_HTML = """
            <!doctype html>
            <html>
              <head>
                <meta charset="UTF-8">
                <style>
                  html, body { margin: 0; padding: 0; background: #1e1f22; color: #ffffff; font-family: sans-serif; }
                  body { padding: 24px; }
                </style>
              </head>
              <body><h1>EyeCode CEFFX Preview</h1></body>
            </html>
            """;

    private BrowserAdapter browser;
    private boolean ceffxBacked;
    private boolean disposed;

    public JavaFxCeffxPreview() {
        getStyleClass().add("ceffx-preview");
        setPrefSize(800, 600);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        ceffxBacked = true;
        createCeffxBrowser();
    }

    JavaFxCeffxPreview(BrowserFactory browserFactory) {
        getStyleClass().add("ceffx-preview");
        setPrefSize(800, 600);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        try {
            attach(browserFactory.create(), true);
        } catch (Throwable failure) {
            showFailure(failure);
        }
    }

    @Override
    protected void layoutChildren() {
        if (!getChildren().isEmpty()) {
            Node child = getChildren().getFirst();
            child.resizeRelocate(0, 0, getWidth(), getHeight());
        }
    }

    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        BrowserAdapter current = browser;
        browser = null;
        if (current != null) {
            disposeBrowser(current);
        }
    }

    Node hostedNodeForTest() {
        return getChildren().isEmpty() ? null : getChildren().getFirst();
    }

    interface BrowserFactory {
        BrowserAdapter create();
    }

    interface BrowserAdapter {
        Node node();

        void loadHtml(String html);

        void dispose();
    }

    private void createCeffxBrowser() {
        CeffxRuntime.runLater(() -> {
            try {
                CefClient client = CeffxRuntime.app().createClient();
                CefBrowser cefBrowser = client.createBrowser(previewUrl(), true, false);
                cefBrowser.createImmediately();
                BrowserAdapter created = new CeffxBrowserAdapter(client, cefBrowser);
                Platform.runLater(() -> attach(created, false));
            } catch (Throwable failure) {
                Platform.runLater(() -> showFailure(failure));
            }
        });
    }

    private void attach(BrowserAdapter created, boolean loadHtml) {
        if (disposed) {
            disposeBrowser(created);
            return;
        }
        if (loadHtml) {
            created.loadHtml(PREVIEW_HTML);
        }
        browser = created;
        getChildren().setAll(created.node());
        requestLayout();
    }

    private void disposeBrowser(BrowserAdapter current) {
        if (ceffxBacked) {
            CeffxRuntime.runLater(current::dispose);
        } else {
            current.dispose();
        }
    }

    private void showFailure(Throwable failure) {
        if (disposed) {
            return;
        }
        Label error = new Label("Preview initialization failed: " + failure.getMessage());
        error.getStyleClass().add("toolwindow-placeholder");
        getChildren().setAll(error);
    }

    private static String previewUrl() {
        String encoded = Base64.getEncoder().encodeToString(PREVIEW_HTML.getBytes(StandardCharsets.UTF_8));
        return "data:text/html;base64," + encoded;
    }

    private static final class CeffxBrowserAdapter implements BrowserAdapter {
        private final CefClient client;
        private final CefBrowser browser;
        private boolean disposed;

        private CeffxBrowserAdapter(CefClient client, CefBrowser browser) {
            this.client = client;
            this.browser = browser;
        }

        @Override
        public Node node() {
            return browser.getPane();
        }

        @Override
        public void loadHtml(String html) {
        }

        @Override
        public void dispose() {
            if (disposed) {
                return;
            }
            disposed = true;
            browser.close(true);
            client.dispose();
        }
    }
}
