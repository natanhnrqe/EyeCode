package com.eyecode.javafx.ui.toolwindow.content;

public final class LearnToolWindowContent extends ToolWindowPlaceholderContent {

    public LearnToolWindowContent() {
        super("Learn");
        addSection("Lesson Atual", placeholder("Select a learning concept from the editor to open its contextual card."));
    }
}
