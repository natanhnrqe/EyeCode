package com.eyecode.javafx.ui.editor;

import com.eyecode.project.ProjectInfo;
import com.eyecode.project.ProjectType;
import com.eyecode.designsystem.icon.EyeCodeIcon;
import com.eyecode.javafx.designsystem.JavaFxIconManager;
import com.eyecode.javafx.designsystem.JavaFxButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class WelcomeProjectSurface extends VBox {

    private final VBox recentProjects = new VBox(4);
    private final ScrollPane recentViewport = new ScrollPane(recentProjects);
    private final Label projectState = new Label("No editor is currently open");
    private final Supplier<List<ProjectInfo>> recentSupplier;
    private final Consumer<ProjectInfo> recentAction;

    public WelcomeProjectSurface(Runnable newProjectAction,
                                 Runnable openProjectAction,
                                 Supplier<List<ProjectInfo>> recentSupplier,
                                 Consumer<ProjectInfo> recentAction) {
        this.recentSupplier = recentSupplier == null ? List::of : recentSupplier;
        this.recentAction = recentAction;
        getStyleClass().add("welcome-project-surface");
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(44, 48, 44, 48));
        setSpacing(14);
        setFillWidth(true);

        Label logo = new Label("EyeCode IDE");
        logo.getStyleClass().add("welcome-logo");
        projectState.getStyleClass().add("welcome-subtitle");
        Label tagline = new Label("Learn Programming. Learn Architecture. Build Real Projects.");
        tagline.getStyleClass().add("welcome-secondary");
        Label start = new Label("Start");
        start.getStyleClass().add("welcome-section-title");

        Button openProject = actionButton("Open Project", openProjectAction);
        Button newProject = actionButton("New Project", newProjectAction);
        Button cloneProject = actionButton("Clone Repository", null);
        cloneProject.setDisable(true);
        HBox actions = new HBox(10, openProject, newProject, cloneProject);
        actions.setAlignment(Pos.CENTER);

        Label recentTitle = new Label("Recent Projects");
        recentTitle.getStyleClass().add("welcome-section-title");
        recentProjects.setPadding(new Insets(8));
        recentProjects.setMaxWidth(Double.MAX_VALUE);
        recentViewport.setFitToWidth(true);
        recentViewport.setFitToHeight(false);
        recentViewport.setPrefViewportHeight(320);
        recentViewport.setMinViewportHeight(160);
        recentViewport.setMaxHeight(340);
        recentViewport.setPrefWidth(620);
        recentViewport.getStyleClass().add("welcome-recent-viewport");
        VBox.setVgrow(recentViewport, Priority.NEVER);
        VBox.setVgrow(recentProjects, Priority.NEVER);
        getChildren().addAll(logo, tagline, projectState, start, actions, recentTitle, recentViewport,
                new Label("Ctrl+B  Go to Definition    Ctrl+Q  Documentation"));
        refreshRecentProjects();
    }

    public void setProjectName(String projectName) {
        projectState.setText(projectName == null || projectName.isBlank()
                ? "No editor is currently open"
                : "No editor is currently open in " + projectName);
    }

    public void refreshRecentProjects() {
        recentProjects.getChildren().clear();
        List<ProjectInfo> projects = recentSupplier.get();
        if (projects == null || projects.isEmpty()) {
            Label empty = new Label("No recent projects");
            empty.getStyleClass().add("welcome-secondary");
            recentProjects.getChildren().add(empty);
            return;
        }
        for (ProjectInfo project : projects) {
            Button item = new Button();
            item.getStyleClass().add("welcome-recent-item");
            item.setMaxWidth(Double.MAX_VALUE);
            item.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
            Label icon = new Label();
            icon.setGraphic(JavaFxIconManager.icon(iconFor(project.getType()), 20));
            VBox text = new VBox(2,
                    new Label(project.getName()),
                    new Label(project.getPath()));
            text.getStyleClass().add("welcome-recent-item-text");
            Label type = new Label(project.getType().getDisplayName());
            type.getStyleClass().add("welcome-recent-item-kind");
            HBox row = new HBox(10, icon, text, type);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(text, Priority.ALWAYS);
            item.setGraphic(row);
            item.setTooltip(new Tooltip(project.getPath()));
            item.setUserData(project);
            item.setOnAction(event -> {
                if (recentAction != null) {
                    recentAction.accept(project);
                }
            });
            recentProjects.getChildren().add(item);
        }
    }

    VBox recentProjectsForTest() {
        return recentProjects;
    }

    ScrollPane recentViewportForTest() {
        return recentViewport;
    }

    private EyeCodeIcon iconFor(ProjectType type) {
        if (type == null) {
            return EyeCodeIcon.PROJECT;
        }
        return switch (type) {
            case SPRING_BOOT -> EyeCodeIcon.PROJECT;
            case MAVEN -> EyeCodeIcon.PROJECT;
            case GRADLE -> EyeCodeIcon.PROJECT;
            case GIT -> EyeCodeIcon.GIT;
            case JAVA -> EyeCodeIcon.JAVA_FILE;
            case UNKNOWN -> EyeCodeIcon.PROJECT_DIRECTORY;
        };
    }

    private Button actionButton(String text, Runnable action) {
        Button button = JavaFxButton.create(text);
        button.setOnAction(event -> {
            if (action != null) {
                action.run();
            }
        });
        return button;
    }
}
