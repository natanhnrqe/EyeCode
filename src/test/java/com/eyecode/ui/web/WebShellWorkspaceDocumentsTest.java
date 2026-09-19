package com.eyecode.ui.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import com.eyecode.diagnostics.JavaDiagnosticsProvider;
import com.eyecode.language.DocumentLanguageResolver;
import com.eyecode.language.ExtensionDocumentLanguageResolver;
import com.eyecode.language.LanguageId;
import com.eyecode.language.diagnostics.DiagnosticsService;
import static org.junit.jupiter.api.Assertions.*;

class WebShellWorkspaceDocumentsTest {
    @TempDir Path temp;

    @Test
    void compositionRegistersTheSameWorkspaceAndDocumentContractsOnce() {
        Surface surface = new Surface();
        try (var runtime = WebShellWorkspaceComposition.create(surface)) {
            assertEquals(7, surface.handlers.keySet().stream().filter(key -> key.startsWith("document/")).count());
            assertEquals(14, surface.handlers.keySet().stream().filter(key -> key.startsWith("workspace/")).count());
            assertNull(surface.call("workspace", "snapshot", Map.of()).error());
            assertEquals("NATIVE_UI_UNAVAILABLE", surface.call("workspace", "chooseDirectory", Map.of()).error().code());
        }
    }

    @Test
    void openEditConflictSaveRenameAndDeletePreservePayloadsAndEvents() throws Exception {
        Path root = Files.createDirectory(temp.resolve("project"));
        Path file = Files.writeString(root.resolve("note.txt"), "initial");
        Surface surface = new Surface();
        try (var runtime = WebShellWorkspaceComposition.create(surface)) {
            assertNull(surface.call("workspace", "openProject", Map.of("path", root.toString())).error());
            Map<String, Object> opened = document(surface.call("workspace", "openFile", Map.of("path", file.toString())));
            String uri = opened.get("uri").toString();
            var changed = surface.call("document", "change", Map.of("uri", uri, "version", opened.get("version"), "content", "edited"));
            assertNull(changed.error());
            assertEquals("edited", document(changed).get("content"));
            assertEquals("DOCUMENT_VERSION_CONFLICT", surface.call("document", "change", Map.of(
                    "uri", uri, "version", opened.get("version"), "content", "stale")).error().code());
            assertNull(surface.call("document", "save", Map.of("uri", uri)).error());
            assertEquals("edited", Files.readString(file));
            assertNull(surface.call("workspace", "rename", Map.of("target", file.toString(), "name", "renamed.txt")).error());
            assertTrue(surface.events.stream().anyMatch(event -> event.name().equals("reidentified")
                    && uri.equals(event.payload().get("previousUri"))));
            Path renamed = root.resolve("renamed.txt");
            assertEquals("edited", Files.readString(renamed));
            assertNull(surface.call("workspace", "delete", Map.of("target", renamed.toString())).error());
            assertFalse(Files.exists(renamed));
            assertTrue(surface.events.stream().anyMatch(event -> event.name().equals("closed")
                    && renamed.toUri().toString().equals(event.payload().get("uri"))));
        }
    }

    @Test
    void explorerAndMutationResultsKeepWebShapeAndErrorCodes() throws Exception {
        Path root = Files.createDirectory(temp.resolve("project"));
        Files.createDirectory(root.resolve("target"));
        Surface surface = new Surface();
        try (var runtime = WebShellWorkspaceComposition.create(surface)) {
            surface.call("workspace", "openProject", Map.of("path", root.toString()));
            var created = surface.call("workspace", "createDirectory", Map.of("target", root.toString(), "name", "src"));
            assertNull(created.error());
            assertEquals(false, created.payload().get("openFile"));
            assertEquals(List.of(), created.payload().get("ancestors"));
            var children = surface.call("workspace", "children", Map.of("path", root.toString()));
            assertEquals(root.toString(), children.payload().get("parent"));
            List<?> entries = (List<?>) children.payload().get("children");
            assertEquals(1, entries.size());
            assertEquals(Map.of("name", "src", "path", root.resolve("src").toString(),
                    "kind", "directory", "hasChildren", false), entries.getFirst());
            assertEquals("INVALID_TREE_PATH", surface.call("workspace", "children", Map.of("path", temp.toString())).error().code());
            assertEquals("CREATE_JAVA_CLASS", surface.call("workspace", "createJavaClass",
                    Map.of("target", root.toString(), "name", "not valid")).error().code());
            assertEquals(List.of(root.toString()), surface.call("workspace", "refresh",
                    Map.of("paths", List.of(root.resolve("missing").toString()))).payload().get("validPaths"));
        }
    }

    @Test
    void closingAndResettingDocumentsDetachObservationListeners() {
        var manager = new com.eyecode.workbench.editor.EditorManager(new com.eyecode.eventbus.EventBus(),
                new com.eyecode.filesystem.DefaultFileSystemService(), new WebShellEditorViewFactory());
        Surface surface = new Surface();
        DocumentLanguageResolver languageResolver = languageResolver();
        var diagnostics = new WebShellDiagnosticsController(surface, diagnostics(languageResolver));
        var documents = new WebShellDocumentController(surface, target -> {}, null,
                WebShellNativeUi.unavailable(), manager, diagnostics, languageResolver);
        try {
            var first = document(surface.call("document", "new", Map.of()));
            var firstModel = manager.getBuffer(manager.getCurrentSession().getSessionId()).orElseThrow().getDocument();
            surface.call("document", "close", Map.of("uri", first.get("uri")));
            surface.events.clear();
            firstModel.setText("detached");
            assertTrue(surface.events.isEmpty());
            surface.call("document", "new", Map.of());
            var secondModel = manager.getBuffer(manager.getCurrentSession().getSessionId()).orElseThrow().getDocument();
            documents.reset();
            surface.events.clear();
            secondModel.setText("reset");
            assertTrue(surface.events.isEmpty());
            documents.dispose();
            documents.dispose();
        } finally {
            documents.dispose();
            diagnostics.dispose();
            manager.dispose();
        }
    }

    private static DocumentLanguageResolver languageResolver() {
        return new ExtensionDocumentLanguageResolver(Map.of(LanguageId.JAVA, java.util.Set.of("java")));
    }

    private static DiagnosticsService diagnostics(DocumentLanguageResolver languageResolver) {
        return new DiagnosticsService(languageResolver, List.of(new JavaDiagnosticsProvider()));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> document(WebShellEnvelope response) {
        assertNull(response.error());
        return (Map<String, Object>) response.payload().get("document");
    }

    private static final class Surface implements WebShellSurface {
        private final Map<String, WebShellMessageHandler> handlers = new ConcurrentHashMap<>();
        private final List<WebShellEnvelope> events = new CopyOnWriteArrayList<>();
        public void registerHandler(String channel, String name, WebShellMessageHandler handler) {
            assertNull(handlers.put(channel + "/" + name, handler));
        }
        public void send(WebShellEnvelope event) { events.add(event); }
        WebShellEnvelope call(String channel, String name, Map<String, Object> payload) {
            var response = handlers.get(channel + "/" + name).handle(WebShellEnvelope.request(channel, name, "test", payload));
            assertEquals("eyecode.web/1", response.protocol());
            assertEquals("test", response.requestId());
            return response;
        }
    }
}
