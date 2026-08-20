package com.eyecode.javafx.ui.toolwindow.content;

import com.eyecode.learning.content.LearningContentEngine;

public final class LearnToolWindowContent extends ToolWindowPlaceholderContent {

    private static final String DEFAULT_LESSON = "java/basics/variables";

    private final JavaFxCeffxLearningSurface learningSurface;

    public LearnToolWindowContent() {
        super("Learn");
        learningSurface = new JavaFxCeffxLearningSurface();
        learningSurface.showHtml(new LearningContentEngine().loadHtmlByIdentifier(DEFAULT_LESSON));
        addSection("Lesson Atual", learningSurface);
    }

    public void dispose() {
        learningSurface.dispose();
    }
}
