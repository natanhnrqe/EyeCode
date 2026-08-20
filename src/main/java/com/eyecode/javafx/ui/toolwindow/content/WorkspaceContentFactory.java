package com.eyecode.javafx.ui.toolwindow.content;

import com.eyecode.javafx.ui.toolwindow.ToolWindowContentFactory;
import com.eyecode.project.model.ProjectModel;
import javafx.scene.Node;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class WorkspaceContentFactory implements ToolWindowContentFactory {

    private static final Set<String> SUPPORTED = Set.of(
            "project", "search", "learn", "roadmap", "documentation",
            "preview", "dependencies", "extensions", "settings", "profile",
            "terminal", "output", "problems", "git", "professor-ia"
    );

    private final Map<String, Node> cache = new HashMap<>();
    private final File projectRoot;

    public WorkspaceContentFactory() {
        this(new File("."));
    }

    public WorkspaceContentFactory(File projectRoot) {
        this.projectRoot = projectRoot;
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

    public void dispose() {
        Node preview = cache.get("preview");
        if (preview instanceof PreviewToolWindowContent content) {
            content.dispose();
        }
        Node learning = cache.get("learn");
        if (learning instanceof LearnToolWindowContent content) {
            content.dispose();
        }
    }

    private Node build(String id) {
        return switch (id) {
            case "project" -> new ProjectToolWindowContent(ProjectModel.fromDirectory(projectRoot));
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
