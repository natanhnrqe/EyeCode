package com.eyecode.javafx.ui.toolwindow.content;

import com.eyecode.javafx.explorer.JavaFxExplorer;
import com.eyecode.javafx.explorer.ExplorerNewKind;
import com.eyecode.javafx.explorer.ExplorerNewRequest;
import com.eyecode.javafx.explorer.ProjectCreationDialog;
import com.eyecode.javafx.explorer.ProjectNode;
import com.eyecode.javafx.explorer.ProjectNode;
import com.eyecode.javafx.designsystem.JavaFxButton;
import com.eyecode.project.ProjectCreationService;
import com.eyecode.project.model.ProjectModel;
import javafx.scene.control.Alert;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import com.eyecode.project.ProjectInfo;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public final class ProjectToolWindowContent extends VBox {

    private final Consumer<Path> fileOpenHandler;
    private final Runnable openProjectAction;
    private final Runnable newProjectAction;
    private Consumer<ProjectNode> renameAction = node -> { };
    private Consumer<ProjectNode> deleteAction = node -> { };
    private final ProjectCreationService creationService = new ProjectCreationService();
    private ProjectModel project;
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
        project = model;
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
            javafx.scene.control.Button open = JavaFxButton.primary("Open Project");
            open.setOnAction(event -> {
                if (openProjectAction != null) {
                    openProjectAction.run();
                }
            });
            javafx.scene.control.Button create = JavaFxButton.create("New Project");
            create.setOnAction(event -> {
                if (newProjectAction != null) {
                    newProjectAction.run();
                }
            });
            emptyState.getChildren().addAll(open, create);
            getChildren().add(emptyState);
            return;
        }
        explorer = new JavaFxExplorer(model, fileOpenHandler, this::handleNewRequest,
                renameAction, deleteAction);
        VBox.setVgrow(explorer, Priority.ALWAYS);
        getChildren().add(explorer);
    }

    public JavaFxExplorer getExplorer() {
        return explorer;
    }

    public void setFileOperationHandlers(Consumer<ProjectNode> renameAction,
                                         Consumer<ProjectNode> deleteAction) {
        this.renameAction = renameAction == null ? node -> { } : renameAction;
        this.deleteAction = deleteAction == null ? node -> { } : deleteAction;
        if (project != null) setProject(project);
    }

    public void refresh(ProjectModel model) {
        if (explorer != null && model != null) {
            explorer.refresh(model);
        }
    }

    public void applyPathChange(Path path) {
        if (explorer != null && path != null) explorer.applyPathChange(path);
    }

    public void applyRename(Path oldPath, Path newPath) {
        if (explorer != null) explorer.applyRename(oldPath, newPath);
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

    private void handleNewRequest(ExplorerNewRequest request) {
        if (request == null || request.node() == null || project == null) {
            return;
        }
        String title = switch (request.kind()) {
            case PACKAGE -> "New Package";
            case JAVA_CLASS -> "New Java Class";
            case INTERFACE -> "New Interface";
            case ENUM -> "New Enum";
            case RECORD -> "New Record";
            case MAIN_CLASS -> "New Main Class";
            case JAVA_FILE -> "New Java File";
        };
        String prompt = request.kind() == ExplorerNewKind.PACKAGE
                ? "Package name" : "Name";
        ProjectCreationDialog dialog = new ProjectCreationDialog(title, prompt,
                request.kind() == ExplorerNewKind.PACKAGE
                        ? ProjectToolWindowContent::isPotentialPackage
                        : ProjectToolWindowContent::isPotentialJavaName);
        dialog.showAndWait().ifPresent(name -> create(request, name));
    }

    private static boolean isPotentialPackage(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (String component : value.split("\\.", -1)) {
            if (!isPotentialJavaName(component)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPotentialJavaName(String value) {
        if (value == null || value.isBlank() || !Character.isJavaIdentifierStart(value.charAt(0))) {
            return false;
        }
        for (int i = 1; i < value.length(); i++) {
            if (!Character.isJavaIdentifierPart(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void create(ExplorerNewRequest request, String name) {
        Path selected = request.node().isDirectory() ? request.node().path() : request.node().path().getParent();
        ProjectCreationService.CreationContext context = new ProjectCreationService.CreationContext(project, selected);
        try {
            ProjectCreationService.CreationResult result;
            if (request.kind() == ExplorerNewKind.PACKAGE) {
                result = creationService.createPackage(context, name);
            } else if (request.kind() == ExplorerNewKind.JAVA_FILE) {
                result = creationService.createJavaFile(context, name);
            } else {
                result = creationService.createJavaType(context, request.kind().javaTypeKind(), name);
            }
            if (explorer != null) {
                explorer.applyPathChange(result.path());
            }
            if (request.kind() != ExplorerNewKind.PACKAGE) {
                fileOpenHandler.accept(result.path());
            }
        } catch (IOException | IllegalArgumentException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Creation failed");
            alert.setHeaderText("Could not create " + name);
            alert.setContentText(exception.getMessage());
            alert.showAndWait();
        }
    }
}
