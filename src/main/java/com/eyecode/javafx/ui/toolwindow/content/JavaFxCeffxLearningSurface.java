package com.eyecode.javafx.ui.toolwindow.content;

import com.eyecode.javafx.ceffx.CeffxDataUrl;
import com.eyecode.javafx.ceffx.CeffxRuntime;
import com.techsenger.ceffx.core.CefClient;
import com.techsenger.ceffx.core.browser.CefBrowser;
import com.techsenger.ceffx.core.browser.CefFrame;
import com.techsenger.ceffx.core.browser.CefMessageRouter;
import com.techsenger.ceffx.core.callback.CefAuthCallback;
import com.techsenger.ceffx.core.callback.CefCallback;
import com.techsenger.ceffx.core.callback.CefQueryCallback;
import com.techsenger.ceffx.core.handler.CefMessageRouterHandlerAdapter;
import com.techsenger.ceffx.core.handler.CefRequestHandler;
import com.techsenger.ceffx.core.handler.CefResourceRequestHandler;
import com.techsenger.ceffx.core.handler.CefLoadHandler;
import com.techsenger.ceffx.core.misc.BoolRef;
import com.techsenger.ceffx.core.network.CefRequest;
import com.eyecode.learning.content.LearningLink;
import com.eyecode.learning.html.LearningHtmlBuilder;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

import java.util.function.Consumer;

public final class JavaFxCeffxLearningSurface extends Region {

    private BrowserAdapter browser;
    private String html = "";
    private boolean ceffxBacked;
    private boolean disposed;
    private boolean browserReady;
    private Consumer<String> navigationListener;

    public JavaFxCeffxLearningSurface() {
        getStyleClass().add("ceffx-learning-surface");
        setPrefSize(800, 600);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        ceffxBacked = true;
        createBrowser();
    }

    JavaFxCeffxLearningSurface(BrowserFactory browserFactory) {
        this(browserFactory, true);
    }

    JavaFxCeffxLearningSurface(BrowserFactory browserFactory, boolean attachImmediately) {
        getStyleClass().add("ceffx-learning-surface");
        setPrefSize(800, 600);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        if (attachImmediately) {
            try {
                attach(browserFactory.create());
            } catch (Throwable failure) {
                showFailure(failure);
            }
        }
    }

    public void showHtml(String newHtml) {
        if (disposed) {
            return;
        }
        html = newHtml == null ? "" : newHtml;
        BrowserAdapter current = browser;
        if (current != null && browserReady) {
            updateHtml(current, html);
        }
    }

    public void clear() {
        showHtml("");
    }

    public void setInternalNavigationListener(Consumer<String> listener) {
        navigationListener = listener;
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
        browserReady = false;
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

    void attachForTest(BrowserAdapter created) {
        attach(created);
    }

    interface BrowserFactory {
        BrowserAdapter create();
    }

    interface BrowserAdapter {
        Node node();

        void loadHtml(String html);

        default void updateHtml(String html) {
            loadHtml(html);
        }

        default boolean isReady() {
            return true;
        }

        void dispose();
    }

    private void createBrowser() {
        try {
            CeffxRuntime.runLater(() -> {
                try {
                    CefClient client = CeffxRuntime.app().createClient();
                    CefMessageRouter router = CefMessageRouter.create(new LearningRouterHandler());
                    client.addMessageRouter(router);
                    client.addRequestHandler(new LearningRequestHandler());

                    CefBrowser cefBrowser =
                            client.createBrowser(
                                    CeffxDataUrl.html(new LearningHtmlBuilder().buildShell()),
                                    true,
                                    false
                            );

                    cefBrowser.createImmediately();

                    Platform.runLater(
                            () -> attach(
                                            new CeffxBrowserAdapter(
                                            client,
                                            cefBrowser,
                                            router
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
        browserReady = created.isReady();

        getChildren().setAll(created.node());
        requestLayout();

        if (browserReady && !html.isEmpty()) {
            updateHtml(created, html);
        }
    }

    private void updateHtml(BrowserAdapter current, String content) {
        if (ceffxBacked) {
            CeffxRuntime.runLater(() -> current.updateHtml(content));
        } else {
            current.updateHtml(content);
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
        private final CefMessageRouter router;
        private boolean disposed;

        private CeffxBrowserAdapter(CefClient client, CefBrowser browser, CefMessageRouter router) {
            this.client = client;
            this.browser = browser;
            this.router = router;
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
        public void updateHtml(String html) {
            browser.executeJavaScript(
                    LearningHtmlPayload.updateScript(html),
                    browser.getURL(),
                    0
            );
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void dispose() {
            if (disposed) {
                return;
            }
            disposed = true;
            router.dispose();
            browser.close(true);
            client.dispose();
        }
    }

    private final class LearningRouterHandler extends CefMessageRouterHandlerAdapter {
        @Override
        public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request,
                               boolean persistent, CefQueryCallback callback) {
            if (request != null && request.contains("learningReady")) {
                Platform.runLater(() -> {
                    if (!disposed) {
                        browserReady = true;
                        BrowserAdapter current = JavaFxCeffxLearningSurface.this.browser;
                        if (current != null) {
                            updateHtml(current, html);
                        }
                    }
                });
            }
            callback.success("ok");
            return true;
        }
    }

    private final class LearningRequestHandler implements CefRequestHandler {

        @Override
        public boolean onBeforeBrowse(
                CefBrowser browser,
                CefFrame frame,
                CefRequest request,
                boolean userGesture,
                boolean isRedirect
        ) {
            return handle(request.getURL());
        }

        @Override
        public boolean onOpenURLFromTab(CefBrowser browser, CefFrame frame, String targetUrl, boolean userGesture) {
            return handle(targetUrl);
        }

        @Override
        public CefResourceRequestHandler getResourceRequestHandler(
                CefBrowser browser,
                CefFrame frame,
                CefRequest request,
                boolean isNavigation,
                boolean isDownload,
                String requestInitiator,
                BoolRef disableDefaultHandling
        ) {
            return null;
        }

        @Override
        public boolean getAuthCredentials(
                CefBrowser browser,
                String originUrl,
                boolean isProxy,
                String host,
                int port,
                String realm,
                String scheme,
                CefAuthCallback callback
        ) {
            return false;
        }

        @Override
        public boolean onCertificateError(
                CefBrowser browser,
                CefLoadHandler.ErrorCode certError,
                String requestUrl,
                CefCallback callback
        ) {
            return false;
        }

        @Override
        public void onRenderProcessTerminated(
                CefBrowser browser,
                CefRequestHandler.TerminationStatus status,
                int errorCode,
                String errorString
        ) {
        }

        private boolean handle(String url) {
            var identifier = LearningLink.identifier(url);
            if (identifier.isPresent()) {
                if (navigationListener != null) {
                    Platform.runLater(() -> navigationListener.accept(identifier.get()));
                }
                return true;
            }
            return url != null && !url.startsWith("data:") && !url.startsWith("about:");
        }
    }
}
