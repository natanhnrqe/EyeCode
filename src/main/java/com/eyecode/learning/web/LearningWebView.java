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

    private final WebEngine webEngine;

    public LearningWebView() {
        setLayout(new BorderLayout());
        setOpaque(false);

        JFXPanel fxPanel = new JFXPanel();
        add(fxPanel, BorderLayout.CENTER);

        WebView view = new WebView();
        webEngine = view.getEngine();

        Platform.runLater(() -> fxPanel.setScene(new Scene(view)));
    }

    public void loadHtml(String html) {
        if (html == null) {
            return;
        }
        var finalHtml = injectCss(html);
        Platform.runLater(() -> webEngine.loadContent(finalHtml, "text/html"));
    }

    public void loadDocument(String html) {
        if (html == null) {
            return;
        }
        var css = loadCss();
        var finalHtml = injectCss(html, css);
        Platform.runLater(() -> webEngine.loadContent(finalHtml, "text/html"));
    }

    private String injectCss(String html) {
        var css = loadCss();
        return injectCss(html, css);
    }

    private String injectCss(String html, String css) {
        if (html == null || html.isBlank()) {
            return "";
        }
        if (css == null || css.isBlank()) {
            return html;
        }
        if (html.contains("<style>")) {
            return html;
        }
        var styleTag = "<style>" + System.lineSeparator()
                + css + System.lineSeparator()
                + "</style>";
        var headClose = "</head>";
        var idx = html.indexOf(headClose);
        if (idx == -1) {
            return html;
        }
        return html.substring(0, idx) + styleTag + System.lineSeparator() + html.substring(idx);
    }

    private String loadCss() {
        var stream = getClass().getResourceAsStream(CSS_RESOURCE);
        if (stream == null) {
            throw new IllegalStateException("CSS resource not found: " + CSS_RESOURCE);
        }
        try (var reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read CSS resource: " + CSS_RESOURCE, e);
        }
    }
}
