package com.eyecode.learning.web;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public final class LearningWebView extends JPanel {

    private static final String CSS_RESOURCE = "/learning/css/learning.css";
    private static final String JS_RESOURCE = "/learning/js/learning.js";

    private volatile WebEngine webEngine;
    private volatile boolean fxInitialized;

    public LearningWebView() {
        setLayout(new BorderLayout());
        setOpaque(false);

        JFXPanel fxPanel = new JFXPanel();
        add(fxPanel, BorderLayout.CENTER);

        Platform.runLater(() -> {
            WebView view = new WebView();
            webEngine = view.getEngine();
            webEngine.setJavaScriptEnabled(true);
            view.setContextMenuEnabled(false);
            view.setZoom(1.0);
            view.setFocusTraversable(true);
            var scene = new Scene(view, Color.rgb(43, 45, 48));
            fxPanel.setScene(scene);
            fxInitialized = true;
        });
    }

    public void loadHtml(String html) {
        if (html == null) {
            return;
        }
        ensureFx(() -> {
            var finalHtml = injectResources(html);
            webEngine.loadContent(finalHtml, "text/html");
        });
    }

    public void loadDocument(String html) {
        if (html == null) {
            return;
        }
        ensureFx(() -> {
            var finalHtml = injectResources(html);
            webEngine.loadContent(finalHtml, "text/html");
        });
    }

    public void scrollToTop() {
        if (webEngine != null) {
            Platform.runLater(() ->
                    webEngine.executeScript("window.scrollTo(0,0)"));
        }
    }

    private void ensureFx(Runnable action) {
        if (fxInitialized) {
            Platform.runLater(action);
        } else {
            Platform.runLater(() -> ensureFx(action));
        }
    }

    private String injectResources(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        var css = loadCss();
        var js = loadJs();
        var result = html;
        if (!result.contains("color-scheme")) {
            result = result.replace("<meta charset=\"UTF-8\">",
                    "<meta charset=\"UTF-8\">" + System.lineSeparator()
                            + "<meta name=\"color-scheme\" content=\"dark\">");
        }
        if (css != null && !css.isBlank() && !result.contains("<style>")) {
            var styleTag = "<style>" + System.lineSeparator()
                    + css + System.lineSeparator()
                    + "</style>" + System.lineSeparator();
            result = result.replace("</head>", styleTag + "</head>");
        }
        if (js != null && !js.isBlank() && !result.contains("<script>")) {
            var scriptTag = "<script>" + System.lineSeparator()
                    + js + System.lineSeparator()
                    + "</script>" + System.lineSeparator();
            result = result.replace("</body>", scriptTag + "</body>");
        }
        return result;
    }

    private String loadCss() {
        return loadResource(CSS_RESOURCE);
    }

    private String loadJs() {
        return loadResource(JS_RESOURCE);
    }

    private String loadResource(String path) {
        InputStream stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("Resource not found: " + path);
        }
        try (var reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read resource: " + path, e);
        }
    }
}
