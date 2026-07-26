package com.eyecode.learning.ui;

public final class HoverDiagnosticLogger {

    private static final boolean ENABLED = false;

    public static void log(String message) {
        if (ENABLED) {
            System.out.println("[HOVER] " + message);
        }
    }

    public static void logSurfaceMove(int offset) {
        log("surface.mouseMoved offset=" + offset);
    }

    public static void logControllerOffset(int offset) {
        log("controller.onOffsetChanged offset=" + offset);
    }

    public static void logStateChange(String from, String to) {
        log("state " + from + " -> " + to);
    }

    public static void logRendererShow() {
        log("renderer.show() called");
    }

    public static void logRendererHide() {
        log("renderer.hide() called");
    }

    public static void logPopupShow() {
        log("popup.show()");
    }

    public static void logPopupHide() {
        log("popup.hide()");
    }

    public static void logPopupVisible(boolean visible) {
        log("popup.isVisible()=" + visible);
    }

    public static void logCardVisible(boolean visible) {
        log("card.isVisible()=" + visible);
    }

    public static void logCardRender() {
        log("card.render() called");
    }

    public static void logConceptResolved(boolean found) {
        log("concept resolved=" + found);
    }
}
