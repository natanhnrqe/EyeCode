package com.eyecode.javafx.ui.editor;

import com.eyecode.javafx.ceffx.CeffxRuntime;
import com.techsenger.ceffx.core.CefClient;
import com.techsenger.ceffx.core.browser.CefBrowser;
import com.techsenger.ceffx.core.browser.CefFrame;
import com.techsenger.ceffx.core.callback.CefAuthCallback;
import com.techsenger.ceffx.core.callback.CefCallback;
import com.techsenger.ceffx.core.handler.CefLoadHandler;
import com.techsenger.ceffx.core.handler.CefRequestHandler;
import com.techsenger.ceffx.core.handler.CefResourceRequestHandler;
import com.techsenger.ceffx.core.misc.BoolRef;
import com.techsenger.ceffx.core.network.CefRequest;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

import java.util.function.Consumer;

public final class JavaFxDocumentationSurface extends Region {

    private BrowserAdapter browser;
    private String url;
    private boolean disposed;
    private final BrowserFactory browserFactory;

    public JavaFxDocumentationSurface() {
        this(null);
    }

    JavaFxDocumentationSurface(BrowserFactory browserFactory) {
        this.browserFactory = browserFactory;
        getStyleClass().add("documentation-browser");
        setMinSize(0, 0);
        setPrefSize(800, 600);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public void open(String targetUrl) {
        if (disposed) {
            return;
        }
        url = targetUrl;
        if (browser == null) {
            createBrowser();
        } else {
            navigate(browser, targetUrl);
        }
    }

    public void reload() {
        if (!disposed && browser != null) {
            browser.reload();
        }
    }

    public String currentUrlForTest() {
        return url;
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

    BrowserAdapter browserForTest() {
        return browser;
    }

    @Override
    protected void layoutChildren() {
        if (!getChildren().isEmpty()) {
            getChildren().getFirst().resizeRelocate(0, 0, getWidth(), getHeight());
        }
    }

    interface BrowserFactory {
        BrowserAdapter create(String url, Consumer<String> navigationPolicy);
    }

    interface BrowserAdapter {
        Node node();

        void navigate(String url);

        void reload();

        void dispose();
    }

    private void createBrowser() {
        if (browserFactory != null) {
            try {
                attach(browserFactory.create(url, JavaFxDocumentationSurface::isSupportedUrl));
            } catch (Throwable failure) {
                showFailure(failure);
            }
            return;
        }
        try {
            CeffxRuntime.runLater(() -> {
                try {
                    CefClient client = CeffxRuntime.app().createClient();
                    client.addRequestHandler(new DocumentationRequestHandler());
                    CefBrowser cefBrowser = client.createBrowser(url, true, false);
                    cefBrowser.createImmediately();
                    Platform.runLater(() -> attach(new CeffxBrowserAdapter(client, cefBrowser)));
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
            created.dispose();
            return;
        }
        browser = created;
        getChildren().setAll(created.node());
        requestLayout();
        navigate(created, url);
    }

    private void navigate(BrowserAdapter target, String targetUrl) {
        if (targetUrl != null && isSupportedUrl(targetUrl)) {
            if (browserFactory == null) {
                CeffxRuntime.runLater(() -> target.navigate(targetUrl));
            } else {
                target.navigate(targetUrl);
            }
        }
    }

    private void disposeBrowser(BrowserAdapter target) {
        if (browserFactory == null) {
            CeffxRuntime.runLater(target::dispose);
        } else {
            target.dispose();
        }
    }

    private void showFailure(Throwable failure) {
        if (!disposed) {
            Label error = new Label("Documentation failed to initialize: " + failure.getMessage());
            error.getStyleClass().add("toolwindow-placeholder");
            getChildren().setAll(error);
        }
    }

    private static boolean isSupportedUrl(String targetUrl) {
        if (targetUrl == null) {
            return false;
        }
        String scheme = java.net.URI.create(targetUrl).getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
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
        public void navigate(String url) {
            browser.loadURL(url);
        }

        @Override
        public void reload() {
            browser.reload();
        }

        @Override
        public void dispose() {
            if (!disposed) {
                disposed = true;
                browser.close(true);
                client.dispose();
            }
        }
    }

    private final class DocumentationRequestHandler implements CefRequestHandler {
        @Override
        public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request,
                                      boolean userGesture, boolean isRedirect) {
            return !isSupportedUrl(request.getURL());
        }

        @Override
        public boolean onOpenURLFromTab(CefBrowser browser, CefFrame frame, String targetUrl,
                                        boolean userGesture) {
            return !isSupportedUrl(targetUrl);
        }

        @Override
        public CefResourceRequestHandler getResourceRequestHandler(CefBrowser browser,
                CefFrame frame, CefRequest request, boolean isNavigation, boolean isDownload,
                String requestInitiator, BoolRef disableDefaultHandling) {
            return null;
        }

        @Override
        public boolean getAuthCredentials(CefBrowser browser, String originUrl, boolean isProxy,
                String host, int port, String realm, String scheme, CefAuthCallback callback) {
            return false;
        }

        @Override
        public boolean onCertificateError(CefBrowser browser, CefLoadHandler.ErrorCode certError,
                String requestUrl, CefCallback callback) {
            return false;
        }

        @Override
        public void onRenderProcessTerminated(CefBrowser browser,
                CefRequestHandler.TerminationStatus status, int errorCode, String errorString) {
        }
    }
}
