package com.eyecode.javafx.web;

import com.eyecode.javafx.ceffx.CeffxRuntime;
import com.techsenger.ceffx.core.CefClient;
import com.techsenger.ceffx.core.CefSettings;
import com.techsenger.ceffx.core.browser.CefBrowser;
import com.techsenger.ceffx.core.browser.CefFrame;
import com.techsenger.ceffx.core.browser.CefMessageRouter;
import com.techsenger.ceffx.core.callback.CefQueryCallback;
import com.techsenger.ceffx.core.handler.CefCursorUtils;
import com.techsenger.ceffx.core.handler.CefDisplayHandlerAdapter;
import com.techsenger.ceffx.core.handler.CefMessageRouterHandlerAdapter;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class JavaFxWebShellSurface extends Region implements WebShellSurface {
    private final WebShellAssetResolver assetResolver;
    private final WebShellProtocolCodec codec;
    private final WebShellDispatcher dispatcher;
    private final List<Runnable> readyListeners = new CopyOnWriteArrayList<>();
    private CefClient client;
    private CefBrowser browser;
    private CefMessageRouter router;
    private Node browserNode;
    private boolean disposed;
    private int mouseMovesToLog;

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

    @Override
    public void registerHandler(String channel, String name, WebShellMessageHandler handler) {
        if (disposed) return;
        dispatcher.register(channel, name, handler);
    }

    public void addReadyListener(Runnable listener) {
        if (listener != null && !disposed) {
            readyListeners.add(listener);
        }
    }

    @Override
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
            for (Runnable listener : readyListeners) {
                try {
                    listener.run();
                } catch (RuntimeException ignored) {
                }
            }
            Map<String, Object> bootstrap = new LinkedHashMap<>();
            bootstrap.put("protocolVersion", WebShellEnvelope.PROTOCOL);
            bootstrap.put("platform", System.getProperty("os.name", "unknown"));
            bootstrap.put("webShellMode", WebShellMode.WEB_SHELL.name());
            String initialFile = System.getProperty("eyecode.webshell.initialFile");
            if (initialFile != null && !initialFile.isBlank()) {
                bootstrap.put("initialFile", initialFile.trim());
            }
            send(WebShellEnvelope.event("shell", "bootstrap", bootstrap));
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
                    createdClient.addDisplayHandler(new CeffxInputDiagnosticHandler());
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
        browserNode.addEventFilter(MouseEvent.ANY, this::traceFxMouseEvent);
        browserNode.setManaged(true);
        if (browserNode instanceof Region region) {
            region.setMinSize(0, 0);
            region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }
        getChildren().setAll(browserNode);
        System.out.println("WEB_SHELL loaded");
        requestLayout();
    }

    private void traceFxMouseEvent(MouseEvent event) {
        if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
            mouseMovesToLog = 3;
        } else if (event.getEventType() == MouseEvent.MOUSE_RELEASED) {
            mouseMovesToLog = 3;
        } else if ((event.getEventType() == MouseEvent.MOUSE_MOVED
                || event.getEventType() == MouseEvent.MOUSE_DRAGGED) && mouseMovesToLog-- <= 0) {
            return;
        }
        System.out.printf("[CEFFX-MOUSE] FX %s button=%s primaryDown=%s middleDown=%s secondaryDown=%s x=%.1f y=%.1f%n",
                event.getEventType().getName(), event.getButton(), event.isPrimaryButtonDown(),
                event.isMiddleButtonDown(), event.isSecondaryButtonDown(), event.getX(), event.getY());
    }

    private static final class CeffxInputDiagnosticHandler extends CefDisplayHandlerAdapter {
        @Override
        public boolean onConsoleMessage(CefBrowser browser, CefSettings.LogSeverity level, String message,
                                        String source, int line) {
            if (message != null && message.startsWith("[WEB-MOUSE]")) {
                System.out.printf("%s source=%s:%d%n", message, source, line);
            }
            return false;
        }

        @Override
        public boolean onCursorChange(CefBrowser browser, int cursorType) {
            Cursor mapped = CefCursorUtils.getCursor(cursorType);
            System.out.printf("[CEFFX-CURSOR] requested=%d mapped=%s applied=false%n", cursorType, mapped);
            return false;
        }
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
