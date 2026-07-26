package com.eyecode.learning.service;

public final class DocumentationLifecycleLogger {
    public static final boolean ENABLED = false;

    public static void log(String msg) {
        if (ENABLED) System.out.println("[DOC] " + msg);
    }

    public static void logOpen() { log("open() called"); }
    public static void logWindowCreated() { log("JFrame created"); }
    public static void logCardCreated() { log("LearningChromiumCard created"); }
    public static void logBrowserCreated() { log("LearningBrowserService created"); }
    public static void logWindowDisplayable() { log("window.isDisplayable()=" + true); }
    public static void logWindowVisible(boolean v) { log("window.isVisible()=" + v); }
    public static void logWindowBounds(int w, int h) { log("window bounds=" + w + "x" + h); }
    public static void logLoadRequested() { log("loadHtml() requested"); }
    public static void logBrowserComponentDisplayable() { log("browser component displayable=true"); }
    public static void logBrowserComponentVisible(boolean v) { log("browser component visible=" + v); }
    public static void logFirstPaint() { log("first paint/repaint"); }
}
