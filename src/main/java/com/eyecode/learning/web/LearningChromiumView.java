package com.eyecode.learning.web;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class LearningChromiumView extends JPanel {

    private static final String LOADING_CARD = "loading";
    private static final String BROWSER_CARD = "browser";
    private static final String ERROR_CARD = "error";
    private static final String DEFAULT_DEMO_HTML = """
            <!doctype html>
            <html>
            <head>
            <meta charset="UTF-8">
            <title>Learning Chromium</title>
            </head>
            <body>
            <h1>Chromium ready</h1>
            <p>The Learning browser is running.</p>
            </body>
            </html>
            """;
    private static final Object APP_LOCK = new Object();

    private static volatile CefApp cefApp;

    private final CardLayout cardLayout;
    private final JPanel contentPanel;
    private final JPanel browserPanel;
    private final JTextArea errorArea;
    private final AtomicBoolean disposed;
    private final AtomicReference<String> pendingHtml;

    private volatile CefClient client;
    private volatile CefBrowser browser;
    private volatile Component browserComponent;

    public LearningChromiumView() {
        setLayout(new BorderLayout());
        setOpaque(false);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setOpaque(false);

        JPanel loadingPanel = new JPanel(new BorderLayout());
        loadingPanel.setOpaque(false);
        JLabel loadingLabel = new JLabel("Initializing Chromium...");
        loadingPanel.add(loadingLabel, BorderLayout.CENTER);

        browserPanel = new JPanel(new BorderLayout());
        browserPanel.setOpaque(false);

        errorArea = new JTextArea();
        errorArea.setEditable(false);
        errorArea.setOpaque(false);
        errorArea.setForeground(Color.RED);
        errorArea.setLineWrap(true);
        errorArea.setWrapStyleWord(true);
        JScrollPane errorScrollPane = new JScrollPane(errorArea);
        errorScrollPane.setBorder(null);
        errorScrollPane.setOpaque(false);
        errorScrollPane.getViewport().setOpaque(false);

        contentPanel.add(loadingPanel, LOADING_CARD);
        contentPanel.add(browserPanel, BROWSER_CARD);
        contentPanel.add(errorScrollPane, ERROR_CARD);

        add(contentPanel, BorderLayout.CENTER);

        disposed = new AtomicBoolean(false);
        pendingHtml = new AtomicReference<>();

        showLoading();
        initializeAsync();
    }

    public void loadHtml(String html) {
        String requestedHtml = html == null ? "" : html;
        pendingHtml.set(requestedHtml);
        if (disposed.get() || !isBrowserReady()) {
            return;
        }
        runOnEdtAsync(() -> {
            if (!disposed.get() && isBrowserReady()) {
                loadHtmlInternal(requestedHtml);
            }
        });
    }

    public void dispose() {
        if (!disposed.compareAndSet(false, true)) {
            return;
        }
        pendingHtml.set(null);
        runOnEdtBlocking(this::disposeOnEdt);
    }

    private void initializeAsync() {
        Thread initThread = new Thread(() -> {
            try {
                CefApp app = ensureCefApp();
                if (disposed.get()) {
                    return;
                }
                runOnEdtBlocking(() -> createBrowserOnEdt(app));
            } catch (Throwable error) {
                runOnEdtAsync(() -> showError(error));
            }
        }, "LearningChromiumView-init");
        initThread.setDaemon(true);
        initThread.start();
    }

    private CefApp ensureCefApp() throws Exception {
        synchronized (APP_LOCK) {
            if (cefApp != null) {
                return cefApp;
            }

            CefSettings settings = new CefSettings();
            File cacheDir = new File(System.getProperty("java.io.tmpdir"), "eyecode-learning-jcef");
            cacheDir.mkdirs();
            settings.cache_path = cacheDir.getAbsolutePath();
            settings.windowless_rendering_enabled = false;
            settings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_WARNING;
            try {
                cefApp = CefApp.getInstance(settings);
                return cefApp;
            } catch (UnsatisfiedLinkError error) {
                throw new IllegalStateException(
                        "JCEF could not be initialized. Run EyeCode with the JetBrains Runtime (JBR) that includes Chromium support.",
                        error);
            }
        }
    }

    private void createBrowserOnEdt(CefApp app) {
        if (disposed.get()) {
            return;
        }

        client = app.createClient();
        browser = client.createBrowser("about:blank", false, false);
        browserComponent = browser.getUIComponent();
        browserPanel.removeAll();
        browserPanel.add(browserComponent, BorderLayout.CENTER);
        browserPanel.revalidate();
        browserPanel.repaint();
        cardLayout.show(contentPanel, BROWSER_CARD);
        contentPanel.revalidate();
        contentPanel.repaint();

        String html = pendingHtml.getAndSet(null);
        if (html == null) {
            html = DEFAULT_DEMO_HTML;
        }
        loadHtmlInternal(html);
    }

    private void loadHtmlInternal(String html) {
        if (!isBrowserReady()) {
            return;
        }
        String normalizedHtml = normalizeHtml(html);
        String dataUrl = "data:text/html;charset=UTF-8;base64,"
                + Base64.getEncoder().encodeToString(normalizedHtml.getBytes(StandardCharsets.UTF_8));
        browser.loadURL(dataUrl);
    }

    private String normalizeHtml(String html) {
        if (html == null) {
            return blankDocument();
        }

        String trimmed = html.trim();
        if (trimmed.isEmpty()) {
            return blankDocument();
        }

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

    private String blankDocument() {
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

    private boolean isBrowserReady() {
        return !disposed.get() && browser != null && browserComponent != null;
    }

    private void disposeOnEdt() {
        if (browser != null) {
            try {
                browser.stopLoad();
            } catch (Exception ignored) {
            }
            try {
                browser.close(true);
            } catch (Exception ignored) {
            }
            browser = null;
        }

        if (browserComponent != null) {
            browserPanel.remove(browserComponent);
            browserComponent = null;
        }

        if (client != null) {
            try {
                client.dispose();
            } catch (Exception ignored) {
            }
            client = null;
        }

        showLoading();
    }

    private void showLoading() {
        cardLayout.show(contentPanel, LOADING_CARD);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showError(Throwable error) {
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        errorArea.setText("Failed to initialize Chromium.\n\n"
                + writer.toString().trim());
        cardLayout.show(contentPanel, ERROR_CARD);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void runOnEdtAsync(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        SwingUtilities.invokeLater(action);
    }

    private void runOnEdtBlocking(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(action);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Chromium initialization interrupted", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new IllegalStateException("Chromium initialization failed", cause);
        }
    }
}
