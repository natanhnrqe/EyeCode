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
        if (root == null || !java.nio.file.Files.isDirectory(root)) {
            throw new IllegalArgumentException("Project root must be an existing directory");
        }
        Path normalizedRoot = root.toAbsolutePath().normalize();
        if (com.eyecode.project.ProjectDetector.detect(normalizedRoot.toFile())
                == com.eyecode.project.ProjectType.UNKNOWN && isEmptyDirectory(normalizedRoot)) {
            throw new IllegalArgumentException("Selected directory does not contain a project");
        }
        execution.stop();
        ProjectModel project = lifecycle.open(normalizedRoot);
        lifecycle.recordRecent(project);
        editor.closeAllSessions();
        editor.watchProject(project.getRootDir());
        execution.refreshConfigurations();
        return project;
    }

    private boolean isEmptyDirectory(Path root) {
        try (var entries = java.nio.file.Files.list(root)) {
            return entries.findAny().isEmpty();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read project directory", exception);
        }
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

    public void removeRecent(Path root) {
        lifecycle.removeRecent(root);
    }

    public Optional<String> selectedMainClass(ProjectModel project) {
        var selected = execution.selectedConfiguration();
        if (selected == null || !selected.projectRoot().equals(project.getRootDir().toAbsolutePath().normalize())) {
            return Optional.empty();
        }
        return Optional.of(selected.mainClass());
    }
}
