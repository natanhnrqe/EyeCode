package com.eyecode.ui.web;

import java.util.Map;

public final class WebShellNativeController {
    private final WebShellWindowControls windowControls;

    public WebShellNativeController(WebShellSurface surface, WebShellWindowControls windowControls) {
        this.windowControls = windowControls == null ? WebShellNativeUi.unavailable() : windowControls;
        surface.registerHandler("native", "windowMinimize", this::minimize);
        surface.registerHandler("native", "windowToggleMaximize", this::toggleMaximize);
        surface.registerHandler("native", "windowClose", this::close);
        surface.registerHandler("native", "windowDragStart", this::startDrag);
        surface.registerHandler("native", "windowDragMove", this::moveDrag);
        surface.registerHandler("native", "windowDragEnd", this::endDrag);
    }

    private WebShellEnvelope minimize(WebShellEnvelope message) {
        windowControls.minimizeWindow();
        return message.response(Map.of("accepted", true));
    }

    private WebShellEnvelope toggleMaximize(WebShellEnvelope message) {
        windowControls.toggleMaximizeWindow();
        return message.response(Map.of("accepted", true));
    }

    private WebShellEnvelope close(WebShellEnvelope message) {
        windowControls.closeWindow();
        return message.response(Map.of("accepted", true));
    }

    private WebShellEnvelope startDrag(WebShellEnvelope message) {
        windowControls.beginWindowDrag(coordinate(message, "screenX"), coordinate(message, "screenY"));
        return message.response(Map.of("accepted", true));
    }

    private WebShellEnvelope moveDrag(WebShellEnvelope message) {
        windowControls.moveWindow(coordinate(message, "screenX"), coordinate(message, "screenY"));
        return message.response(Map.of("accepted", true));
    }

    private WebShellEnvelope endDrag(WebShellEnvelope message) {
        windowControls.endWindowDrag();
        return message.response(Map.of("accepted", true));
    }

    private static int coordinate(WebShellEnvelope message, String name) {
        Object value = message.payload().get(name);
        return value instanceof Number number ? number.intValue() : 0;
    }
}
