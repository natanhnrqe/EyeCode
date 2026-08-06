package com.eyecode.javafx.ui.toolwindow.content;

import com.eyecode.javafx.explorer.JavaFxExplorer;
import com.eyecode.project.model.ProjectModel;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.File;

public final class ProjectToolWindowContent extends VBox {

    public ProjectToolWindowContent() {
        this(ProjectModel.fromDirectory(new File(".")));
    }

    public ProjectToolWindowContent(ProjectModel model) {
        getStyleClass().add("toolwindow-content");
        setFillWidth(true);

        JavaFxExplorer explorer = new JavaFxExplorer(model);
        VBox.setVgrow(explorer, Priority.ALWAYS);

        getChildren().add(explorer);
    }
}