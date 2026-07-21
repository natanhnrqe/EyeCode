package com.eyecode.learning.web;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.JPanel;
import java.awt.BorderLayout;

public final class LearningWebView extends JPanel {

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
        Platform.runLater(() -> webEngine.loadContent(html));
    }
}
