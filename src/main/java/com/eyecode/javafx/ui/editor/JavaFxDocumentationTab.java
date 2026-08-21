package com.eyecode.javafx.ui.editor;

import com.eyecode.learning.content.DocumentationTarget;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class JavaFxDocumentationTab extends VBox {

    private final JavaFxDocumentationSurface surface;
    private final Label title = new Label("Documentation");
    private final Label location = new Label();

    public JavaFxDocumentationTab(JavaFxDocumentationSurface surface) {
        this.surface = surface;
        getStyleClass().add("documentation-tab-content");
        Button reload = new Button("Reload");
        reload.setOnAction(event -> surface.reload());
        title.getStyleClass().add("documentation-tab-title");
        location.getStyleClass().add("documentation-tab-location");
        VBox labels = new VBox(title, location);
        HBox header = new HBox(reload, labels);
        header.getStyleClass().add("documentation-tab-header");
        header.setSpacing(10);
        VBox.setVgrow(surface, Priority.ALWAYS);
        getChildren().addAll(header, surface);
    }

    public void open(DocumentationTarget target) {
        title.setText(target.label());
        location.setText(target.url());
        surface.open(target.url());
    }

    public void dispose() {
        surface.dispose();
    }

    JavaFxDocumentationSurface surfaceForTest() {
        return surface;
    }

    String titleForTest() {
        return title.getText();
    }
}
