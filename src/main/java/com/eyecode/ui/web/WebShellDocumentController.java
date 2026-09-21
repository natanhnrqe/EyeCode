package com.eyecode.ui.web;

import com.eyecode.autosave.ExternalFileEvent;
import com.eyecode.autosave.ExternalFileState;
import com.eyecode.autosave.SavedEvent;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.intelligence.events.DocumentChangeListener;
import com.eyecode.language.DocumentLanguageResolver;
import com.eyecode.language.LanguageDocument;
import com.eyecode.ui.web.monaco.MonacoModelId;
import com.eyecode.language.documentation.JdkSourceDeclarationLocator;
import com.eyecode.language.documentation.JdkSourceLoader;
import com.eyecode.language.documentation.JdkSourceTarget;
import com.eyecode.learning.content.DocumentationTarget;
import com.eyecode.workbench.editor.EditorManager;
import com.eyecode.workbench.editor.EditorSession;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import static com.eyecode.ui.web.WebShellPayload.*;

public final class WebShellDocumentController {
    private final WebShellSurface surface;
    private final WebShellNativeFileSelection nativeUi;
    private final EditorManager manager;
    private final WebShellDiagnosticsController diagnosticsController;
    private final DocumentLanguageResolver languageResolver;
    private final JdkSourceLoader jdkSourceLoader = new JdkSourceLoader();
    private final JdkSourceDeclarationLocator jdkSourceDeclarationLocator = new JdkSourceDeclarationLocator();
    private final Map<String, WebJdkSourceDocument> jdkSourceDocuments = new LinkedHashMap<>();
    private final Map<String, WebDocumentationDocument> documentationDocuments = new LinkedHashMap<>();
    private final WebShellDocumentationHost documentationHost;
    private final java.util.function.Consumer<DocumentationTarget> documentationOpener;
    private final Map<String, DocumentObservation> observedDocuments = new LinkedHashMap<>();
    private final Map<String, String> untitledNames = new LinkedHashMap<>();
    private final Set<String> reidentifyingSessions = new java.util.HashSet<>();
    private final Consumer<SavedEvent> saveListener;
    private final Consumer<ExternalFileEvent> externalFileListener;
    private final Consumer<Path> documentClosed;
    private int nextUntitledNumber = 1;
    private boolean disposed;

    WebShellDocumentController(WebShellSurface surface, Consumer<DocumentationTarget> documentationOpener,
                               WebShellDocumentationHost documentationHost, WebShellNativeFileSelection nativeUi,
                               EditorManager manager, WebShellDiagnosticsController diagnosticsController,
                               DocumentLanguageResolver languageResolver) {
        this(surface, documentationOpener, documentationHost, nativeUi, manager, diagnosticsController, languageResolver,
                ignored -> { });
    }

    WebShellDocumentController(WebShellSurface surface, Consumer<DocumentationTarget> documentationOpener,
                               WebShellDocumentationHost documentationHost, WebShellNativeFileSelection nativeUi,
                               EditorManager manager, WebShellDiagnosticsController diagnosticsController,
                               DocumentLanguageResolver languageResolver, Consumer<Path> documentClosed) {
        this.surface = Objects.requireNonNull(surface);
        this.nativeUi = Objects.requireNonNull(nativeUi);
        this.manager = Objects.requireNonNull(manager);
        this.diagnosticsController = Objects.requireNonNull(diagnosticsController);
        this.languageResolver = Objects.requireNonNull(languageResolver);
        this.documentClosed = documentClosed == null ? ignored -> { } : documentClosed;
        this.documentationOpener = documentationOpener == null ? target -> { } : documentationOpener;
        this.documentationHost = documentationHost;
        this.saveListener = this::onSaved;
        this.externalFileListener = this::onExternalChanged;
        manager.addSaveListener(saveListener);
        manager.addExternalFileListener(externalFileListener);
        surface.registerHandler("document", "open", this::open);
        surface.registerHandler("document", "new", this::newDocument);
        surface.registerHandler("document", "activate", this::activate);
        surface.registerHandler("document", "change", this::change);
        surface.registerHandler("document", "save", this::save);
        surface.registerHandler("document", "close", this::close);
        surface.registerHandler("document", "layout", this::documentationLayout);
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        manager.removeSaveListener(saveListener);
        manager.removeExternalFileListener(externalFileListener);
        reset();
    }

    void reset() {
        diagnosticsController.clear();
        for (String id : List.copyOf(observedDocuments.keySet())) forgetSession(id);
        untitledNames.clear();
        reidentifyingSessions.clear();
        jdkSourceDocuments.clear();
        documentationDocuments.clear();
        if (documentationHost != null) documentationHost.hide();
    }

    void forgetSession(String id) {
        DocumentObservation observation = observedDocuments.remove(id);
        if (observation != null) observation.close();
        untitledNames.remove(id);
    }

    void reidentifying(List<EditorSession> sessions, boolean active) {
        for (EditorSession session : sessions) {
            if (active) reidentifyingSessions.add(session.getSessionId());
            else reidentifyingSessions.remove(session.getSessionId());
        }
    }

    private void observe(EditorSession session) {
        if (observedDocuments.containsKey(session.getSessionId())) return;
        EditorDocument document = documentFor(session);
        if (document == null) return;
        Runnable publish = () -> {
            if (!disposed && !reidentifyingSessions.contains(session.getSessionId())) {
                surface.send(WebShellEnvelope.event("document", "changed", snapshot(session).payload()));
            }
        };
        DocumentChangeListener changes = event -> publish.run();
        EditorDocument.DirtyChangeListener dirty = value -> publish.run();
        document.addDocumentChangeListener(changes);
        document.addDirtyChangeListener(dirty);
        observedDocuments.put(session.getSessionId(), new DocumentObservation(document, changes, dirty));
    }

    private record DocumentObservation(EditorDocument document, DocumentChangeListener changes,
                                       EditorDocument.DirtyChangeListener dirty) {
        void close() {
            document.removeDocumentChangeListener(changes);
            document.removeDirtyChangeListener(dirty);
        }
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
            if (!manager.isExistingFile(path)) {
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
        WebDocumentationDocument documentation = documentationFor(message.payload());
        if (documentation != null) {
            if (documentationHost != null) documentationHost.open(documentation.target());
            sendActiveChanged(documentation);
            return message.response(Map.of("document", documentation.payload()));
        }
        WebJdkSourceDocument source = sourceFor(message.payload());
        if (source != null) {
            sendActiveChanged(source);
            return message.response(Map.of("document", source.payload()));
        }
        EditorSession session = sessionFor(message.payload());
        if (session == null) return message.error(new WebShellError(
                "DOCUMENT_NOT_OPEN", "The requested document is not open", true));
        if (documentationHost != null) documentationHost.hide();
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
        if (sourceFor(message.payload()) != null) {
            return message.error(new WebShellError("DOCUMENT_READ_ONLY",
                    "JDK source documents are read-only", true));
        }
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
        if (sourceFor(message.payload()) != null) {
            return message.error(new WebShellError("DOCUMENT_READ_ONLY",
                    "JDK source documents are read-only", true));
        }
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
        if (!nativeUi.isAvailable()) {
            return nativeUiUnavailable(message, "File save selection is unavailable in the web runtime");
        }
        Path destination = chooseSaveTarget(session);
        if (destination == null) return message.response(Map.of("cancelled", true));
        String previousUri = MonacoModelId.forSession(session);
        if (manager.isOpenInAnotherSession(session, destination)) {
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
        String suggestedName = untitledNames.get(session.getSessionId());
        return nativeUi.chooseJavaSaveTarget(suggestedName);
    }

    private WebShellEnvelope close(WebShellEnvelope message) {
        String documentationUri = text(message.payload(), "uri");
        WebDocumentationDocument documentation = documentationDocuments.remove(documentationUri);
        if (documentation != null) {
            if (documentationHost != null) documentationHost.hide();
            surface.send(WebShellEnvelope.event("document", "closed", Map.of("uri", documentation.uri())));
            EditorSession active = manager.getCurrentSession();
            if (active != null) sendActiveChanged(active);
            return message.response(Map.of("closed", true));
        }
        String sourceUri = documentationUri;
        WebJdkSourceDocument source = jdkSourceDocuments.remove(sourceUri);
        if (source != null) {
            surface.send(WebShellEnvelope.event("document", "closed", Map.of("uri", source.uri())));
            EditorSession active = manager.getCurrentSession();
            if (active != null) sendActiveChanged(active);
            return message.response(Map.of("closed", true));
        }
        EditorSession session = sessionFor(message.payload());
        if (session == null) return message.error(new WebShellError(
                "DOCUMENT_NOT_OPEN", "The requested document is not open", true));
        diagnosticsController.invalidate(MonacoModelId.forSession(session));
        documentClosed.accept(session.getFile());
        boolean closed = manager.closeSession(session.getSessionId());
        if (!closed) return message.error(new WebShellError(
                "CLOSE_FAILED", "The document could not be closed", true));
        forgetSession(session.getSessionId());
        untitledNames.remove(session.getSessionId());
        surface.send(WebShellEnvelope.event("document", "closed", Map.of(
                "uri", MonacoModelId.forSession(session))));
        EditorSession active = manager.getCurrentSession();
        if (active != null) sendActiveChanged(active);
        return message.response(Map.of("closed", true));
    }

    EditorSession openPath(Path path) {
        EditorSession session = manager.openDocument(path.toAbsolutePath().normalize());
        observe(session);
        WebDocumentSnapshot result = snapshot(session);
        surface.send(WebShellEnvelope.event("document", "opened", result.payload()));
        sendActiveChanged(session);
        return session;
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
    }

    void sendActiveChanged(EditorSession session) {
        surface.send(WebShellEnvelope.event("document", "activeChanged", Map.of(
                "uri", MonacoModelId.forSession(session),
                "documentId", session.getDocumentId())));
    }

    private void sendActiveChanged(WebDocumentationDocument document) {
        surface.send(WebShellEnvelope.event("document", "activeChanged", Map.of(
                "uri", document.uri(), "documentId", document.uri())));
    }

    private void sendActiveChanged(WebJdkSourceDocument source) {
        surface.send(WebShellEnvelope.event("document", "activeChanged", Map.of(
                "uri", source.uri(), "documentId", source.uri())));
    }

    void openDocumentationTarget(DocumentationTarget target) {
        String uri = documentationUri(target);
        boolean existing = documentationDocuments.containsKey(uri);
        WebDocumentationDocument document = documentationDocuments.computeIfAbsent(uri,
                ignored -> new WebDocumentationDocument(uri, target));
        if (!existing) surface.send(WebShellEnvelope.event("document", "opened", document.payload()));
        documentationOpener.accept(target);
        sendActiveChanged(document);
    }

    private WebShellEnvelope documentationLayout(WebShellEnvelope message) {
        if (documentationHost != null) {
            documentationHost.layoutFromBrowser(
                    number(message.payload(), "x", 0),
                    number(message.payload(), "y", 0),
                    number(message.payload(), "width", 0),
                    number(message.payload(), "height", 0));
        }
        return message.response(Map.of("updated", true));
    }

    private static String documentationUri(DocumentationTarget target) {
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(target.url().getBytes(StandardCharsets.UTF_8));
        return "documentation://" + token;
    }

    void openJdkSource(JdkSourceTarget target) {
        String uri = target.sourceIdentity();
        WebJdkSourceDocument cached = jdkSourceDocuments.get(uri);
        String content = cached == null
                ? jdkSourceLoader.load(target).orElseThrow(() ->
                new IllegalStateException("JDK source is unavailable for " + target.displayName()))
                : cached.content();
        int offset = jdkSourceDeclarationLocator.find(content, target);
        int line = 1;
        int column = 1;
        for (int index = 0; index < offset; index++) {
            if (content.charAt(index) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        WebJdkSourceDocument source = new WebJdkSourceDocument(
                uri, target.displayName(), content, line, column);
        jdkSourceDocuments.put(uri, source);
        surface.send(WebShellEnvelope.event("document", "opened", source.payload()));
        sendActiveChanged(source);
    }

    private WebDocumentationDocument documentationFor(Map<String, Object> payload) {
        String uri = text(payload, "uri");
        return uri.isBlank() ? null : documentationDocuments.get(uri);
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

    WebDocumentSnapshot snapshot(EditorSession session) {
        String displayName = untitledNames.get(session.getSessionId());
        String name = displayName == null ? session.getDisplayName() : displayName;
        String language = languageResolver.resolve(new LanguageDocument(MonacoModelId.forSession(session),
                session.getFile(), name, null)).map(value -> value.value()).orElse("plaintext");
        return displayName == null
                ? WebDocumentSnapshot.file(session, documentFor(session), language)
                : WebDocumentSnapshot.untitled(session, documentFor(session), displayName, language);
    }

    private WebShellEnvelope nativeUiUnavailable(WebShellEnvelope message, String detail) {
        return message.error(new WebShellError("NATIVE_UI_UNAVAILABLE", detail, true));
    }

    private record WebDocumentationDocument(String uri, DocumentationTarget target) {
        private Map<String, Object> payload() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("uri", uri);
            payload.put("documentId", uri);
            payload.put("displayName", "Documentação: " + target.label());
            payload.put("language", "html");
            payload.put("content", "");
            payload.put("version", 1);
            payload.put("dirty", false);
            payload.put("readOnly", true);
            payload.put("kind", "documentation");
            payload.put("documentationUrl", target.url());
            return payload;
        }
    }

    private record WebJdkSourceDocument(String uri, String displayName, String content,
                                        int revealLine, int revealColumn) {
        private Map<String, Object> payload() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("uri", uri);
            payload.put("documentId", uri);
            payload.put("displayName", displayName);
            payload.put("language", "java");
            payload.put("content", content);
            payload.put("version", 1);
            payload.put("dirty", false);
            payload.put("readOnly", true);
            payload.put("kind", "jdk-source");
            payload.put("revealLine", revealLine);
            payload.put("revealColumn", revealColumn);
            return payload;
        }
    }

    private WebJdkSourceDocument sourceFor(Map<String, Object> payload) {
        String uri = text(payload, "uri");
        return uri.isBlank() ? null : jdkSourceDocuments.get(uri);
    }
}
