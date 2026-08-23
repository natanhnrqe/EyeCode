package com.eyecode.project;

import com.eyecode.project.model.ProjectModel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ProjectLifecycleService {

    public interface Listener {
        void onProjectChanged(ProjectModel project);
    }

    private final ProjectService projectService;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private ProjectModel currentProject;

    public ProjectLifecycleService() {
        this(new ProjectService());
    }

    public ProjectLifecycleService(ProjectService projectService) {
        if (projectService == null) {
            throw new IllegalArgumentException("projectService must not be null");
        }
        this.projectService = projectService;
    }

    public ProjectModel open(Path directory) {
        Path root = normalizeDirectory(directory);
        ProjectModel project = ProjectModel.fromDirectory(root.toFile());
        currentProject = project;
        projectService.addRecent(project.toInfo());
        notifyListeners(project);
        return project;
    }

    public ProjectModel openRecent(ProjectInfo info) {
        if (info == null) {
            throw new IllegalArgumentException("Recent project must not be null");
        }
        return open(Path.of(info.getPath()));
    }

    public void close() {
        if (currentProject == null) {
            return;
        }
        currentProject = null;
        notifyListeners(null);
    }

    public ProjectModel currentProject() {
        return currentProject;
    }

    public List<ProjectInfo> recentProjects() {
        return projectService.getRecentProjects();
    }

    public void removeRecent(Path directory) {
        if (directory != null) {
            projectService.removeRecent(directory.toString());
        }
    }

    public void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private Path normalizeDirectory(Path directory) {
        if (directory == null) {
            throw new IllegalArgumentException("Project directory must not be null");
        }
        Path root = directory.toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Project directory does not exist: " + root);
        }
        return root;
    }

    private void notifyListeners(ProjectModel project) {
        for (Listener listener : listeners) {
            listener.onProjectChanged(project);
        }
    }
}
