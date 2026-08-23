package com.eyecode.javafx.ui.toolwindow.content;

import com.eyecode.javafx.ui.toolwindow.ToolWindowContentFactory;
import com.eyecode.project.ProjectInfo;
import com.eyecode.project.ProjectLifecycleService;
import com.eyecode.project.model.ProjectModel;
import com.eyecode.javafx.explorer.ProjectNode;
import com.eyecode.runtime.RunService;
import javafx.scene.Node;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.file.Path;
import java.util.function.Consumer;

public final class WorkspaceContentFactory implements ToolWindowContentFactory {

    private static final Set<String> SUPPORTED = Set.of(
            "project", "search", "learn", "roadmap", "documentation", "run",
            "preview", "dependencies", "extensions", "settings", "profile",
            "terminal", "output", "problems", "git", "professor-ia"
    );

    private final Map<String, Node> cache = new HashMap<>();
    private final ProjectLifecycleService lifecycleService;
    private final ProjectModel initialProject;
    private final Consumer<Path> fileOpenHandler;
    private final Consumer<ProjectInfo> recentOpenHandler;
    private final Runnable openProjectHandler;
    private final Runnable newProjectHandler;
    private final RunService runService;
    private Consumer<ProjectNode> renameAction = node -> { };
    private Consumer<ProjectNode> deleteAction = node -> { };
    private ProjectModel project;

    public WorkspaceContentFactory() {
        this(new File("."));
    }

    public WorkspaceContentFactory(File projectRoot) {
        this(ProjectModel.fromDirectory(projectRoot), path -> { }, projectInfo -> { });
    }

    public WorkspaceContentFactory(ProjectLifecycleService lifecycleService,
                                   Consumer<Path> fileOpenHandler,
                                   Consumer<ProjectInfo> recentOpenHandler) {
        this(lifecycleService,
                lifecycleService == null ? null : lifecycleService.currentProject(),
                fileOpenHandler, recentOpenHandler, null, null);
    }

    public WorkspaceContentFactory(ProjectLifecycleService lifecycleService,
                                   ProjectModel initialProject,
                                   Consumer<Path> fileOpenHandler,
                                   Consumer<ProjectInfo> recentOpenHandler) {
        this(lifecycleService, initialProject, fileOpenHandler, recentOpenHandler, null, null);
    }

    public WorkspaceContentFactory(ProjectLifecycleService lifecycleService,
                                   ProjectModel initialProject,
                                   Consumer<Path> fileOpenHandler,
                                   Consumer<ProjectInfo> recentOpenHandler,
                                   Runnable openProjectHandler,
                                   Runnable newProjectHandler) {
        this(lifecycleService, initialProject, fileOpenHandler, recentOpenHandler,
                openProjectHandler, newProjectHandler, null);
    }

    public WorkspaceContentFactory(ProjectLifecycleService lifecycleService,
                                   ProjectModel initialProject,
                                   Consumer<Path> fileOpenHandler,
                                   Consumer<ProjectInfo> recentOpenHandler,
                                   Runnable openProjectHandler,
                                   Runnable newProjectHandler,
                                   RunService runService) {
        this.lifecycleService = lifecycleService;
        this.initialProject = initialProject;
        this.project = initialProject;
        this.fileOpenHandler = fileOpenHandler == null ? path -> { } : fileOpenHandler;
        this.recentOpenHandler = recentOpenHandler == null ? projectInfo -> { } : recentOpenHandler;
        this.openProjectHandler = openProjectHandler;
        this.newProjectHandler = newProjectHandler;
        this.runService = runService;
    }

    private WorkspaceContentFactory(ProjectModel initialProject,
                                    Consumer<Path> fileOpenHandler,
                                    Consumer<ProjectInfo> recentOpenHandler) {
        this.lifecycleService = null;
        this.initialProject = initialProject;
        this.project = initialProject;
        this.fileOpenHandler = fileOpenHandler == null ? path -> { } : fileOpenHandler;
        this.recentOpenHandler = recentOpenHandler == null ? projectInfo -> { } : recentOpenHandler;
        this.openProjectHandler = null;
        this.newProjectHandler = null;
        this.runService = null;
    }

    @Override
    public Node createContent(String toolWindowId) {
        return cache.computeIfAbsent(toolWindowId, this::build);
    }

    @Override
    public boolean supports(String toolWindowId) {
        return SUPPORTED.contains(toolWindowId);
    }

    public Node cached(String toolWindowId) {
        return cache.get(toolWindowId);
    }

    public void setProject(ProjectModel project) {
        this.project = project;
        Node node = cache.get("project");
        if (node instanceof ProjectToolWindowContent content) {
            content.setProject(project);
            if (lifecycleService != null) {
                content.setRecentProjects(lifecycleService.recentProjects(), recentOpenHandler);
            }
        }
    }

    public void setRecentProjects(List<ProjectInfo> projects) {
        Node node = cache.get("project");
        if (node instanceof ProjectToolWindowContent content) {
            content.setRecentProjects(projects, recentOpenHandler);
        }
    }

    public void refreshProjectTree() {
        Node node = cache.get("project");
        if (node instanceof ProjectToolWindowContent content && project != null) {
            content.refresh(project);
        }
    }

    public void setFileOperationHandlers(Consumer<ProjectNode> renameAction,
                                         Consumer<ProjectNode> deleteAction) {
        this.renameAction = renameAction == null ? node -> { } : renameAction;
        this.deleteAction = deleteAction == null ? node -> { } : deleteAction;
        Node node = cache.get("project");
        if (node instanceof ProjectToolWindowContent content) {
            content.setFileOperationHandlers(this.renameAction, this.deleteAction);
        }
    }

    public void dispose() {
        Node preview = cache.get("preview");
        if (preview instanceof PreviewToolWindowContent content) {
            content.dispose();
        }
    }

    private Node build(String id) {
        return switch (id) {
            case "project" -> {
                ProjectToolWindowContent content = new ProjectToolWindowContent(
                        project, fileOpenHandler, openProjectHandler, newProjectHandler);
                content.setFileOperationHandlers(renameAction, deleteAction);
                if (lifecycleService != null) {
                    content.setRecentProjects(lifecycleService.recentProjects(), recentOpenHandler);
                }
                yield content;
            }
            case "run" -> new RunToolWindowContent(runService);
            case "learn" -> new LearnToolWindowContent();
            case "roadmap" -> new RoadmapToolWindowContent();
            case "documentation" -> new DocumentationToolWindowContent();
            case "search" -> new SearchToolWindowContent();
            case "preview" -> new PreviewToolWindowContent();
            case "dependencies" -> new DependenciesToolWindowContent();
            case "settings" -> new SettingsToolWindowContent();
            default -> GenericToolWindowContent.forId(id);
        };
    }
}
