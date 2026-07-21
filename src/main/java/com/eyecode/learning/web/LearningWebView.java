package com.eyecode.learning.web;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
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

    private final WebEngine webEngine;
    private final WebView view;

    public LearningWebView() {
        setLayout(new BorderLayout());
        setOpaque(false);

        JFXPanel fxPanel = new JFXPanel();
        add(fxPanel, BorderLayout.CENTER);

        view = new WebView();
        webEngine = view.getEngine();

        webEngine.setJavaScriptEnabled(true);
        view.setContextMenuEnabled(false);
        view.setZoom(1.0);
        view.setFocusTraversable(true);

        Platform.runLater(() -> {
            fxPanel.setScene(new Scene(view));
            view.setStyle("-fx-background-color: transparent;");
        });
    }

    public void loadHtml(String html) {
        if (html == null) {
            return;
        }
        var finalHtml = injectResources(html);
        Platform.runLater(() -> webEngine.loadContent(finalHtml, "text/html"));
    }

    public void loadDocument(String html) {
        if (html == null) {
            return;
        }
        var finalHtml = injectResources(html);
        Platform.runLater(() -> webEngine.loadContent(finalHtml, "text/html"));
    }

    public void scrollToTop() {
        Platform.runLater(() ->
                webEngine.executeScript("window.scrollTo(0,0)"));
    }

    private String injectResources(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        var css = loadCss();
        var js = loadJs();
        var result = html;
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
