package com.eyecode.chromium;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefContextMenuHandler;
import org.cef.handler.CefDisplayHandler;
import org.cef.handler.CefFocusHandler;
import org.cef.handler.CefKeyboardHandler;
import org.cef.handler.CefLifeSpanHandler;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefRequestHandler;
import org.cef.network.CefRequest;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;

public final class ChromiumDemoFrame extends JFrame {

    private static final String HTML_OK = "<html><body>OK</body></html>";

    private CefClient client;
    private CefBrowser browser;
    private Component browserComponent;

    public ChromiumDemoFrame(CefApp cefApp) {
        super("EyeCode");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(1280, 800);
        setLocationRelativeTo(null);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                disposeChromium();
            }
        });

        createClient(cefApp);
        createBrowserNow();
        showAndLoad();
    }

    private void createClient(CefApp cefApp) {
        client = cefApp.createClient();
        addHandlers();
    }

    private void addHandlers() {
        client.addLifeSpanHandler(new CefLifeSpanHandler() {
            @Override public boolean onBeforePopup(CefBrowser b, CefFrame f, String t, String n) { return false; }
            @Override public void onAfterCreated(CefBrowser b) {}
            @Override public void onAfterParentChanged(CefBrowser b) {}
            @Override public boolean doClose(CefBrowser b) { return false; }
            @Override public void onBeforeClose(CefBrowser b) {}
        });

        client.addLoadHandler(new CefLoadHandler() {
            @Override public void onLoadingStateChange(CefBrowser b, boolean loading, boolean back, boolean fwd) {}
            @Override public void onLoadStart(CefBrowser b, CefFrame f, CefRequest.TransitionType t) {}
            @Override public void onLoadEnd(CefBrowser b, CefFrame f, int code) {}
            @Override public void onLoadError(CefBrowser b, CefFrame f, ErrorCode e, String msg, String url) {}
        });

        client.addDisplayHandler(new CefDisplayHandler() {
            @Override public void onAddressChange(CefBrowser b, CefFrame f, String url) {}
            @Override public void onTitleChange(CefBrowser b, String t) {}
            @Override public void onFullscreenModeChange(CefBrowser b, boolean f) {}
            @Override public boolean onTooltip(CefBrowser b, String t) { return false; }
            @Override public void onStatusMessage(CefBrowser b, String v) {}
            @Override public boolean onConsoleMessage(CefBrowser b, org.cef.CefSettings.LogSeverity l, String msg, String src, int line) { return false; }
            @Override public boolean onCursorChange(CefBrowser b, int c) {
                if (browserComponent != null) {
                    browserComponent.setCursor(switch (c) {
                        case 0  -> Cursor.getDefaultCursor();
                        case 2  -> Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
                        case 3  -> Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
                        case 4  -> Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
                        case 6, 13 -> Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
                        case 7, 10 -> Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
                        case 8  -> Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
                        case 9  -> Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
                        case 11 -> Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
                        case 12 -> Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
                        case 29 -> Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
                        case 1  -> Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
                        default -> Cursor.getDefaultCursor();
                    });
                }
                return true; }
        });

        client.addRequestHandler(new CefRequestHandler() {
            @Override public boolean onBeforeBrowse(CefBrowser b, CefFrame f, CefRequest r, boolean u, boolean i) { return false; }
            @Override public boolean onOpenURLFromTab(CefBrowser b, CefFrame f, String url, boolean u) { return false; }
            @Override public org.cef.handler.CefResourceRequestHandler getResourceRequestHandler(
                    CefBrowser b, CefFrame f, CefRequest r, boolean nav, boolean dl, String init, org.cef.misc.BoolRef d) { return null; }
            @Override public boolean getAuthCredentials(CefBrowser b, String o, boolean p, String h, int port, String realm, String s, org.cef.callback.CefAuthCallback cb) { return false; }
            @Override public boolean onCertificateError(CefBrowser b, org.cef.handler.CefLoadHandler.ErrorCode e, String url, org.cef.security.CefSSLInfo i, org.cef.callback.CefCallback cb) { return false; }
            @Override public void onRenderProcessTerminated(CefBrowser b, TerminationStatus status, int code, String msg) {}
        });

        client.addFocusHandler(new CefFocusHandler() {
            @Override public void onTakeFocus(CefBrowser b, boolean next) {}
            @Override public boolean onSetFocus(CefBrowser b, FocusSource s) { return false; }
            @Override public void onGotFocus(CefBrowser b) {}
        });

        client.addContextMenuHandler(new CefContextMenuHandler() {
            @Override public void onBeforeContextMenu(CefBrowser b, CefFrame f, org.cef.callback.CefContextMenuParams p, org.cef.callback.CefMenuModel m) {}
            @Override public boolean runContextMenu(CefBrowser b, CefFrame f, org.cef.callback.CefContextMenuParams p, org.cef.callback.CefMenuModel m, org.cef.callback.CefRunContextMenuCallback cb) { return false; }
            @Override public boolean onContextMenuCommand(CefBrowser b, CefFrame f, org.cef.callback.CefContextMenuParams p, int c, int e) { return false; }
            @Override public void onContextMenuDismissed(CefBrowser b, CefFrame f) {}
        });

        client.addKeyboardHandler(new CefKeyboardHandler() {
            @Override public boolean onPreKeyEvent(CefBrowser b, org.cef.handler.CefKeyboardHandler.CefKeyEvent e, org.cef.misc.BoolRef r) { return false; }
            @Override public boolean onKeyEvent(CefBrowser b, org.cef.handler.CefKeyboardHandler.CefKeyEvent e) { return false; }
        });
    }

    private void createBrowserNow() {
        try {
            browser = client.createBrowser("about:blank", false, false);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return;
        }
        if (browser == null) return;

        try {
            browserComponent = browser.getUIComponent();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return;
        }
        if (browserComponent == null) return;

        add(browserComponent, BorderLayout.CENTER);
        validate();
        repaint();
    }

    private void showAndLoad() {
        setVisible(true);

        String pageUrl = System.getProperty("eyecode.jcef.url",
            "data:text/html;charset=UTF-8," + java.net.URLEncoder.encode(HTML_OK, java.nio.charset.StandardCharsets.UTF_8));
        Timer loadTimer = new Timer(200, e -> {
            browser.loadURL(pageUrl);
        });
        loadTimer.setRepeats(false);
        loadTimer.start();
    }

    private void disposeChromium() {
        if (browser != null) {
            browser.close(true);
            browser = null;
        }
        if (browserComponent != null) {
            remove(browserComponent);
            browserComponent = null;
        }
        client.dispose();
        System.exit(0);
    }

    private static void pureSwingTest(String title) {
        JFrame f = new JFrame(title);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(400, 300);
        f.setLocationRelativeTo(null);
        f.add(new JLabel("Swing only mode", JLabel.CENTER));
        f.setVisible(true);
    }

    public static void main(String[] args) {
        boolean pureSwing = Boolean.getBoolean("eyecode.swing.only");
        if (pureSwing) {
            SwingUtilities.invokeLater(() -> pureSwingTest("EyeCode - Swing Only"));
            return;
        }

        String javaHome = System.getProperty("java.home");
        java.io.File helperFile = new java.io.File(javaHome, "bin/jcef_helper.exe");

        String[] cefArgs = {"--no-sandbox"};

        if (!CefApp.startup(cefArgs)) {
            System.err.println("JCEF startup() failed");
            return;
        }

        org.cef.CefSettings settings = new org.cef.CefSettings();
        settings.windowless_rendering_enabled = false;
        settings.browser_subprocess_path = helperFile.getAbsolutePath();
        settings.log_file = new java.io.File(System.getProperty("java.io.tmpdir"), "cef_debug.log").getAbsolutePath();
        settings.log_severity = org.cef.CefSettings.LogSeverity.LOGSEVERITY_VERBOSE;
        settings.cache_path = new java.io.File(System.getProperty("java.io.tmpdir"), "eyecode-jcef-cache").getAbsolutePath();
        new java.io.File(settings.cache_path).mkdirs();

        CefApp app = CefApp.getInstance(cefArgs, settings);

        long deadline = System.currentTimeMillis() + 15_000;
        CefApp.CefAppState state = CefApp.getState();
        while (state != CefApp.CefAppState.INITIALIZED) {
            if (System.currentTimeMillis() > deadline) {
                System.err.println("JCEF init timeout");
                return;
            }
            try { Thread.sleep(100); } catch (InterruptedException ex) { break; }
            state = CefApp.getState();
        }

        SwingUtilities.invokeLater(() -> new ChromiumDemoFrame(app));
    }
}
