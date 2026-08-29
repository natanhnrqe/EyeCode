package com.eyecode.javafx.web;

import com.eyecode.autosave.ExternalFileEvent;
import com.eyecode.autosave.SavedEvent;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.filesystem.DefaultFileSystemService;
import com.eyecode.javafx.monaco.MonacoModelId;
import com.eyecode.workbench.editor.EditorManager;
import com.eyecode.workbench.editor.EditorSession;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WebShellWorkspaceController {
    private final JavaFxWebShellSurface surface;
    private final EditorManager manager;
    private final WebShellCompletionController completionController;
    private final WebShellLearningController learningController;
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
        manager.addSaveListener(this::onSaved);
        manager.addExternalFileListener(this::onExternalChanged);
        surface.registerHandler("document", "open", this::open);
        surface.registerHandler("document", "new", this::newDocument);
        surface.registerHandler("document", "activate", this::activate);
        surface.registerHandler("document", "change", this::change);
        surface.registerHandler("document", "save", this::save);
        surface.registerHandler("document", "close", this::close);
    }

    public EditorManager editorManager() {
        return manager;
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        manager.closeAllSessions();
        manager.shutdownAutosave();
        observedDocuments.clear();
        untitledNames.clear();
    }

    private WebShellEnvelope open(WebShellEnvelope message) {
        String rawPath = text(message.payload(), "path");
        if (rawPath.isBlank()) rawPath = text(message.payload(), "uri");
        if (rawPath.isBlank()) return message.error(new WebShellError(
                "INVALID_DOCUMENT", "A file path or file URI is required", true));
        try {
            Path path = rawPath.startsWith("file:")
                    ? MonacoModelId.pathForModel(rawPath).orElseThrow()
                    : Path.of(rawPath);
            path = path.toAbsolutePath().normalize();
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

    private WebShellEnvelope activate(WebShellEnvelope message) {
        EditorSession session = sessionFor(message.payload());
        if (session == null) return message.error(new WebShellError(
                "DOCUMENT_NOT_OPEN", "The requested document is not open", true));
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
        if (session == null) return message.error(new WebShellError(
                "DOCUMENT_NOT_OPEN", "The requested document is not open", true));
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
        if (session == null) return message.error(new WebShellError(
                "DOCUMENT_NOT_OPEN", "The requested document is not open", true));
        boolean closed = manager.closeSession(session.getSessionId());
        if (!closed) return message.error(new WebShellError(
                "CLOSE_FAILED", "The document could not be closed", true));
        observedDocuments.remove(session.getSessionId());
        untitledNames.remove(session.getSessionId());
        surface.send(WebShellEnvelope.event("document", "closed", Map.of(
                "uri", MonacoModelId.forSession(session))));
        EditorSession active = manager.getCurrentSession();
        if (active != null) sendActiveChanged(active);
        return message.response(Map.of("closed", true));
    }

    private EditorSession openPath(Path path) {
        EditorSession session = manager.openDocument(path.toAbsolutePath().normalize());
        observe(session);
        WebDocumentSnapshot result = snapshot(session);
        surface.send(WebShellEnvelope.event("document", "opened", result.payload()));
        sendActiveChanged(session);
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
        if (session != null) surface.send(WebShellEnvelope.event("document", "externalChanged",
                snapshot(session).payload()));
    }

    private void sendActiveChanged(EditorSession session) {
        surface.send(WebShellEnvelope.event("document", "activeChanged", Map.of(
                "uri", MonacoModelId.forSession(session),
                "documentId", session.getDocumentId())));
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

    private static String text(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static long number(Map<String, Object> payload, String key, long fallback) {
        Object value = payload == null ? null : payload.get(key);
        return value instanceof Number number ? number.longValue() : fallback;
    }
}
