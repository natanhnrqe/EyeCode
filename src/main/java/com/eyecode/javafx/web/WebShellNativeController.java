package com.eyecode.javafx.web;

import java.util.Map;

public final class WebShellNativeController {
    private final WebShellNativeUi nativeUi;

    public WebShellNativeController(WebShellSurface surface, WebShellNativeUi nativeUi) {
        this.nativeUi = nativeUi == null ? WebShellNativeUi.unavailable() : nativeUi;
        surface.registerHandler("native", "windowMinimize", this::minimize);
        surface.registerHandler("native", "windowToggleMaximize", this::toggleMaximize);
        surface.registerHandler("native", "windowClose", this::close);
    }

    private WebShellEnvelope minimize(WebShellEnvelope message) {
        nativeUi.minimizeWindow();
        return message.response(Map.of("accepted", true));
    }

    private WebShellEnvelope toggleMaximize(WebShellEnvelope message) {
        nativeUi.toggleMaximizeWindow();
        return message.response(Map.of("accepted", true));
    }

    private WebShellEnvelope close(WebShellEnvelope message) {
        nativeUi.closeWindow();
        return message.response(Map.of("accepted", true));
    }
}
