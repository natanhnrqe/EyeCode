package com.eyecode.javafx.ui.toolwindow.content;

public final class PreviewToolWindowContent extends ToolWindowPlaceholderContent {

    private final JavaFxCeffxPreview preview;

    public PreviewToolWindowContent() {
        super("Preview");
        preview = new JavaFxCeffxPreview();
        addSection("Preview", preview);
    }

    public void dispose() {
        preview.dispose();
    }
}
