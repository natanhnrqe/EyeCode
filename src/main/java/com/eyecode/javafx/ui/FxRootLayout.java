package com.eyecode.javafx.ui;

import com.eyecode.javafx.designsystem.FxCanvas;
import com.eyecode.javafx.designsystem.FxSpacing;
import com.eyecode.javafx.ui.toolwindow.FxBottomToolWindow;
import com.eyecode.javafx.ui.toolwindow.FxLeftToolWindow;
import com.eyecode.javafx.ui.toolwindow.FxWorkspaceNavigator;
import com.eyecode.javafx.ui.toolwindow.ToolWindowContentFactory;
import com.eyecode.javafx.ui.toolwindow.content.WorkspaceContentFactory;
import com.eyecode.workbench.toolwindow.ToolWindowManager;
import com.eyecode.workbench.toolwindow.ToolWindowPosition;
import com.eyecode.workbench.toolwindow.WorkspaceNavigatorItem;
import com.eyecode.workbench.toolwindow.WorkspaceNavigatorModel;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.List;

public final class FxRootLayout extends BorderPane {

    private ToolWindowManager toolWindowManager;
    private FxLeftToolWindow leftToolWindow;
    private FxBottomToolWindow bottomToolWindow;
    private HBox bottomBar;

    public FxRootLayout(Runnable onWindowClose) {
        getStyleClass().add("root-layout");

        FxToolbar toolbar = onWindowClose != null ? new FxToolbar(onWindowClose) : new FxToolbar();
        Region workspace = buildWorkspace();

        setTop(toolbar);
        setCenter(workspace);
        setBottom(bottomBar);
    }

    private Region buildWorkspace() {
        ToolWindowManager manager = new ToolWindowManager();
        this.toolWindowManager = manager;
        registerDefaultToolWindows(manager);

        WorkspaceNavigatorModel navigatorModel = new WorkspaceNavigatorModel();
        navigatorModel.setItems(defaultNavigatorItems());

        ToolWindowContentFactory contentFactory = new WorkspaceContentFactory();

        FxWorkspaceNavigator navigator = new FxWorkspaceNavigator(navigatorModel, manager);
        FxLeftToolWindow leftToolWindow = new FxLeftToolWindow(manager, contentFactory);
        this.leftToolWindow = leftToolWindow;
        FxEditorContainer editorContainer = new FxEditorContainer();
        FxBottomToolWindow bottomToolWindow = new FxBottomToolWindow(manager, contentFactory);
        this.bottomToolWindow = bottomToolWindow;
        FxBottomToolWindowBar bottomBar = new FxBottomToolWindowBar(manager);

        manager.activate("project");
        manager.activate("terminal");

        SplitPane centerSplit = new SplitPane(leftToolWindow, editorContainer);
        centerSplit.getStyleClass().add("center-split");
        centerSplit.setDividerPositions(0.20);

        SplitPane rootSplit = new SplitPane(centerSplit, bottomToolWindow);
        rootSplit.getStyleClass().add("root-split");
        rootSplit.setOrientation(Orientation.VERTICAL);
        rootSplit.setDividerPositions(0.72);

        HBox bottomWrap = new HBox(bottomBar);
        bottomWrap.getStyleClass().add("bottom-tool-window-wrap");
        HBox.setHgrow(bottomBar, Priority.ALWAYS);
        bottomBar.setMaxWidth(Double.MAX_VALUE);
        this.bottomBar = bottomWrap;

        BorderPane workspace = new BorderPane();
        workspace.getStyleClass().add("workspace");
        workspace.setCenter(rootSplit);
        workspace.setLeft(navigator);

        FxCanvas canvas = new FxCanvas(workspace);
        canvas.setPadding(new Insets(
                FxSpacing.CANVAS_PADDING,
                FxSpacing.CANVAS_PADDING,
                FxSpacing.CANVAS_PADDING,
                FxSpacing.CANVAS_PADDING));
        return canvas;
    }

    public ToolWindowManager getToolWindowManager() {
        return toolWindowManager;
    }

    public FxLeftToolWindow getLeftToolWindow() {
        return leftToolWindow;
    }

    public FxBottomToolWindow getBottomToolWindow() {
        return bottomToolWindow;
    }

    private void registerDefaultToolWindows(ToolWindowManager manager) {
        manager.register("project",       "Project",       "PROJECT",           ToolWindowPosition.LEFT);
        manager.register("search",        "Search",        "SEARCH",            ToolWindowPosition.LEFT);
        manager.register("learn",         "Learn",         "FOLDERS",           ToolWindowPosition.LEFT);
        manager.register("roadmap",       "Roadmap",       "STRUCTURE",         ToolWindowPosition.LEFT);
        manager.register("documentation", "Documentation", "TEXT_FILE",         ToolWindowPosition.LEFT);
        manager.register("preview",       "Preview",       "PLAY",              ToolWindowPosition.LEFT);
        manager.register("dependencies",  "Dependencies",  "ASSETS",            ToolWindowPosition.LEFT);
        manager.register("extensions",    "Extensions",    "SERVICES",          ToolWindowPosition.LEFT);
        manager.register("settings",      "Settings",      "SETTINGS",          ToolWindowPosition.LEFT);
        manager.register("profile",       "Profile",       "PROJECT_DIRECTORY", ToolWindowPosition.LEFT);

        manager.register("terminal",     "Terminal",     "TERMINAL", ToolWindowPosition.BOTTOM);
        manager.register("output",       "Output",       "CLEAR",    ToolWindowPosition.BOTTOM);
        manager.register("problems",     "Problems",     "PROBLEM",  ToolWindowPosition.BOTTOM);
        manager.register("git",          "Git",          "GIT",      ToolWindowPosition.BOTTOM);
        manager.register("professor-ia", "Professor IA", "SERVICES", ToolWindowPosition.BOTTOM);
    }

    private List<WorkspaceNavigatorItem> defaultNavigatorItems() {
        return List.of(
                item("project",       "Project",       "PROJECT"),
                item("search",        "Search",        "SEARCH"),
                item("learn",         "Learn",         "FOLDERS"),
                item("roadmap",       "Roadmap",       "STRUCTURE"),
                item("documentation", "Documentation", "TEXT_FILE"),
                item("preview",       "Preview",       "PLAY"),
                item("dependencies",  "Dependencies",  "ASSETS"),
                item("extensions",    "Extensions",    "SERVICES"),
                item("settings",      "Settings",      "SETTINGS"),
                item("profile",       "Profile",       "PROJECT_DIRECTORY")
        );
    }

    private WorkspaceNavigatorItem item(String id, String title, String iconKey) {
        return new WorkspaceNavigatorItem(id, iconKey, title, title, id);
    }
}