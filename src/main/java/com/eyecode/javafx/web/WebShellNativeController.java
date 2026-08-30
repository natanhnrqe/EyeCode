package com.eyecode.javafx.web;

import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.Map;

public final class WebShellNativeController {
    private final Stage stage;

    public WebShellNativeController(JavaFxWebShellSurface surface, Stage stage) {
        this.stage = stage;
        surface.registerHandler("native", "windowMinimize", this::minimize);
        surface.registerHandler("native", "windowToggleMaximize", this::toggleMaximize);
        surface.registerHandler("native", "windowClose", this::close);
    }

    private WebShellEnvelope minimize(WebShellEnvelope message) {
        Platform.runLater(() -> stage.setIconified(true));
        return message.response(Map.of("accepted", true));
    }

    private WebShellEnvelope toggleMaximize(WebShellEnvelope message) {
        Platform.runLater(() -> stage.setMaximized(!stage.isMaximized()));
        return message.response(Map.of("accepted", true));
    }

    private WebShellEnvelope close(WebShellEnvelope message) {
        Platform.runLater(stage::close);
        return message.response(Map.of("accepted", true));
    }
}
