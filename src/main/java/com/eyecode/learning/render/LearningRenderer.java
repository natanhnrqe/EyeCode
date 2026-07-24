package com.eyecode.learning.render;

import com.eyecode.learning.content.LearningContentEngine;

public final class LearningRenderer {

    private LearningRenderer() {
    }

    public static String renderLesson(String resourcePath) {
        var engine = new LearningContentEngine();
        return engine.loadHtml(resourcePath);
    }

    public static String renderMarkdown(String markdown) {
        var engine = new LearningContentEngine();
        return engine.convert(markdown);
    }
}
