package com.eyecode.application;

import com.eyecode.project.MavenProjectCreationService;
import com.eyecode.project.ProjectInfo;
import com.eyecode.project.ProjectLifecycleService;
import com.eyecode.project.model.ProjectModel;
import com.eyecode.runtime.RunService;
import com.eyecode.workbench.editor.EditorManager;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class WorkspaceProjects {
    private final ProjectLifecycleService lifecycle;
    private final EditorManager editor;
    private final RunService execution;
    private final MavenProjectCreationService creation;

    public WorkspaceProjects(ProjectLifecycleService lifecycle, EditorManager editor,
                             RunService execution, MavenProjectCreationService creation) {
        this.lifecycle = Objects.requireNonNull(lifecycle);
        this.editor = Objects.requireNonNull(editor);
        this.execution = Objects.requireNonNull(execution);
        this.creation = Objects.requireNonNull(creation);
    }

    public ProjectModel open(Path root) {
        execution.stop();
        ProjectModel project = lifecycle.open(root);
        lifecycle.recordRecent(project);
        editor.closeAllSessions();
        editor.watchProject(project.getRootDir());
        execution.refreshConfigurations();
        return project;
    }

    public ProjectModel create(MavenProjectCreationService.CreationRequest request) throws IOException {
        return open(creation.create(request).projectRoot());
    }

    public ProjectModel current() {
        return lifecycle.currentProject();
    }

    public ProjectModel requireCurrent() {
        ProjectModel project = current();
        if (project == null) throw new IllegalArgumentException("No project is open");
        return project;
    }

    public List<ProjectInfo> recent() {
        return lifecycle.recentProjects();
    }

    public Optional<String> selectedMainClass(ProjectModel project) {
        var selected = execution.selectedConfiguration();
        if (selected == null || !selected.projectRoot().equals(project.getRootDir().toAbsolutePath().normalize())) {
            return Optional.empty();
        }
        return Optional.of(selected.mainClass());
    }
}
