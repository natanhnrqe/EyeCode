package com.eyecode.javafx.ui;

import com.eyecode.designsystem.icon.EyeCodeIcon;
import com.eyecode.javafx.designsystem.FxSpacing;
import com.eyecode.javafx.designsystem.JavaFxIconButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.function.BooleanSupplier;

public final class FxToolbar extends HBox {

    private static final double ACTION_SPACING = 6;
    private static final double WINDOW_SPACING = 2;
    private static final double GROUP_GAP = 10;

    private final Label projectLabel;
    private final Button hamburger;

    private final Button runButton;
    private final Button rerunButton;
    private final Button stopButton;

    private BooleanSupplier running = () -> false;
    private BooleanSupplier rerunAvailable = () -> false;

    public FxToolbar() {
        this(null);
    }

    public FxToolbar(Runnable onClose) {
        getStyleClass().add("toolbar");

        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(
                0,
                FxSpacing.TOOLBAR_SIDE_PAD,
                0,
                FxSpacing.TOOLBAR_SIDE_PAD
        ));

        setPrefHeight(FxSpacing.TOOLBAR_HEIGHT);
        setMinHeight(FxSpacing.TOOLBAR_HEIGHT);
        setMaxHeight(FxSpacing.TOOLBAR_HEIGHT);

        /*
         * LEFT
         */
        HBox left = new HBox(8);
        left.getStyleClass().add("toolbar-left");
        left.setAlignment(Pos.CENTER_LEFT);

        this.hamburger =
                JavaFxIconButton.create(EyeCodeIcon.HAMBURGER, "Menu");

        Label logo = logoLabel();
        this.projectLabel = projectLabel();

        Button projectButton =
                JavaFxIconButton.create(EyeCodeIcon.PROJECT, "Project");

        HBox.setMargin(logo, new Insets(0, 10, 0, 6));
        HBox.setMargin(projectLabel, new Insets(0, 0, 0, 2));

        left.getChildren().addAll(
                hamburger,
                logo,
                projectButton,
                projectLabel
        );

        /*
         * EXECUTION
         *
         * Keep this isolated.
         * No fake Run Configuration button here.
         */
        HBox execution = new HBox();
        execution.getStyleClass().add("toolbar-execution");
        execution.setAlignment(Pos.CENTER);


        this.runButton =
                JavaFxIconButton.create(EyeCodeIcon.RUN, "Run");

        this.rerunButton =
                JavaFxIconButton.create(EyeCodeIcon.RELOAD, "Rerun");

        this.stopButton =
                JavaFxIconButton.create(EyeCodeIcon.STOP, "Stop");

        execution.getChildren().addAll(
                runButton,
                rerunButton,
                stopButton
        );

        /*
         * OTHER ACTIONS
         */
        HBox actions = new HBox(ACTION_SPACING);
        actions.getStyleClass().add("toolbar-actions");
        actions.setAlignment(Pos.CENTER);

        actions.getChildren().addAll(
                JavaFxIconButton.create(EyeCodeIcon.SEARCH, "Search"),
                JavaFxIconButton.create(EyeCodeIcon.GIT, "Git"),

                separator(),

                execution,

                JavaFxIconButton.create(EyeCodeIcon.DEBUG, "Debug"),

                separator(),

                JavaFxIconButton.create(EyeCodeIcon.SETTINGS, "Settings")
        );

        /*
         * WINDOW CONTROLS
         */
        HBox windowControls = new HBox(WINDOW_SPACING);
        windowControls.getStyleClass().add("toolbar-window");
        windowControls.setAlignment(Pos.CENTER_RIGHT);

        windowControls.getChildren().addAll(
                windowButton(
                        EyeCodeIcon.MINIMIZE,
                        "win-min",
                        "Minimize",
                        null
                ),
                windowButton(
                        EyeCodeIcon.MAXIMIZE,
                        "win-max",
                        "Maximize",
                        null
                ),
                windowButton(
                        EyeCodeIcon.CLOSE,
                        "win-close",
                        "Close",
                        onClose
                )
        );

        HBox right = new HBox(GROUP_GAP);
        right.getStyleClass().add("toolbar-right");
        right.setAlignment(Pos.CENTER_RIGHT);
        right.getChildren().addAll(
                actions,
                windowControls
        );

        /*
         * PUSH RIGHT SIDE TO THE EDGE
         */
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(
                left,
                spacer,
                right
        );

        refreshExecutionState();
    }

    public void setExecutionActions(
            Runnable run,
            Runnable rerun,
            Runnable stop,
            BooleanSupplier running,
            BooleanSupplier rerunAvailable
    ) {
        this.running =
                running != null ? running : () -> false;

        this.rerunAvailable =
                rerunAvailable != null ? rerunAvailable : () -> false;

        runButton.setOnAction(event -> {
            if (run != null) {
                run.run();
            }
        });

        rerunButton.setOnAction(event -> {
            if (rerun != null) {
                rerun.run();
            }
        });

        stopButton.setOnAction(event -> {
            if (stop != null) {
                stop.run();
            }
        });

        refreshExecutionState();
    }

    public void refreshExecutionState() {
        boolean active = running.getAsBoolean();
        boolean canRerun = rerunAvailable.getAsBoolean();

        runButton.setDisable(active);
        rerunButton.setDisable(!canRerun);
        stopButton.setDisable(!active);
    }

    java.util.List<Button> executionButtonsForTest() {
        return java.util.List.of(runButton, rerunButton, stopButton);
    }

    public void setProjectMenuActions(
            Runnable newProject,
            Runnable openProject,
            Runnable recentProjects
    ) {
        ContextMenu menu = new ContextMenu();

        MenuItem projectGroup = new MenuItem("Project / File");
        projectGroup.setDisable(true);

        menu.getItems().add(projectGroup);
        menu.getItems().addAll(
                menuItem("New Project", newProject),
                menuItem("Open Project", openProject),
                menuItem("Recent Projects", recentProjects)
        );

        hamburger.setOnAction(event ->
                menu.show(
                        hamburger,
                        Side.BOTTOM,
                        0,
                        4
                )
        );

        hamburger.setUserData(menu);
    }

    public void setProjectName(String name) {
        projectLabel.setText(
                name == null || name.isBlank()
                        ? "No project"
                        : name
        );
    }

    private Button windowButton(
            EyeCodeIcon icon,
            String id,
            String tooltip,
            Runnable action
    ) {
        Button button =
                JavaFxIconButton.windowButton(icon, id, tooltip);

        if (action != null) {
            button.setOnAction(event -> action.run());
        }

        return button;
    }

    private MenuItem menuItem(String text, Runnable action) {
        MenuItem item = new MenuItem(text);

        item.setOnAction(event -> {
            if (action != null) {
                action.run();
            }
        });

        return item;
    }

    ContextMenu projectMenuForTest() {
        return hamburger.getUserData() instanceof ContextMenu menu
                ? menu
                : null;
    }

    private Label logoLabel() {
        Label label = new Label("EyeCode");
        label.getStyleClass().add("toolbar-logo");
        return label;
    }

    private Label projectLabel() {
        Label label = new Label("No project");
        label.getStyleClass().add("toolbar-project");
        return label;
    }

    private Region separator() {
        Region separator = new Region();
        separator.getStyleClass().add("toolbar-separator");

        HBox.setMargin(
                separator,
                new Insets(0, 4, 0, 4)
        );

        return separator;
    }
}
