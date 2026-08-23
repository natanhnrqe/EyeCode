package com.eyecode.javafx.ui.toolwindow.content;

import com.eyecode.javafx.explorer.JavaFxExplorer;
import com.eyecode.project.model.ProjectModel;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import com.eyecode.project.ProjectInfo;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public final class ProjectToolWindowContent extends VBox {

    private final Consumer<Path> fileOpenHandler;
    private final Runnable openProjectAction;
    private final Runnable newProjectAction;
    private JavaFxExplorer explorer;
    private VBox emptyState;

    public ProjectToolWindowContent() {
        this(ProjectModel.fromDirectory(new File(".")));
    }

    public ProjectToolWindowContent(ProjectModel model) {
        this(model, path -> { });
    }

    public ProjectToolWindowContent(ProjectModel model, Consumer<Path> fileOpenHandler) {
        this(model, fileOpenHandler, null, null);
    }

    public ProjectToolWindowContent(ProjectModel model,
                                    Consumer<Path> fileOpenHandler,
                                    Runnable openProjectAction,
                                    Runnable newProjectAction) {
        getStyleClass().add("toolwindow-content");
        setFillWidth(true);
        this.fileOpenHandler = fileOpenHandler == null ? path -> { } : fileOpenHandler;
        this.openProjectAction = openProjectAction;
        this.newProjectAction = newProjectAction;
        setProject(model);
    }

    public void setProject(ProjectModel model) {
        if (explorer != null) {
            getChildren().remove(explorer);
            explorer = null;
        }
        if (emptyState != null) {
            getChildren().remove(emptyState);
            emptyState = null;
        }
        if (model == null) {
            emptyState = new VBox(10);
            emptyState.getStyleClass().add("project-empty-state");
            javafx.scene.control.Label label = new javafx.scene.control.Label("No project open");
            label.getStyleClass().add("explorer-placeholder");
            emptyState.getChildren().add(label);
            javafx.scene.control.Button open = new javafx.scene.control.Button("Open Project");
            open.setOnAction(event -> {
                if (openProjectAction != null) {
                    openProjectAction.run();
                }
            });
            javafx.scene.control.Button create = new javafx.scene.control.Button("New Project");
            create.setOnAction(event -> {
                if (newProjectAction != null) {
                    newProjectAction.run();
                }
            });
            emptyState.getChildren().addAll(open, create);
            getChildren().add(emptyState);
            return;
        }
        explorer = new JavaFxExplorer(model, fileOpenHandler);
        VBox.setVgrow(explorer, Priority.ALWAYS);
        getChildren().add(explorer);
    }

    public JavaFxExplorer getExplorer() {
        return explorer;
    }

    public void refresh(ProjectModel model) {
        if (explorer != null && model != null) {
            explorer.refresh(model);
        }
    }

    public void setRecentProjects(List<ProjectInfo> projects, Consumer<ProjectInfo> recentOpenHandler) {
        if (emptyState == null) {
            return;
        }
        emptyState.getChildren().removeIf(node -> !(node instanceof javafx.scene.control.Label));
        if (projects == null || projects.isEmpty()) {
            return;
        }
        javafx.scene.control.Label heading = new javafx.scene.control.Label("Recent Projects");
        heading.getStyleClass().add("explorer-placeholder");
        emptyState.getChildren().add(heading);
        for (ProjectInfo project : projects) {
            javafx.scene.control.Button button = new javafx.scene.control.Button(project.getName());
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(event -> {
                if (recentOpenHandler != null) {
                    recentOpenHandler.accept(project);
                }
            });
            emptyState.getChildren().add(button);
        }
    }
}
