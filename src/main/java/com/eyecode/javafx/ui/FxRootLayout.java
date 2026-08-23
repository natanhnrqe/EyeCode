package com.eyecode.javafx.ui;

import com.eyecode.javafx.designsystem.FxCanvas;
import com.eyecode.javafx.designsystem.FxSpacing;
import com.eyecode.javafx.ui.toolwindow.FxBottomToolWindow;
import com.eyecode.javafx.ui.toolwindow.FxLeftToolWindow;
import com.eyecode.javafx.ui.toolwindow.FxWorkspaceNavigator;
import com.eyecode.javafx.ui.toolwindow.ToolWindowContentFactory;
import com.eyecode.javafx.ui.toolwindow.content.WorkspaceContentFactory;
import com.eyecode.project.ProjectInfo;
import com.eyecode.project.ProjectLifecycleService;
import com.eyecode.project.model.ProjectModel;
import com.eyecode.runtime.RunService;
import com.eyecode.workbench.toolwindow.ToolWindowManager;
import com.eyecode.workbench.toolwindow.ToolWindowPosition;
import com.eyecode.workbench.toolwindow.WorkspaceNavigatorItem;
import com.eyecode.workbench.toolwindow.WorkspaceNavigatorModel;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.application.Platform;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public final class FxRootLayout extends BorderPane {

    private ToolWindowManager toolWindowManager;
    private FxLeftToolWindow leftToolWindow;
    private FxBottomToolWindow bottomToolWindow;
    private WorkspaceContentFactory contentFactory;
    private FxEditorContainer editorContainer;
    private HBox bottomBar;
    private final ProjectLifecycleService projectLifecycleService;
    private RunService runService;
    private FxToolbar toolbar;

    public FxRootLayout(Runnable onWindowClose) {
        this(onWindowClose, new ProjectLifecycleService(), ProjectModel.fromDirectory(new File(".")));
    }

    public FxRootLayout(Runnable onWindowClose, ProjectLifecycleService projectLifecycleService) {
        this(onWindowClose, projectLifecycleService, null);
    }

    private FxRootLayout(Runnable onWindowClose,
                         ProjectLifecycleService projectLifecycleService,
                         ProjectModel initialProject) {
        getStyleClass().add("root-layout");
        this.projectLifecycleService = projectLifecycleService == null
                ? new ProjectLifecycleService() : projectLifecycleService;

        this.toolbar = new FxToolbar(onWindowClose);
        toolbar.setProjectMenuActions(
                this::showNewProjectSurface,
                this::openProjectDialog,
                this::showRecentProjects);
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

        ProjectModel initialProject = projectLifecycleService.currentProject() != null
                ? projectLifecycleService.currentProject()
                : ProjectModel.fromDirectory(new File("."));
        this.runService = new RunService(projectLifecycleService);
        WorkspaceContentFactory contentFactory = new WorkspaceContentFactory(
                projectLifecycleService,
                initialProject,
                this::openFile,
                this::openRecentProject,
                this::openProjectDialog,
                this::showNewProjectSurface,
                runService);
        this.contentFactory = contentFactory;

        FxWorkspaceNavigator navigator = new FxWorkspaceNavigator(navigatorModel, manager);
        FxLeftToolWindow leftToolWindow = new FxLeftToolWindow(manager, contentFactory);
        this.leftToolWindow = leftToolWindow;
        FxEditorContainer editorContainer = new FxEditorContainer(
                this::showNewProjectSurface,
                this::openProjectDialog,
                projectLifecycleService::recentProjects,
                this::openRecentProject);
        this.editorContainer = editorContainer;
        runService.setBeforeRunFlush(editorContainer::flushAutosave);
        toolbar.setExecutionActions(
                () -> {
                    runService.runCurrent();
                    manager.activate("run");
                },
                () -> {
                    runService.rerun();
                    manager.activate("run");
                },
                runService::stop,
                runService::isRunning,
                runService::hasLastRequest);
        toolbar.setRunnable(() -> runService.selectedConfiguration() != null);
        refreshRunConfigurations();
        runService.addListener(new RunService.Listener() {
            @Override public void onStarted(com.eyecode.runtime.RunRequest request) { refreshRunControls(); }
            @Override public void onOutput(String line, boolean error) { refreshRunControls(); }
            @Override public void onFinished(int exitCode, boolean stopped) { refreshRunControls(); }
        });
        projectLifecycleService.addListener(project -> refreshRunConfigurations());
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

    public ProjectLifecycleService getProjectLifecycleService() {
        return projectLifecycleService;
    }

    public void openProject(Path directory) {
        if (directory == null) {
            return;
        }
        if (editorContainer.hasDirtySessions() && !confirmDiscardChanges()) {
            return;
        }
        if (runService != null) {
            if (editorContainer != null && !editorContainer.flushAutosave()) {
                showError("Save", "Could not save pending editor changes.");
                return;
            }
            runService.stop();
        }
        try {
            ProjectModel project = projectLifecycleService.open(directory);
            projectLifecycleService.recordRecent(project);
            contentFactory.setProject(project);
            contentFactory.setRecentProjects(projectLifecycleService.recentProjects());
            editorContainer.openProject(project);
            toolbar.setProjectName(project.getName());
            editorContainer.setProjectName(project.getName());
        } catch (IllegalArgumentException exception) {
            showError("Open Project", exception.getMessage());
        }
    }

    public void dispose() {
        if (editorContainer != null) {
            editorContainer.flushAutosave();
        }
        if (runService != null) {
            runService.dispose();
        }
        if (contentFactory != null) {
            contentFactory.dispose();
        }
        if (editorContainer != null) {
            editorContainer.dispose();
        }
        projectLifecycleService.close();
    }

    private void refreshRunControls() {
        if (Platform.isFxApplicationThread()) {
            toolbar.refreshExecutionState();
        } else {
            Platform.runLater(toolbar::refreshExecutionState);
        }
    }

    private void refreshRunConfigurations() {
        Runnable refresh = () -> toolbar.setRunConfigurations(
                runService.configurations(),
                runService.selectedConfiguration(),
                id -> {
                    runService.selectConfiguration(id);
                    refreshRunConfigurations();
                });
        if (Platform.isFxApplicationThread()) {
            refresh.run();
        } else {
            Platform.runLater(refresh);
        }
    }

    private void openProjectDialog() {
        Window owner = getScene() == null ? null : getScene().getWindow();
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open Project");
        File selected = chooser.showDialog(owner);
        if (selected != null) {
            openProject(selected.toPath());
        }
    }

    private void openRecentProject(ProjectInfo project) {
        if (project == null) {
            return;
        }
        Path path = Path.of(project.getPath());
        if (!java.nio.file.Files.isDirectory(path)) {
            projectLifecycleService.removeRecent(path);
            contentFactory.setRecentProjects(projectLifecycleService.recentProjects());
            showError("Recent Project", "Project directory no longer exists: " + path);
            return;
        }
        openProject(path);
    }

    private void openFile(Path file) {
        if (editorContainer != null) {
            editorContainer.openFile(file);
        }
    }

    private void showNewProjectSurface() {
        editorContainer.showNewProjectSurface();
    }

    private void showRecentProjects() {
        editorContainer.refreshWelcomeProjects();
        editorContainer.showWelcomeSurface();
    }

    private boolean confirmDiscardChanges() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "There are unsaved editor changes. Close them and switch project?",
                ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle("Switch Project");
        alert.setHeaderText("Unsaved Changes");
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message == null ? "Unable to open project." : message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.showAndWait();
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

        manager.register("run",          "Run",          "RUN",      ToolWindowPosition.BOTTOM);
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
