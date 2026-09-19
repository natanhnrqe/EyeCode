package com.eyecode.ui.web;

import com.eyecode.application.ProjectExplorerQuery;
import com.eyecode.application.WorkspaceProjects;
import com.eyecode.autosave.ExternalFileEvent;
import com.eyecode.project.ProjectFileOperationService;
import com.eyecode.project.MavenProjectCreationService;
import com.eyecode.project.model.ProjectModel;
import com.eyecode.ui.web.monaco.MonacoModelId;
import com.eyecode.workbench.editor.EditorManager;
import com.eyecode.workbench.editor.EditorSession;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import static com.eyecode.ui.web.WebShellPayload.*;

public final class WebShellWorkspaceController {
    private final WebShellSurface surface;
    private final WebShellNativeFileSelection nativeUi;
    private final EditorManager manager;
    private final WorkspaceProjects projects;
    private final ProjectExplorerQuery explorer;
    private final ProjectFileOperationService fileOperations;
    private final WebShellDocumentController documents;
    private final WebShellExecutionController executionController;
    private final Consumer<ExternalFileEvent> externalFileListener;
    private boolean disposed;

    WebShellWorkspaceController(WebShellSurface surface, WebShellNativeFileSelection nativeUi,
                                EditorManager manager, WorkspaceProjects projects, ProjectExplorerQuery explorer,
                                ProjectFileOperationService fileOperations, WebShellDocumentController documents,
                                WebShellExecutionController executionController) {
        this.surface = Objects.requireNonNull(surface);
        this.nativeUi = Objects.requireNonNull(nativeUi);
        this.manager = Objects.requireNonNull(manager);
        this.projects = Objects.requireNonNull(projects);
        this.explorer = Objects.requireNonNull(explorer);
        this.fileOperations = Objects.requireNonNull(fileOperations);
        this.documents = Objects.requireNonNull(documents);
        this.executionController = Objects.requireNonNull(executionController);
        this.externalFileListener = event -> {
            if (!disposed && event != null) sendTreeChanged(event.path());
        };
        manager.addExternalFileListener(externalFileListener);
        surface.registerHandler("workspace", "snapshot", this::workspaceSnapshot);
        surface.registerHandler("workspace", "openProject", this::openProject);
        surface.registerHandler("workspace", "createProject", this::createProject);
        surface.registerHandler("workspace", "chooseDirectory", this::chooseDirectory);
        surface.registerHandler("workspace", "refresh", this::refreshWorkspace);
        surface.registerHandler("workspace", "children", this::workspaceChildren);
        surface.registerHandler("workspace", "openFile", this::openWorkspaceFile);
        surface.registerHandler("workspace", "createFile", this::createFile);
        surface.registerHandler("workspace", "createDirectory", this::createDirectory);
        surface.registerHandler("workspace", "createJavaClass", this::createJavaClass);
        surface.registerHandler("workspace", "createPackage", this::createPackage);
        surface.registerHandler("workspace", "rename", this::renamePath);
        surface.registerHandler("workspace", "delete", this::deletePath);
        surface.registerHandler("workspace", "duplicate", this::duplicatePath);
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        manager.removeExternalFileListener(externalFileListener);
    }

    private WebShellEnvelope workspaceSnapshot(WebShellEnvelope message) {
        return message.response(workspacePayload());
    }

    private WebShellEnvelope openProject(WebShellEnvelope message) {
        String rawPath = text(message.payload(), "path");
        if (rawPath.isBlank() && nativeUi.isAvailable()) {
            nativeUi.chooseDirectoryAsync("Open Project")
                    .thenApplyAsync(root -> openProjectResult(message, root))
                    .exceptionally(exception -> message.error(new WebShellError("NATIVE_UI_FAILED", exception.getMessage(), true)))
                    .thenAccept(surface::send);
            return null;
        }
        if (rawPath.isBlank()) {
            return nativeUiUnavailable(message, "Project directory selection is unavailable in the web runtime");
        }
        return openProjectResult(message, Path.of(rawPath));
    }

    private WebShellEnvelope openProjectResult(WebShellEnvelope message, Path root) {
        if (root == null) return message.response(Map.of("cancelled", true));
        try {
            return message.response(openWorkspace(root));
        } catch (IllegalArgumentException exception) {
            return message.error(new WebShellError("INVALID_PROJECT", exception.getMessage(), true));
        }
    }

    private WebShellEnvelope createProject(WebShellEnvelope message) {
        try {
            ProjectModel created = projects.create(
                    new MavenProjectCreationService.CreationRequest(
                            text(message.payload(), "name"),
                            text(message.payload(), "location"),
                            text(message.payload(), "groupId")));
            return message.response(workspaceOpened(created));
        } catch (IllegalArgumentException exception) {
            return message.error(new WebShellError("INVALID_PROJECT", exception.getMessage(), true));
        } catch (IOException exception) {
            return message.error(new WebShellError("PROJECT_CREATION_FAILED", safeMessage(exception), true));
        }
    }

    private WebShellEnvelope chooseDirectory(WebShellEnvelope message) {
        if (nativeUi.isAvailable()) {
            nativeUi.chooseDirectoryAsync("Choose Project Location")
                    .thenApplyAsync(directory -> chooseDirectoryResult(message, directory))
                    .exceptionally(exception -> message.error(new WebShellError("NATIVE_UI_FAILED", exception.getMessage(), true)))
                    .thenAccept(surface::send);
            return null;
        }
        return nativeUiUnavailable(message, "Directory selection is unavailable in the web runtime");
    }

    private WebShellEnvelope chooseDirectoryResult(WebShellEnvelope message, Path directory) {
        return message.response(directory == null ? Map.of("cancelled", true) : Map.of("path", directory.toString()));
    }

    private Map<String, Object> openWorkspace(Path root) {
        return workspaceOpened(projects.open(root));
    }

    private Map<String, Object> workspaceOpened(ProjectModel project) {
        documents.reset();
        surface.send(WebShellEnvelope.event("workspace", "reset", Map.of()));
        Map<String, Object> payload = workspacePayload();
        explorer.preferredEntryPoint(project, projects.selectedMainClass(project))
                .ifPresent(path -> payload.put("reveal", revealPayload(project, path)));
        surface.send(WebShellEnvelope.event("workspace", "changed", payload));
        executionController.publishWorkspaceState();
        return payload;
    }

    private WebShellEnvelope workspaceChildren(WebShellEnvelope message) {
        ProjectModel project = projects.current();
        if (project == null) return message.response(Map.of("children", List.of()));
        String rawPath = text(message.payload(), "path");
        Path directory = rawPath.isBlank() ? project.getRootDir() : Path.of(rawPath);
        try {
            directory = explorer.requireDirectory(project, directory);
            return message.response(Map.of("parent", directory.toString(), "children",
                    explorer.children(project, directory).stream().map(entry -> treePayload(entry, false)).toList()));
        } catch (IllegalArgumentException exception) {
            return message.error(new WebShellError("INVALID_TREE_PATH", safeMessage(exception), true));
        }
    }

    private WebShellEnvelope refreshWorkspace(WebShellEnvelope message) {
        ProjectModel project = projects.current();
        if (project == null) return message.response(workspacePayload());
        List<String> validPaths = explorer.validExpandedDirectories(project,
                paths(message.payload(), "paths").stream().map(Path::of).toList())
                .stream().map(Path::toString).toList();
        Map<String, Object> payload = workspacePayload();
        payload.put("validPaths", validPaths);
        return message.response(payload);
    }

    private WebShellEnvelope openWorkspaceFile(WebShellEnvelope message) {
        String rawPath = text(message.payload(), "path");
        if (rawPath.isBlank()) return message.error(new WebShellError(
                "INVALID_DOCUMENT", "A project file path is required", true));
        try {
            Path path = Path.of(rawPath).toAbsolutePath().normalize();
            ProjectModel project = projects.current();
            if (!explorer.isProjectFile(project, path)) {
                return message.error(new WebShellError("DOCUMENT_NOT_FOUND", path.toString(), true));
            }
            EditorSession session = documents.openPath(path);
            return message.response(Map.of("document", documents.snapshot(session).payload()));
        } catch (RuntimeException exception) {
            return message.error(new WebShellError("INVALID_DOCUMENT", exception.getMessage(), true));
        }
    }

    private WebShellEnvelope createFile(WebShellEnvelope message) {
        return createInDirectory(message, "CREATE_FILE", fileOperations::createFile, true);
    }

    private WebShellEnvelope createDirectory(WebShellEnvelope message) {
        return createInDirectory(message, "CREATE_DIRECTORY", fileOperations::createDirectory, false);
    }

    private WebShellEnvelope createJavaClass(WebShellEnvelope message) {
        return createInDirectory(message, "CREATE_JAVA_CLASS", fileOperations::createJavaClass, true);
    }

    private WebShellEnvelope createPackage(WebShellEnvelope message) {
        return createInDirectory(message, "CREATE_PACKAGE", fileOperations::createPackage, false);
    }

    private WebShellEnvelope createInDirectory(WebShellEnvelope message, String errorCode,
                                                DirectoryMutation mutation, boolean openFile) {
        try {
            ProjectModel project = projects.requireCurrent();
            Path directory = Path.of(text(message.payload(), "target")).toAbsolutePath().normalize();
            Path created = mutation.apply(project, directory, text(message.payload(), "name"));
            Path parent = created.getParent();
            sendTreeChanged(created);
            Map<String, Object> payload = mutationPayload(created, parent, openFile);
            return message.response(payload);
        } catch (IOException | IllegalArgumentException exception) {
            return message.error(new WebShellError(errorCode, safeMessage(exception), true));
        }
    }

    private WebShellEnvelope duplicatePath(WebShellEnvelope message) {
        try {
            ProjectModel project = projects.requireCurrent();
            Path duplicate = fileOperations.duplicate(project, Path.of(text(message.payload(), "target")));
            sendTreeChanged(duplicate);
            return message.response(mutationPayload(duplicate, duplicate.getParent(), false));
        } catch (IOException | IllegalArgumentException exception) {
            return message.error(new WebShellError("DUPLICATE_FAILED", safeMessage(exception), true));
        }
    }

    private WebShellEnvelope renamePath(WebShellEnvelope message) {
        try {
            ProjectModel project = projects.requireCurrent();
            Path target = fileOperations.requireTarget(project, Path.of(text(message.payload(), "target")));
            List<EditorSession> affected = manager.sessionsUnder(target);
            Map<String, String> previousUris = new LinkedHashMap<>();
            for (EditorSession session : affected) previousUris.put(session.getSessionId(), MonacoModelId.forSession(session));
            documents.reidentifying(affected, true);
            try {
                EditorManager.PathMutationResult result = manager.renamePathSafely(project, target, text(message.payload(), "name"));
                if (result == EditorManager.PathMutationResult.SAVE_FAILED) {
                    return message.error(new WebShellError("RENAME_SAVE_FAILED", "Unable to save an open document before rename", true));
                }
                if (result != EditorManager.PathMutationResult.SUCCESS) {
                    return message.error(new WebShellError("RENAME_FAILED", "Unable to rename the selected path", true));
                }
            } finally {
                documents.reidentifying(affected, false);
            }
            for (EditorSession session : affected) {
                String previousUri = previousUris.get(session.getSessionId());
                WebDocumentSnapshot document = documents.snapshot(session);
                surface.send(WebShellEnvelope.event("document", "reidentified", Map.of(
                        "previousUri", previousUri, "document", document.payload())));
            }
            Path parent = target.getParent();
            if (parent != null) sendTreeChanged(target);
            Path renamed = target.resolveSibling(text(message.payload(), "name")).normalize();
            return message.response(mutationPayload(renamed, renamed.getParent(), false));
        } catch (IllegalArgumentException exception) {
            return message.error(new WebShellError("RENAME_FAILED", safeMessage(exception), true));
        }
    }

    private WebShellEnvelope deletePath(WebShellEnvelope message) {
        try {
            ProjectModel project = projects.requireCurrent();
            Path target = fileOperations.requireTarget(project, Path.of(text(message.payload(), "target")));
            List<EditorSession> affected = manager.sessionsUnder(target);
            Map<String, String> closedUris = new LinkedHashMap<>();
            for (EditorSession session : affected) closedUris.put(session.getSessionId(), MonacoModelId.forSession(session));
            EditorManager.PathMutationResult result = manager.deletePathSafely(project, target);
            if (result == EditorManager.PathMutationResult.DIRTY_DOCUMENTS) {
                return message.error(new WebShellError("DIRTY_DOCUMENTS", "Save or discard changes before deleting an open document", true));
            }
            if (result != EditorManager.PathMutationResult.SUCCESS) {
                return message.error(new WebShellError("DELETE_FAILED", "Unable to delete the selected path", true));
            }
            for (EditorSession session : affected) {
                documents.forgetSession(session.getSessionId());
                surface.send(WebShellEnvelope.event("document", "closed", Map.of("uri", closedUris.get(session.getSessionId()))));
            }
            Path parent = target.getParent();
            if (parent != null) sendTreeChanged(target);
            EditorSession active = manager.getCurrentSession();
            if (active != null) documents.sendActiveChanged(active);
            return message.response(Map.of("parent", parent == null ? "" : parent.toString()));
        } catch (IllegalArgumentException exception) {
            return message.error(new WebShellError("DELETE_FAILED", safeMessage(exception), true));
        }
    }

    private void sendTreeChanged(Path changedPath) {
        explorer.changedParent(projects.current(), changedPath).ifPresent(parent ->
                surface.send(WebShellEnvelope.event("workspace", "treeChanged", Map.of("parent", parent.toString()))));
    }

    private Map<String, Object> mutationPayload(Path path, Path parent, boolean openFile) {
        Map<String, Object> payload = new LinkedHashMap<>();
        Path target = path.toAbsolutePath().normalize();
        payload.put("path", target.toString());
        payload.put("parent", parent == null ? "" : parent.toAbsolutePath().normalize().toString());
        payload.put("openFile", openFile);
        ProjectModel project = projects.current();
        if (project != null) {
            payload.put("ancestors", explorer.ancestors(project, target).stream().map(Path::toString).toList());
        }
        return payload;
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null ? "The filesystem operation failed" : exception.getMessage();
    }

    @FunctionalInterface
    private interface DirectoryMutation {
        Path apply(ProjectModel project, Path directory, String name) throws IOException;
    }

    private Map<String, Object> workspacePayload() {
        ProjectModel project = projects.current();
        Map<String, Object> payload = new LinkedHashMap<>();
        if (project != null) payload.put("project", projectPayload(project));
        payload.put("recentProjects", projects.recent().stream()
                .map(info -> Map.<String, Object>of("name", info.getName(), "path", info.getPath()))
                .toList());
        return payload;
    }

    private Map<String, Object> projectPayload(ProjectModel project) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", project.getName());
        payload.put("path", project.getRootDir().toString());
        payload.put("type", project.getType().name());
        payload.put("root", treePayload(explorer.node(project.getRootDir()), true));
        return payload;
    }

    private Map<String, Object> revealPayload(ProjectModel project, Path target) {
        return Map.of("targetPath", target.toAbsolutePath().normalize().toString(),
                "ancestors", explorer.ancestors(project, target).stream().map(Path::toString).toList());
    }

    private WebShellEnvelope nativeUiUnavailable(WebShellEnvelope message, String detail) {
        return message.error(new WebShellError("NATIVE_UI_UNAVAILABLE", detail, true));
    }

    private Map<String, Object> treePayload(ProjectExplorerQuery.Entry entry, boolean root) {
        return Map.of("name", entry.name(), "path", entry.path().toString(),
                "kind", entry.directory() ? (root ? "project" : "directory") : "file",
                "hasChildren", entry.hasChildren());
    }
}
