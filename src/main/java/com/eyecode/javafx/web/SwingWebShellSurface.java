package com.eyecode.javafx.web;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefRendering;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.handler.CefResourceRequestHandler;
import org.cef.handler.CefResourceRequestHandlerAdapter;
import org.cef.misc.BoolRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;
import org.cef.network.CefURLRequest;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.SwingUtilities;

public final class SwingWebShellSurface implements WebShellSurface {
    private static final String CONTROL_PAGE_PROPERTY = "eyecode.swing.spike.controlPage";
    private final WebShellAssetResolver assetResolver;
    private final String explicitInitialUrl;
    private final WebShellProtocolCodec codec = new WebShellProtocolCodec();
    private final WebShellDispatcher dispatcher = new WebShellDispatcher();
    private CefApp app;
    private CefClient client;
    private CefBrowser browser;
    private CefMessageRouter router;
    private boolean disposed;
    private String initialUrl;
    private volatile int requestedCursorType = Integer.MIN_VALUE;
    private volatile int lastCursorCallbackType = Integer.MIN_VALUE;

    public SwingWebShellSurface() {
        this(null, new WebShellAssetResolver());
    }

    SwingWebShellSurface(WebShellAssetResolver assetResolver) {
        this(null, assetResolver);
    }

    public SwingWebShellSurface(String initialUrl) {
        this(initialUrl, new WebShellAssetResolver());
    }

    private SwingWebShellSurface(String initialUrl, WebShellAssetResolver assetResolver) {
        this.assetResolver = assetResolver == null ? new WebShellAssetResolver() : assetResolver;
        this.explicitInitialUrl = initialUrl == null || initialUrl.isBlank() ? null : initialUrl;
        registerHandlers();
        app = createApp();
        System.out.println("[JCEF-SPIKE] CefApp instance state=" + CefApp.getState());
        client = app.createClient();
        System.out.println("[JCEF-SPIKE] client created");
        client.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override
            public void onAfterCreated(CefBrowser created) {
                System.out.println("[JCEF-SPIKE] onAfterCreated identifier=" + created.getIdentifier()
                        + " class=" + created.getClass().getName());
            }
        });
        client.addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public boolean onConsoleMessage(CefBrowser source, CefSettings.LogSeverity level, String message,
                                            String sourceName, int line) {
                System.out.println("[JCEF-CONSOLE] level=" + level + " message=" + message
                        + " source=" + sourceName + " line=" + line);
                return false;
            }

            @Override
            public boolean onCursorChange(CefBrowser source, int cursorType) {
                int previous = lastCursorCallbackType;
                if (previous != cursorType) {
                    lastCursorCallbackType = cursorType;
                    System.out.println("[CURSOR] callback old=" + previous + " new=" + cursorType);
                }
                if (source == null) return false;
                if (requestedCursorType == cursorType) return true;
                Cursor cursor;
                try {
                    cursor = new Cursor(cursorType);
                } catch (IllegalArgumentException ignored) {
                    return false;
                }
                requestedCursorType = cursorType;
                Runnable applyCursor = () -> {
                    if (requestedCursorType != cursorType) return;
                    Component component = source.getUIComponent();
                    if (component != null) component.setCursor(cursor);
                };
                if (SwingUtilities.isEventDispatchThread()) applyCursor.run();
                else SwingUtilities.invokeLater(applyCursor);
                return true;
            }
        });
        client.addRequestHandler(new CefRequestHandlerAdapter() {
            @Override
            public CefResourceRequestHandler getResourceRequestHandler(CefBrowser source,
                                                                        org.cef.browser.CefFrame frame,
                                                                        CefRequest request,
                                                                        boolean isNavigation,
                                                                        boolean isDownload,
                                                                        String requestInitiator,
                                                                        BoolRef disableDefaultHandling) {
                return new CefResourceRequestHandlerAdapter() {
                    @Override
                    public void onResourceLoadComplete(CefBrowser resourceBrowser,
                                                       org.cef.browser.CefFrame resourceFrame,
                                                       CefRequest resourceRequest,
                                                       CefResponse response,
                                                       CefURLRequest.Status status,
                                                       long receivedContentLength) {
                        if (status != CefURLRequest.Status.UR_SUCCESS || response.getStatus() >= 400) {
                            System.out.println("[JCEF-RESOURCE] status=" + status + " http="
                                    + response.getStatus() + " url=" + resourceRequest.getURL());
                        }
                    }
                };
            }
        });
        client.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadingStateChange(CefBrowser loaded, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                System.out.println("[JCEF-SPIKE] onLoadingStateChange loading=" + isLoading
                        + " back=" + canGoBack + " forward=" + canGoForward + " url=" + loaded.getURL());
            }

            @Override
            public void onLoadStart(CefBrowser loaded, org.cef.browser.CefFrame frame,
                                    org.cef.network.CefRequest.TransitionType transitionType) {
                System.out.println("[JCEF-SPIKE] onLoadStart url=" + frame.getURL() + " transition=" + transitionType);
            }

            @Override
            public void onLoadEnd(CefBrowser loaded, org.cef.browser.CefFrame frame, int statusCode) {
                System.out.println("[JCEF-SPIKE] onLoadEnd status=" + statusCode + " url=" + frame.getURL());
                if (frame.isMain() && frame.getURL().endsWith("/webshell/index.html")) {
                    loaded.executeJavaScript("""
                            (() => {
                                const root = document.getElementById('root');
                                console.log('[JCEF-PROBE] readyState=' + document.readyState
                                    + ' location=' + document.location.href
                                    + ' bodyLength=' + document.body.innerHTML.length
                                    + ' root=' + (root ? root.outerHTML : 'null')
                                    + ' bridge=' + typeof window.eyeCodeBridge
                                    + ' scripts=' + document.scripts.length
                                    + ' rootChildren=' + (root ? root.childElementCount : 'null'));
                            })();
                            """, frame.getURL(), 0);
                }
            }

            @Override
            public void onLoadError(CefBrowser loaded, org.cef.browser.CefFrame frame,
                                    org.cef.handler.CefLoadHandler.ErrorCode errorCode,
                                    String errorText, String failedUrl) {
                System.out.println("[JCEF-SPIKE] onLoadError code=" + errorCode + " text=" + errorText
                        + " url=" + failedUrl);
            }
        });
        router = CefMessageRouter.create(new RouterHandler());
        client.addMessageRouter(router);
    }

    public void start() {
        if (disposed || browser != null) return;
        initialUrl = initialUrl();
        browser = client.createBrowser(initialUrl, CefRendering.DEFAULT, false);
        System.out.println("[JCEF-SPIKE] browser created class=" + browser.getClass().getName()
                + " windowless=" + browser.isWindowless() + " url=" + initialUrl);
        System.out.println("[JCEF-SPIKE] ui component class=" + browser.getUIComponent().getClass().getName());
        System.out.println("[JCEF-SPIKE] browser createImmediately EDT=" + javax.swing.SwingUtilities.isEventDispatchThread());
        browser.createImmediately();
        installCursorMouseDiagnostic(browser.getUIComponent());
        System.out.println("[JCEF-SPIKE] browser identifier after createImmediately=" + browser.getIdentifier());
    }

    private void installCursorMouseDiagnostic(Component component) {
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                logCursorMouseEvent("MOUSE_PRESSED", component);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                logCursorMouseEvent("MOUSE_RELEASED", component);
            }
        });
    }

    private void logCursorMouseEvent(String event, Component component) {
        System.out.println("[CURSOR] event=" + event + " componentCursor=" + component.getCursor().getType()
                + " requestedCursorType=" + requestedCursorType);
    }

    public Component component() {
        if (browser == null) throw new IllegalStateException("Swing WebShell browser has not started");
        return browser.getUIComponent();
    }

    public void logComponentState(Container container) {
        Component current = component();
        System.out.println("[JCEF-SPIKE] frame displayable=" + container.isDisplayable()
                + " showing=" + container.isShowing() + " size=" + container.getWidth() + "x" + container.getHeight());
        System.out.println("[JCEF-SPIKE] browser component displayable=" + current.isDisplayable()
                + " showing=" + current.isShowing() + " size=" + current.getWidth() + "x" + current.getHeight());
    }

    @Override
    public void send(WebShellEnvelope message) {
        CefBrowser current = browser;
        if (message == null || disposed || current == null) return;
        current.executeJavaScript("window.eyeCodeBridge.receive(" + codec.encode(message) + ")",
                current.getURL(), 0);
    }

    @Override
    public void registerHandler(String channel, String name, WebShellMessageHandler handler) {
        if (!disposed) dispatcher.register(channel, name, handler);
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        CefMessageRouter currentRouter = router;
        router = null;
        CefBrowser currentBrowser = browser;
        browser = null;
        CefClient currentClient = client;
        client = null;
        CefApp currentApp = app;
        app = null;
        if (currentRouter != null) currentRouter.dispose();
        if (currentBrowser != null) currentBrowser.close(true);
        if (currentClient != null) currentClient.dispose();
        if (currentApp != null) currentApp.dispose();
    }

    private void registerHandlers() {
        dispatcher.register("shell", "ping", message -> message.response(Map.of("message", "pong")));
        dispatcher.register("shell", "ready", message -> {
            send(WebShellEnvelope.event("shell", "bootstrap", bootstrapPayload()));
            return message.response(Map.of("accepted", true));
        });
    }

    private Map<String, Object> bootstrapPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("protocolVersion", WebShellEnvelope.PROTOCOL);
        payload.put("platform", System.getProperty("os.name", "unknown"));
        payload.put("webShellMode", WebShellMode.WEB_SHELL.name());
        String initialFile = System.getProperty("eyecode.webshell.initialFile");
        if (initialFile != null && !initialFile.isBlank()) payload.put("initialFile", initialFile.trim());
        return payload;
    }

    private static CefApp createApp() {
        String[] arguments = {"--no-sandbox"};
        boolean startup = CefApp.startup(arguments);
        System.out.println("[JCEF-SPIKE] CefApp.startup result=" + startup);
        if (!startup) throw new IllegalStateException("JCEF startup failed");
        CefSettings settings = new CefSettings();
        settings.windowless_rendering_enabled = false;
        settings.no_sandbox = true;
        settings.browser_subprocess_path = System.getProperty("java.home") + "/bin/jcef_helper.exe";
        settings.cache_path = new File(System.getProperty("java.io.tmpdir"), "eyecode-swing-jcef-spike").getAbsolutePath();
        settings.log_file = new File(System.getProperty("java.io.tmpdir"), "eyecode-swing-jcef-spike.log").getAbsolutePath();
        settings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_INFO;
        System.out.println("[JCEF-SPIKE] settings windowless=" + settings.windowless_rendering_enabled
                + " log=" + settings.log_file);
        return createCefApp(arguments, settings);
    }

    private static CefApp createCefApp(String[] arguments, CefSettings settings) {
        try {
            Method legacyFactory = CefApp.class.getMethod("getInstance", String[].class, CefSettings.class);
            return invokeCefAppFactory(legacyFactory, arguments, settings);
        } catch (NoSuchMethodException ignored) {
            try {
                Method currentFactory = CefApp.class.getMethod("getInstance", String[].class, CefSettings.class, File.class);
                return invokeCefAppFactory(currentFactory, arguments, settings, null);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unsupported JCEF CefApp bootstrap API", exception);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to initialize JCEF", exception);
        }
    }

    private static CefApp invokeCefAppFactory(Method factory, Object... arguments) throws ReflectiveOperationException {
        try {
            return (CefApp) factory.invoke(null, arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) throw runtimeException;
            if (cause instanceof Error error) throw error;
            throw exception;
        }
    }

    private String initialUrl() {
        if (Boolean.getBoolean(CONTROL_PAGE_PROPERTY)) {
            String html = """
    <html>
    <body style="padding:40px; font-family:sans-serif">

        <h1>JCEF Cursor Test</h1>

        <input
            placeholder="TEXT"
            style="font-size:20px; padding:10px">

        <br><br>

        <button
            style="cursor:pointer; font-size:20px; padding:10px">
            HAND / POINTER
        </button>

        <br><br>

        <div style="
            cursor:col-resize;
            width:300px;
            height:80px;
            background:#ddd;
            padding:20px">
            COL-RESIZE
        </div>

        <br><br>

        <div style="
            cursor:row-resize;
            width:300px;
            height:80px;
            background:#bbb;
            padding:20px">
            ROW-RESIZE
        </div>

    </body>
    </html>
    """;
            return "data:text/html;charset=UTF-8;base64,"
                    + Base64.getEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8));
        }
        if (explicitInitialUrl != null) return explicitInitialUrl;
        return assetResolver.entryUrl();
    }

    private final class RouterHandler extends CefMessageRouterHandlerAdapter {
        @Override
        public boolean onQuery(CefBrowser source, org.cef.browser.CefFrame frame, long queryId, String request,
                               boolean persistent, CefQueryCallback callback) {
            try {
                WebShellEnvelope message = codec.decode(request);
                System.out.println("SWING_WEBSHELL_SPIKE query " + message.channel() + "/" + message.name());
                WebShellEnvelope response = dispatcher.dispatch(message);
                callback.success(codec.encode(response == null
                        ? WebShellEnvelope.event("shell", "ignored", Map.of()) : response));
            } catch (IllegalArgumentException exception) {
                callback.failure(400, exception.getMessage() == null ? "Invalid WebShell request" : exception.getMessage());
            } catch (RuntimeException exception) {
                callback.failure(500, exception.getMessage() == null ? "WebShell dispatch failed" : exception.getMessage());
            }
            return true;
        }
    }
}
