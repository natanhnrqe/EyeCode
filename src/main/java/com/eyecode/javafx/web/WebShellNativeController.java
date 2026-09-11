package com.eyecode.javafx.web;

import java.util.Map;

public final class WebShellNativeController {
    private final WebShellNativeUi nativeUi;

    public WebShellNativeController(WebShellSurface surface, WebShellNativeUi nativeUi) {
        this.nativeUi = nativeUi == null ? WebShellNativeUi.unavailable() : nativeUi;
        surface.registerHandler("native", "windowMinimize", this::minimize);
        surface.registerHandler("native", "windowToggleMaximize", this::toggleMaximize);
        surface.registerHandler("native", "windowClose", this::close);
        surface.registerHandler("native", "windowDragStart", this::startDrag);
        surface.registerHandler("native", "windowDragMove", this::moveDrag);
        surface.registerHandler("native", "windowDragEnd", this::endDrag);
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

    private WebShellEnvelope startDrag(WebShellEnvelope message) {
        nativeUi.beginWindowDrag(coordinate(message, "screenX"), coordinate(message, "screenY"));
        return message.response(Map.of("accepted", true));
    }

    private WebShellEnvelope moveDrag(WebShellEnvelope message) {
        nativeUi.moveWindow(coordinate(message, "screenX"), coordinate(message, "screenY"));
        return message.response(Map.of("accepted", true));
    }

    private WebShellEnvelope endDrag(WebShellEnvelope message) {
        nativeUi.endWindowDrag();
        return message.response(Map.of("accepted", true));
    }

    private static int coordinate(WebShellEnvelope message, String name) {
        Object value = message.payload().get(name);
        return value instanceof Number number ? number.intValue() : 0;
    }
}
