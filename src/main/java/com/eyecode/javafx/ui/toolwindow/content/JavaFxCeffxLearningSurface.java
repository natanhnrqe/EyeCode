package com.eyecode.javafx.ui.toolwindow.content;

import com.eyecode.javafx.ceffx.CeffxDataUrl;
import com.eyecode.javafx.ceffx.CeffxRuntime;
import com.techsenger.ceffx.core.CefClient;
import com.techsenger.ceffx.core.browser.CefBrowser;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

public final class JavaFxCeffxLearningSurface extends Region {

    private BrowserAdapter browser;
    private String html = "";
    private boolean ceffxBacked;
    private boolean disposed;

    public JavaFxCeffxLearningSurface() {
        getStyleClass().add("ceffx-learning-surface");
        setPrefSize(800, 600);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        ceffxBacked = true;
        createBrowser();
    }

    JavaFxCeffxLearningSurface(BrowserFactory browserFactory) {
        getStyleClass().add("ceffx-learning-surface");
        setPrefSize(800, 600);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        try {
            attach(browserFactory.create());
        } catch (Throwable failure) {
            showFailure(failure);
        }
    }

    public void showHtml(String newHtml) {
        if (disposed) {
            return;
        }
        html = newHtml == null ? "" : newHtml;
        BrowserAdapter current = browser;
        if (current != null) {
            loadHtml(current, html);
        }
    }

    public void clear() {
        showHtml("");
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

    @Override
    protected void layoutChildren() {
        if (!getChildren().isEmpty()) {
            Node child = getChildren().getFirst();
            child.resizeRelocate(0, 0, getWidth(), getHeight());
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

    private void createBrowser() {
        try {
            CeffxRuntime.runLater(() -> {
                try {
                    CefClient client = CeffxRuntime.app().createClient();

                    CefBrowser cefBrowser =
                            client.createBrowser(
                                    "about:blank",
                                    true,
                                    false
                            );

                    cefBrowser.createImmediately();

                    Platform.runLater(
                            () -> attach(
                                    new CeffxBrowserAdapter(
                                            client,
                                            cefBrowser
                                    )
                            )
                    );
                } catch (Throwable failure) {
                    Platform.runLater(() -> showFailure(failure));
                }
            });
        } catch (Throwable failure) {
            showFailure(failure);
        }
    }

    private void attach(BrowserAdapter created) {
        if (disposed) {
            disposeBrowser(created);
            return;
        }

        browser = created;

        getChildren().setAll(created.node());
        requestLayout();

        loadHtml(created, html);
    }

    private void loadHtml(BrowserAdapter current, String content) {
        if (ceffxBacked) {
            CeffxRuntime.runLater(() -> current.loadHtml(content));
        } else {
            current.loadHtml(content);
        }
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
        Label error = new Label("Learning browser failed to initialize: " + failure.getMessage());
        error.getStyleClass().add("toolwindow-placeholder");
        getChildren().setAll(error);
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
            browser.loadURL(CeffxDataUrl.html(html));
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
