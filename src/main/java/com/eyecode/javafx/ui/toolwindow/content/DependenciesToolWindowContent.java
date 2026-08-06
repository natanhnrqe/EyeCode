package com.eyecode.javafx.ui.toolwindow.content;

public final class DependenciesToolWindowContent extends ToolWindowPlaceholderContent {

    public DependenciesToolWindowContent() {
        super("Dependencies");
        addSection("Maven", placeholder("Nenhuma dependência Maven"));
        addSection("Gradle", placeholder("Nenhuma dependência Gradle"));
        addSection("Libraries", placeholder("Nenhuma biblioteca ainda"));
    }
}