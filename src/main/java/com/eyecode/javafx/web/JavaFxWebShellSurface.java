package com.eyecode.javafx.web;

import com.eyecode.javafx.ceffx.CeffxRuntime;
import com.techsenger.ceffx.core.CefClient;
import com.techsenger.ceffx.core.browser.CefBrowser;
import com.techsenger.ceffx.core.browser.CefFrame;
import com.techsenger.ceffx.core.browser.CefMessageRouter;
import com.techsenger.ceffx.core.callback.CefQueryCallback;
import com.techsenger.ceffx.core.handler.CefMessageRouterHandlerAdapter;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

import java.util.Map;

public final class JavaFxWebShellSurface extends Region {
    private final WebShellAssetResolver assetResolver;
    private final WebShellProtocolCodec codec;
    private final WebShellDispatcher dispatcher;
    private CefClient client;
    private CefBrowser browser;
    private CefMessageRouter router;
    private Node browserNode;
    private boolean disposed;

    public JavaFxWebShellSurface() {
        this(new WebShellAssetResolver());
    }

    JavaFxWebShellSurface(WebShellAssetResolver assetResolver) {
        this.assetResolver = assetResolver == null ? new WebShellAssetResolver() : assetResolver;
        this.codec = new WebShellProtocolCodec();
        this.dispatcher = new WebShellDispatcher();
        getStyleClass().add("web-shell-surface");
        setMinSize(0, 0);
        setPrefSize(0, 0);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        registerHandlers();
        System.out.println("WEB_SHELL created");
        createBrowser();
    }

    public boolean isDisposed() {
        return disposed;
    }

    public boolean isBrowserAttached() {
        return browser != null && browserNode != null;
    }

    public String entryUrl() {
        return assetResolver.entryUrl();
    }

    public void send(WebShellEnvelope message) {
        if (message == null || disposed) return;
        String json = codec.encode(message);
        try {
            CeffxRuntime.runLater(() -> {
                CefBrowser current = browser;
                if (!disposed && current != null) {
                    current.executeJavaScript("window.eyeCodeBridge.receive(" + json + ")",
                            current.getURL(), 0);
                }
            });
        } catch (IllegalStateException ignored) {
        }
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        System.out.println("WEB_SHELL disposed");
        CefBrowser currentBrowser = browser;
        CefClient currentClient = client;
        CefMessageRouter currentRouter = router;
        browser = null;
        client = null;
        router = null;
        browserNode = null;
        if (currentBrowser != null || currentClient != null) {
            try {
                CeffxRuntime.runLater(() -> {
                    if (currentRouter != null) currentRouter.dispose();
                    if (currentBrowser != null) currentBrowser.close(true);
                    if (currentClient != null) currentClient.dispose();
                });
            } catch (IllegalStateException ignored) {
                if (currentRouter != null) currentRouter.dispose();
            }
        }
        getChildren().clear();
    }

    @Override
    protected void layoutChildren() {
        if (browserNode != null) {
            browserNode.resizeRelocate(0, 0, getWidth(), getHeight());
        }
    }

    private void registerHandlers() {
        dispatcher.register("shell", "ping", message -> message.response(Map.of("message", "pong")));
        dispatcher.register("shell", "ready", message -> {
            send(WebShellEnvelope.event("shell", "bootstrap", Map.of(
                    "protocolVersion", WebShellEnvelope.PROTOCOL,
                    "platform", System.getProperty("os.name", "unknown"),
                    "webShellMode", WebShellMode.WEB_SHELL.name())));
            return message.response(Map.of("accepted", true));
        });
    }

    private void createBrowser() {
        try {
            CeffxRuntime.runLater(() -> {
                CefClient createdClient = null;
                CefMessageRouter createdRouter = null;
                CefBrowser createdBrowser = null;
                try {
                    createdClient = CeffxRuntime.app().createClient();
                    createdRouter = CefMessageRouter.create(new RouterHandler());
                    createdClient.addMessageRouter(createdRouter);
                    createdBrowser = createdClient.createBrowser(assetResolver.entryUrl(), true, false);
                    createdBrowser.createImmediately();
                    CefClient attachedClient = createdClient;
                    CefBrowser attachedBrowser = createdBrowser;
                    CefMessageRouter attachedRouter = createdRouter;
                    Platform.runLater(() -> attach(attachedClient, attachedBrowser, attachedRouter));
                } catch (Throwable failure) {
                    disposeCreated(createdRouter, createdBrowser, createdClient);
                    showFailureLater(failure);
                }
            });
        } catch (Throwable failure) {
            showFailure(failure);
        }
    }

    private void disposeCreated(CefMessageRouter createdRouter, CefBrowser createdBrowser,
                                CefClient createdClient) {
        if (createdRouter != null) createdRouter.dispose();
        if (createdBrowser != null) createdBrowser.close(true);
        if (createdClient != null) createdClient.dispose();
    }

    private void showFailureLater(Throwable failure) {
        try {
            Platform.runLater(() -> showFailure(failure));
        } catch (IllegalStateException ignored) {
        }
    }

    private void attach(CefClient createdClient, CefBrowser createdBrowser, CefMessageRouter createdRouter) {
        if (disposed) {
            try {
                CeffxRuntime.runLater(() -> disposeCreated(createdRouter, createdBrowser, createdClient));
            } catch (IllegalStateException ignored) {
                disposeCreated(createdRouter, createdBrowser, createdClient);
            }
            return;
        }
        client = createdClient;
        browser = createdBrowser;
        router = createdRouter;
        browserNode = createdBrowser.getPane();
        browserNode.setManaged(true);
        if (browserNode instanceof Region region) {
            region.setMinSize(0, 0);
            region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }
        getChildren().setAll(browserNode);
        System.out.println("WEB_SHELL loaded");
        requestLayout();
    }

    private void showFailure(Throwable failure) {
        if (disposed) return;
        Label label = new Label("Web Shell failed to initialize: "
                + (failure.getMessage() == null ? failure : failure.getMessage()));
        label.getStyleClass().add("toolwindow-placeholder");
        getChildren().setAll(label);
    }

    private final class RouterHandler extends CefMessageRouterHandlerAdapter {
        @Override
        public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request,
                               boolean persistent, CefQueryCallback callback) {
            try {
                WebShellEnvelope message = codec.decode(request);
                WebShellEnvelope response = dispatcher.dispatch(message);
                callback.success(response == null ? "{}" : codec.encode(response));
            } catch (IllegalArgumentException exception) {
                callback.failure(400, exception.getMessage() == null ? "Invalid Web Shell message" : exception.getMessage());
            } catch (RuntimeException exception) {
                callback.failure(500, exception.getMessage() == null ? "Web Shell dispatch failed" : exception.getMessage());
            }
            return true;
        }
    }
}
