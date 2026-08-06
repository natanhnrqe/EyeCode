package com.eyecode.javafx.ui.toolwindow.content;

public final class PreviewToolWindowContent extends ToolWindowPlaceholderContent {

    public PreviewToolWindowContent() {
        super("Preview");
        addSection("Preview",
                placeholder("HTML • Markdown • FXML • Image preview"),
                placeholder("Pré-visualização disponível em breve"));
    }
}