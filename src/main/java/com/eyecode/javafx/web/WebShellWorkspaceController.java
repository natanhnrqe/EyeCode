package com.eyecode.javafx.web;

import com.eyecode.autosave.ExternalFileEvent;
import com.eyecode.autosave.ExternalFileState;
import com.eyecode.autosave.SavedEvent;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.filesystem.DefaultFileSystemService;
import com.eyecode.javafx.monaco.MonacoModelId;
import com.eyecode.project.ProjectLifecycleService;
import com.eyecode.project.model.ProjectModel;
import com.eyecode.runtime.RunConfiguration;
import com.eyecode.runtime.RunService;
import com.eyecode.workbench.editor.EditorManager;
import com.eyecode.workbench.editor.EditorSession;
import javafx.stage.DirectoryChooser;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

public final class WebShellWorkspaceController {
    private final JavaFxWebShellSurface surface;
    private final EditorManager manager;
    private final WebShellCompletionController completionController;
    private final WebShellLearningController learningController;
    private final ProjectLifecycleService projectLifecycleService;
    private final RunService runService;
    private final Map<String, EditorDocument> observedDocuments = new LinkedHashMap<>();
    private final Map<String, String> untitledNames = new LinkedHashMap<>();
    private int nextUntitledNumber = 1;
    private boolean disposed;

    public WebShellWorkspaceController(JavaFxWebShellSurface surface) {
        this.surface = surface;
        this.manager = new EditorManager(null, new DefaultFileSystemService(),
                new WebShellEditorViewFactory());
        this.completionController = new WebShellCompletionController(surface, manager);
        this.learningController = new WebShellLearningController(surface, manager);
        this.projectLifecycleService = new ProjectLifecycleService();
        this.runService = new RunService(projectLifecycleService);
        this.runService.setBeforeRunFlush(manager::flushAutosave);
        this.runService.addListener(new RunService.Listener() {
            @Override public void onStarted(com.eyecode.runtime.RunRequest request) { sendRunState(); }
            @Override public void onOutput(String line, boolean error) {
                if (line != null && !line.isBlank()) {
                    surface.send(WebShellEnvelope.event("run", "output", Map.of(
                            "line", line, "error", error)));
                }
            }
            @Override public void onFinished(int exitCode, boolean stopped) { sendRunState(); }
        });
        manager.addSaveListener(this::onSaved);
        manager.addExternalFileListener(this::onExternalChanged);
        surface.registerHandler("document", "open", this::open);
        surface.registerHandler("document", "new", this::newDocument);
        surface.registerHandler("document", "activate", this::activate);
        surface.registerHandler("document", "change", this::change);
        surface.registerHandler("document", "save", this::save);
        surface.registerHandler("document", "close", this::close);
        surface.registerHandler("workspace", "snapshot", this::workspaceSnapshot);
        surface.registerHandler("workspace", "openProject", this::openProject);
        surface.registerHandler("workspace", "refresh", this::refreshWorkspace);
        surface.registerHandler("workspace", "children", this::workspaceChildren);
        surface.registerHandler("workspace", "openFile", this::openWorkspaceFile);
        surface.registerHandler("run", "state", this::runState);
        surface.registerHandler("run", "run", this::run);
        surface.registerHandler("run", "rerun", this::rerun);
        surface.registerHandler("run", "stop", this::stop);
        surface.registerHandler("run", "selectConfiguration", this::selectRunConfiguration);
    }

    public EditorManager editorManager() {
        return manager;
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        manager.closeAllSessions();
        manager.shutdownAutosave();
        runService.dispose();
        projectLifecycleService.close();
        observedDocuments.clear();
        untitledNames.clear();
    }

    private WebShellEnvelope open(WebShellEnvelope message) {
        String rawPath = text(message.payload(), "path");
        if (rawPath.isBlank()) rawPath = text(message.payload(), "uri");
        traceRequest("document/open", message.payload());
        if (rawPath.isBlank()) return message.error(new WebShellError(
                "INVALID_DOCUMENT", "A file path or file URI is required", true));
        try {
            Path path = rawPath.startsWith("file:")
                    ? MonacoModelId.pathForModel(rawPath).orElseThrow()
                    : Path.of(rawPath);
            path = path.toAbsolutePath().normalize();
            tracePath("document/open", rawPath, path, isAlreadyOpen(path));
            if (!java.nio.file.Files.isRegularFile(path)) {
                return message.error(new WebShellError("DOCUMENT_NOT_FOUND", path.toString(), true));
            }
            EditorSession session = openPath(path);
            WebDocumentSnapshot responseSnapshot = snapshot(session);
            return message.response(Map.of("document", responseSnapshot.payload()));
        } catch (RuntimeException exception) {
            return message.error(new WebShellError("INVALID_DOCUMENT",
                    exception.getMessage() == null ? "Unable to open document" : exception.getMessage(), true));
        }
    }

    private WebShellEnvelope workspaceSnapshot(WebShellEnvelope message) {
        return message.response(workspacePayload());
    }

    private WebShellEnvelope openProject(WebShellEnvelope message) {
        String rawPath = text(message.payload(), "path");
        Path root = rawPath.isBlank() ? chooseProjectDirectory() : Path.of(rawPath);
        if (root == null) return message.response(Map.of("cancelled", true));
        try {
            ProjectModel project = projectLifecycleService.open(root);
            projectLifecycleService.recordRecent(project);
            manager.closeAllSessions();
            manager.watchProject(project.getRootDir());
            runService.refreshConfigurations();
            Map<String, Object> payload = workspacePayload();
            preferredEntryPoint(project).ifPresent(path -> payload.put("reveal", revealPayload(project, path)));
            surface.send(WebShellEnvelope.event("workspace", "changed", payload));
            sendRunState();
            return message.response(payload);
        } catch (IllegalArgumentException exception) {
            return message.error(new WebShellError("INVALID_PROJECT", exception.getMessage(), true));
        }
    }

    private WebShellEnvelope workspaceChildren(WebShellEnvelope message) {
        ProjectModel project = projectLifecycleService.currentProject();
        if (project == null) return message.response(Map.of("children", List.of()));
        String rawPath = text(message.payload(), "path");
        Path directory = rawPath.isBlank() ? project.getRootDir() : Path.of(rawPath);
        Path root = project.getRootDir().toAbsolutePath().normalize();
        directory = directory.toAbsolutePath().normalize();
        if (!directory.startsWith(root) || !Files.isDirectory(directory)) {
            return message.error(new WebShellError("INVALID_TREE_PATH", "The requested folder is not in the project", true));
        }
        return message.response(Map.of("parent", directory.toString(), "children", treeChildren(directory)));
    }

    private WebShellEnvelope refreshWorkspace(WebShellEnvelope message) {
        ProjectModel project = projectLifecycleService.currentProject();
        if (project == null) return message.response(workspacePayload());
        Path root = project.getRootDir().toAbsolutePath().normalize();
        List<String> validPaths = paths(message.payload(), "paths").stream()
                .map(Path::of)
                .map(path -> path.toAbsolutePath().normalize())
                .filter(path -> path.startsWith(root) && Files.isDirectory(path))
                .map(Path::toString)
                .toList();
        Map<String, Object> payload = workspacePayload();
        payload.put("validPaths", validPaths.isEmpty() ? List.of(root.toString()) : validPaths);
        return message.response(payload);
    }

    private WebShellEnvelope openWorkspaceFile(WebShellEnvelope message) {
        String rawPath = text(message.payload(), "path");
        traceRequest("workspace/openFile", message.payload());
        if (rawPath.isBlank()) return message.error(new WebShellError(
                "INVALID_DOCUMENT", "A project file path is required", true));
        try {
            Path path = Path.of(rawPath).toAbsolutePath().normalize();
            tracePath("workspace/openFile", rawPath, path, isAlreadyOpen(path));
            ProjectModel project = projectLifecycleService.currentProject();
            if (project == null || !path.startsWith(project.getRootDir()) || !Files.isRegularFile(path)) {
                return message.error(new WebShellError("DOCUMENT_NOT_FOUND", path.toString(), true));
            }
            EditorSession session = openPath(path);
            return message.response(Map.of("document", snapshot(session).payload()));
        } catch (RuntimeException exception) {
            return message.error(new WebShellError("INVALID_DOCUMENT", exception.getMessage(), true));
        }
    }

    private WebShellEnvelope runState(WebShellEnvelope message) {
        return message.response(runPayload());
    }

    private WebShellEnvelope run(WebShellEnvelope message) {
        boolean started = runService.runCurrent();
        sendRunState();
        return message.response(Map.of("started", started));
    }

    private WebShellEnvelope rerun(WebShellEnvelope message) {
        boolean started = runService.rerun();
        sendRunState();
        return message.response(Map.of("started", started));
    }

    private WebShellEnvelope stop(WebShellEnvelope message) {
        runService.stop();
        sendRunState();
        return message.response(Map.of("stopped", true));
    }

    private WebShellEnvelope selectRunConfiguration(WebShellEnvelope message) {
        boolean selected = runService.selectConfiguration(text(message.payload(), "id"));
        sendRunState();
        return message.response(Map.of("selected", selected));
    }

    private WebShellEnvelope activate(WebShellEnvelope message) {
        EditorSession session = sessionFor(message.payload());
        if (session == null) return documentNotOpen(message, "document/activate");
        traceSession("document/activate", session, false);
        manager.activateSession(session.getSessionId());
        sendActiveChanged(session);
        return message.response(Map.of("document", snapshot(session).payload()));
    }

    private WebShellEnvelope newDocument(WebShellEnvelope message) {
        String content = text(message.payload(), "content");
        try {
            EditorSession session = manager.openDocument(null, content);
            String displayName = "Untitled " + nextUntitledNumber++ + ".java";
            untitledNames.put(session.getSessionId(), displayName);
            observe(session);
            WebDocumentSnapshot result = snapshot(session);
            surface.send(WebShellEnvelope.event("document", "opened", result.payload()));
            sendActiveChanged(session);
            return message.response(Map.of("document", result.payload()));
        } catch (RuntimeException exception) {
            return message.error(new WebShellError("NEW_DOCUMENT_FAILED",
                    exception.getMessage() == null ? "Unable to create document" : exception.getMessage(), true));
        }
    }

    private WebShellEnvelope change(WebShellEnvelope message) {
        EditorSession session = sessionFor(message.payload());
        if (session == null) return documentNotOpen(message, "document/change");
        traceSession("document/change", session, false);
        EditorDocument document = documentFor(session);
        if (document == null) return message.error(new WebShellError(
                "DOCUMENT_UNAVAILABLE", "The document is unavailable", true));
        long expectedVersion = number(message.payload(), "version", document.currentVersion());
        if (expectedVersion != document.currentVersion()) return message.error(new WebShellError(
                "DOCUMENT_VERSION_CONFLICT", "The document version is no longer current", true));
        if (!message.payload().containsKey("content")) return message.error(new WebShellError(
                "INVALID_DOCUMENT", "Document content is required", true));
        String content = text(message.payload(), "content");
        if (!content.equals(document.snapshot().getText())) document.setText(content);
        WebDocumentSnapshot result = snapshot(session);
        surface.send(WebShellEnvelope.event("document", "changed", result.payload()));
        traceSession("document/changed", session, false);
        traceRegistry("document/change");
        return message.response(Map.of("document", result.payload()));
    }

    private WebShellEnvelope save(WebShellEnvelope message) {
        EditorSession session = sessionFor(message.payload());
        if (session == null) return message.error(new WebShellError(
                "DOCUMENT_NOT_OPEN", "The requested document is not open", true));
        if (session.getFile() == null) return saveAs(message, session);
        boolean saved = manager.flushSession(session.getSessionId());
        if (!saved) return message.error(new WebShellError(
                "SAVE_FAILED", "The document could not be saved", true));
        return message.response(Map.of("document", snapshot(session).payload()));
    }

    private WebShellEnvelope saveAs(WebShellEnvelope message, EditorSession session) {
        Path destination = chooseSaveTarget(session);
        if (destination == null) return message.response(Map.of("cancelled", true));
        String previousUri = MonacoModelId.forSession(session);
        if (manager.getSessions().stream()
                .anyMatch(other -> other != session
                        && MonacoModelId.identity(destination).equals(MonacoModelId.identity(other.getFile())))) {
            return message.error(new WebShellError(
                    "DOCUMENT_ALREADY_OPEN",
                    "The selected destination is already open",
                    true));
        }
        if (!manager.saveAs(session.getSessionId(), destination)) {
            return message.error(new WebShellError(
                    "SAVE_AS_FAILED",
                    "The document could not be saved to the selected destination",
                    true));
        }
        WebDocumentSnapshot result = snapshot(session);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("previousUri", previousUri);
        payload.put("document", result.payload());
        surface.send(WebShellEnvelope.event("document", "reidentified", payload));
        return message.response(payload);
    }

    private Path chooseSaveTarget(EditorSession session) {
        if (Platform.isFxApplicationThread()) return showSaveDialog(session);
        CompletableFuture<Path> result = new CompletableFuture<>();
        try {
            Platform.runLater(() -> {
                try {
                    result.complete(showSaveDialog(session));
                } catch (RuntimeException exception) {
                    result.completeExceptionally(exception);
                }
            });
            return result.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException | IllegalStateException exception) {
            return null;
        }
    }

    private Path showSaveDialog(EditorSession session) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Java File");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Java Files", "*.java"));
        String suggestedName = untitledNames.get(session.getSessionId());
        if (suggestedName != null) chooser.setInitialFileName(suggestedName);
        Window owner = surface.getScene() == null ? null : surface.getScene().getWindow();
        java.io.File selected = chooser.showSaveDialog(owner);
        return selected == null ? null : selected.toPath().toAbsolutePath().normalize();
    }

    private WebShellEnvelope close(WebShellEnvelope message) {
        EditorSession session = sessionFor(message.payload());
        if (session == null) return documentNotOpen(message, "document/close");
        traceSession("document/close", session, false);
        boolean closed = manager.closeSession(session.getSessionId());
        if (!closed) return message.error(new WebShellError(
                "CLOSE_FAILED", "The document could not be closed", true));
        observedDocuments.remove(session.getSessionId());
        untitledNames.remove(session.getSessionId());
        surface.send(WebShellEnvelope.event("document", "closed", Map.of(
                "uri", MonacoModelId.forSession(session))));
        traceSession("document/closed", session, false);
        traceRegistry("document/close");
        EditorSession active = manager.getCurrentSession();
        if (active != null) sendActiveChanged(active);
        return message.response(Map.of("closed", true));
    }

    private EditorSession openPath(Path path) {
        boolean alreadyOpen = isAlreadyOpen(path);
        EditorSession session = manager.openDocument(path.toAbsolutePath().normalize());
        traceSession("document/opened", session, alreadyOpen);
        observe(session);
        WebDocumentSnapshot result = snapshot(session);
        surface.send(WebShellEnvelope.event("document", "opened", result.payload()));
        sendActiveChanged(session);
        traceRegistry("document/open");
        return session;
    }

    private void observe(EditorSession session) {
        if (observedDocuments.containsKey(session.getSessionId())) return;
        EditorDocument document = documentFor(session);
        if (document == null) return;
        observedDocuments.put(session.getSessionId(), document);
        document.addDocumentChangeListener(event -> {
            if (!disposed) surface.send(WebShellEnvelope.event("document", "changed",
                    snapshot(session).payload()));
        });
        document.addDirtyChangeListener(dirty -> {
            if (!disposed) surface.send(WebShellEnvelope.event("document", "changed",
                    snapshot(session).payload()));
        });
    }

    private void onSaved(SavedEvent event) {
        if (disposed || event == null) return;
        EditorSession session = sessionForPath(event.path());
        if (session == null) return;
        surface.send(WebShellEnvelope.event("document", event.succeeded() ? "saved" : "saveFailed",
                Map.of("document", snapshot(session).payload(),
                        "message", event.error() == null ? "" : event.error().getMessage())));
    }

    private void onExternalChanged(ExternalFileEvent event) {
        if (disposed || event == null) return;
        EditorSession session = sessionForPath(event.path());
        if (session != null && event.state() != ExternalFileState.SYNCED
                && event.state() != ExternalFileState.IGNORED) surface.send(WebShellEnvelope.event("document", "externalChanged",
                snapshot(session).payload()));
        sendTreeChanged(event.path());
    }

    private void sendTreeChanged(Path changedPath) {
        ProjectModel project = projectLifecycleService.currentProject();
        if (project == null || changedPath == null) return;
        Path root = project.getRootDir().toAbsolutePath().normalize();
        Path changed = changedPath.toAbsolutePath().normalize();
        if (!changed.startsWith(root)) return;
        Path parent = changed.getParent();
        if (parent == null || !parent.startsWith(root)) return;
        surface.send(WebShellEnvelope.event("workspace", "treeChanged", Map.of("parent", parent.toString())));
    }

    private void sendActiveChanged(EditorSession session) {
        surface.send(WebShellEnvelope.event("document", "activeChanged", Map.of(
                "uri", MonacoModelId.forSession(session),
                "documentId", session.getDocumentId())));
        traceSession("document/activeChanged", session, false);
    }

    private EditorSession sessionFor(Map<String, Object> payload) {
        String uri = text(payload, "uri");
        String documentId = text(payload, "documentId");
        return manager.getSessions().stream()
                .filter(session -> (!uri.isBlank() && (MonacoModelId.forSession(session).equals(uri)
                        || MonacoModelId.matches(uri, session.getFile())))
                        || (!documentId.isBlank() && documentId.equals(session.getDocumentId())))
                .findFirst().orElse(null);
    }

    private EditorSession sessionForPath(Path path) {
        if (path == null) return null;
        String identity = MonacoModelId.identity(path);
        return manager.getSessions().stream()
                .filter(session -> identity.equals(MonacoModelId.identity(session.getFile())))
                .findFirst().orElse(null);
    }

    private WebShellEnvelope documentNotOpen(WebShellEnvelope message, String operation) {
        traceRequest(operation + " DOCUMENT_NOT_OPEN", message.payload());
        traceRegistry(operation + " miss");
        return message.error(new WebShellError(
                "DOCUMENT_NOT_OPEN", "The requested document is not open", true));
    }

    private EditorDocument documentFor(EditorSession session) {
        return session == null ? null : manager.getBuffer(session.getSessionId())
                .map(buffer -> buffer.getDocument()).orElse(null);
    }

    private WebDocumentSnapshot snapshot(EditorSession session) {
        String displayName = untitledNames.get(session.getSessionId());
        return displayName == null
                ? WebDocumentSnapshot.file(session, documentFor(session))
                : WebDocumentSnapshot.untitled(session, documentFor(session), displayName);
    }

    private Map<String, Object> workspacePayload() {
        ProjectModel project = projectLifecycleService.currentProject();
        Map<String, Object> payload = new LinkedHashMap<>();
        if (project != null) payload.put("project", projectPayload(project));
        payload.put("recentProjects", projectLifecycleService.recentProjects().stream()
                .map(info -> Map.<String, Object>of("name", info.getName(), "path", info.getPath()))
                .toList());
        return payload;
    }

    private Map<String, Object> projectPayload(ProjectModel project) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", project.getName());
        payload.put("path", project.getRootDir().toString());
        payload.put("type", project.getType().name());
        payload.put("root", treeNode(project.getRootDir(), true));
        return payload;
    }

    private Optional<Path> preferredEntryPoint(ProjectModel project) {
        RunConfiguration selected = runService.selectedConfiguration();
        if (selected != null && selected.projectRoot().equals(project.getRootDir().toAbsolutePath().normalize())) {
            Optional<Path> source = sourceFor(project, selected.mainClass());
            if (source.isPresent()) return source;
        }
        for (Path sourceRoot : sourceRoots(project)) {
            if (!Files.isDirectory(sourceRoot)) continue;
            try (var paths = Files.walk(sourceRoot, 12)) {
                Optional<Path> main = paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().equals("Main.java"))
                        .filter(this::isSensibleSource)
                        .sorted(Comparator.comparing(path -> path.toAbsolutePath().normalize().toString()))
                        .findFirst();
                if (main.isPresent()) return main.map(path -> path.toAbsolutePath().normalize());
            } catch (IOException ignored) {
            }
        }
        return Optional.empty();
    }

    private Optional<Path> sourceFor(ProjectModel project, String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isBlank()) return Optional.empty();
        String relative = qualifiedName.replace('.', File.separatorChar) + ".java";
        return sourceRoots(project).stream().map(root -> root.resolve(relative).normalize())
                .filter(Files::isRegularFile).findFirst();
    }

    private List<Path> sourceRoots(ProjectModel project) {
        Path root = project.getRootDir().toAbsolutePath().normalize();
        Path standard = root.resolve("src/main/java");
        return Files.isDirectory(standard) ? List.of(standard) : List.of(root.resolve("src"));
    }

    private boolean isSensibleSource(Path path) {
        for (Path part : path) {
            if (Set.of("target", "build", "out", ".gradle", ".idea", "node_modules", ".git").contains(part.toString())) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Object> revealPayload(ProjectModel project, Path target) {
        Path root = project.getRootDir().toAbsolutePath().normalize();
        List<Path> reverseAncestors = new ArrayList<>();
        for (Path current = target.getParent(); current != null && !current.equals(root); current = current.getParent()) {
            reverseAncestors.add(current);
        }
        java.util.Collections.reverse(reverseAncestors);
        return Map.of("targetPath", target.toAbsolutePath().normalize().toString(),
                "ancestors", reverseAncestors.stream().map(Path::toString).toList());
    }

    private Map<String, Object> treeNode(Path path, boolean root) {
        Path normalized = path.toAbsolutePath().normalize();
        boolean directory = Files.isDirectory(normalized);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", normalized.getFileName().toString());
        payload.put("path", normalized.toString());
        payload.put("kind", directory ? (root ? "project" : "directory") : "file");
        payload.put("hasChildren", directory && hasVisibleChildren(normalized));
        return payload;
    }

    private List<Map<String, Object>> treeChildren(Path directory) {
        try (var stream = Files.list(directory)) {
            return stream.filter(this::isVisibleProjectPath)
                    .sorted(Comparator
                            .comparing((Path path) -> !Files.isDirectory(path))
                            .thenComparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .map(path -> treeNode(path, false))
                    .toList();
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private boolean hasVisibleChildren(Path directory) {
        try (var stream = Files.list(directory)) {
            return stream.anyMatch(this::isVisibleProjectPath);
        } catch (IOException ignored) {
            return false;
        }
    }

    private boolean isVisibleProjectPath(Path path) {
        if (!Files.isDirectory(path)) return true;
        String name = path.getFileName().toString();
        return !Set.of(".git", ".idea", ".gradle", ".eyecode", "target", "build", "out").contains(name);
    }

    private Path chooseProjectDirectory() {
        if (Platform.isFxApplicationThread()) return showProjectDialog();
        CompletableFuture<Path> result = new CompletableFuture<>();
        try {
            Platform.runLater(() -> {
                try {
                    result.complete(showProjectDialog());
                } catch (RuntimeException exception) {
                    result.completeExceptionally(exception);
                }
            });
            return result.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException | IllegalStateException exception) {
            return null;
        }
    }

    private Path showProjectDialog() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open Project");
        Window owner = surface.getScene() == null ? null : surface.getScene().getWindow();
        File selected = chooser.showDialog(owner);
        return selected == null ? null : selected.toPath().toAbsolutePath().normalize();
    }

    private Map<String, Object> runPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("running", runService.isRunning());
        payload.put("rerunAvailable", runService.hasLastRequest());
        payload.put("configurations", runService.configurations().stream()
                .map(this::runConfigurationPayload).toList());
        RunConfiguration selected = runService.selectedConfiguration();
        payload.put("selectedConfigurationId", selected == null ? "" : selected.id());
        return payload;
    }

    private Map<String, Object> runConfigurationPayload(RunConfiguration configuration) {
        return Map.of("id", configuration.id(), "name", configuration.displayName(),
                "mainClass", configuration.mainClass(), "kind", configuration.kind().name());
    }

    private void sendRunState() {
        if (!disposed) surface.send(WebShellEnvelope.event("run", "state", runPayload()));
    }

    private boolean isAlreadyOpen(Path path) {
        String identity = MonacoModelId.identity(path);
        return manager.getSessions().stream()
                .anyMatch(session -> identity.equals(MonacoModelId.identity(session.getFile())));
    }

    private void traceRequest(String operation, Map<String, Object> payload) {
        System.out.printf("WEB_DOCUMENT operation=%s rawPath=%s uri=%s documentId=%s%n",
                operation, text(payload, "path"), text(payload, "uri"), text(payload, "documentId"));
    }

    private void tracePath(String operation, String rawPath, Path normalizedPath, boolean alreadyOpen) {
        System.out.printf("WEB_DOCUMENT operation=%s rawPath=%s normalizedPath=%s canonicalPath=%s alreadyOpen=%s%n",
                operation, rawPath, normalizedPath, MonacoModelId.identity(normalizedPath), alreadyOpen);
    }

    private void traceSession(String operation, EditorSession session, boolean alreadyOpen) {
        System.out.printf("WEB_DOCUMENT operation=%s sessionId=%s documentId=%s uri=%s displayName=%s file=%s canonicalPath=%s alreadyOpen=%s%n",
                operation,
                session.getSessionId(),
                session.getDocumentId(),
                MonacoModelId.forSession(session),
                session.getDisplayName(),
                session.getFile(),
                MonacoModelId.identity(session.getFile()),
                alreadyOpen);
    }

    private void traceRegistry(String operation) {
        StringBuilder sessions = new StringBuilder();
        for (EditorSession session : manager.getSessions()) {
            if (!sessions.isEmpty()) sessions.append(" | ");
            sessions.append("sessionId=").append(session.getSessionId())
                    .append(",documentId=").append(session.getDocumentId())
                    .append(",uri=").append(MonacoModelId.forSession(session))
                    .append(",file=").append(session.getFile())
                    .append(",canonicalPath=").append(MonacoModelId.identity(session.getFile()));
        }
        System.out.printf("WEB_DOCUMENT_REGISTRY operation=%s sessions=[%s]%n", operation, sessions);
    }

    private static String text(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static long number(Map<String, Object> payload, String key, long fallback) {
        Object value = payload == null ? null : payload.get(key);
        return value instanceof Number number ? number.longValue() : fallback;
    }

    private static List<String> paths(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        if (!(value instanceof Iterable<?> values)) return List.of();
        List<String> result = new ArrayList<>();
        for (Object item : values) {
            if (item != null) result.add(String.valueOf(item));
        }
        return result;
    }
}
