package com.eyecode.learning.render;

import com.eyecode.browser.BrowserService;
import com.eyecode.browser.BrowserToolWindow;
import com.eyecode.learning.content.LearningContentEngine;

public final class LearningRenderer {

    private static BrowserService browserService;
    private static BrowserToolWindow browserToolWindow;
    private static LearningContentEngine contentEngine;

    private LearningRenderer() {
    }

    public static void initialize(BrowserService bs, BrowserToolWindow btw) {
        browserService = bs;
        browserToolWindow = btw;
        contentEngine = new LearningContentEngine();
    }

    public static boolean isInitialized() {
        return browserService != null;
    }

    public static void showLesson(String resourcePath) {
        if (browserService == null) return;
        String html = contentEngine.loadHtml(resourcePath);
        showHtml(html);
    }

    public static void showHtml(String html) {
        if (browserService == null) return;
        browserService.showLearningContent(html);
        if (browserToolWindow != null) {
            browserToolWindow.openIfNecessary();
        }
    }
}
