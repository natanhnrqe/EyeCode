package com.eyecode.javafx.ui.toolwindow.content;

public final class RoadmapToolWindowContent extends ToolWindowPlaceholderContent {

    public RoadmapToolWindowContent() {
        super("Roadmap");
        addSection("Current Track", placeholder("Nenhuma trilha ativa"));
        addSection("Progress", placeholder("0% concluído"));
        addSection("Lessons", placeholder("Nenhuma lição ainda"));
        addSection("Modules", placeholder("Nenhum módulo ainda"));
    }
}