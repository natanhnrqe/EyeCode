package com.eyecode.ui.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
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
            assertEquals(15, surface.handlers.keySet().stream().filter(key -> key.startsWith("workspace/")).count());
            assertNull(surface.call("workspace", "snapshot", Map.of()).error());
            assertEquals("NATIVE_UI_UNAVAILABLE", surface.call("workspace", "chooseDirectory", Map.of()).error().code());
            assertNull(surface.call("workspace", "removeRecent", Map.of("path", temp.toString())).error());
        }
    }

    @Test
    void createProjectRegistersAndOpensTheMavenRootWithoutJdt() throws Exception {
        Surface surface = new Surface();
        try (var runtime = WebShellWorkspaceComposition.create(surface)) {
            var created = surface.call("workspace", "createProject", Map.of(
                    "name", "Demo", "location", temp.toString(), "groupId", "example.app"));

            assertNull(created.error());
            Path root = temp.resolve("Demo");
            assertEquals(root.toString(), ((Map<?, ?>) created.payload().get("project")).get("path"));
            assertTrue(Files.isRegularFile(root.resolve("pom.xml")));
            assertTrue(Files.isRegularFile(root.resolve(".gitignore")));
            Path main = root.resolve("src/main/java/example/app/Main.java");
            assertTrue(Files.isRegularFile(main));
            assertNull(surface.call("workspace", "openFile", Map.of("path", main.toString())).error());
            assertNull(surface.call("workspace", "openProject", Map.of("path", root.toString())).error());
        }
    }

    @Test
    void nativeProjectPickerSupportsCreateOpenCancelAndInvalidSelection() throws Exception {
        Path location = Files.createDirectory(temp.resolve("picker-location"));
        Path invalid = Files.createDirectory(temp.resolve("not-a-project"));
        AtomicReference<Path> selected = new AtomicReference<>(location);
        List<String> pickerTitles = new CopyOnWriteArrayList<>();
        WebShellNativeUi nativeUi = new WebShellNativeUi() {
            @Override public boolean isAvailable() { return true; }
            @Override public CompletableFuture<Path> chooseDirectoryAsync(String title) {
                pickerTitles.add(title);
                return CompletableFuture.completedFuture(selected.get());
            }
            @Override public Path chooseJavaSaveTarget(String suggestedName) { return null; }
        };
        Surface surface = new Surface();
        try (var runtime = WebShellWorkspaceComposition.create(surface, target -> { }, nativeUi)) {
            assertNull(surface.handler("workspace", "chooseDirectory").handle(
                    WebShellEnvelope.request("workspace", "chooseDirectory", "create-browse", Map.of())));
            WebShellEnvelope createBrowse = surface.awaitResponse("create-browse");
            assertNull(createBrowse.error());
            assertEquals(location.toString(), createBrowse.payload().get("path"));
            assertEquals("Choose Project Location", pickerTitles.getFirst());

            WebShellEnvelope created = surface.call("workspace", "createProject", Map.of(
                    "name", "Picked", "location", createBrowse.payload().get("path"), "groupId", "example.app"));
            assertNull(created.error());
            Path projectRoot = location.resolve("Picked");
            assertTrue(Files.isRegularFile(projectRoot.resolve("pom.xml")));

            selected.set(projectRoot);
            assertNull(surface.handler("workspace", "openProject").handle(
                    WebShellEnvelope.request("workspace", "openProject", "open-browse", Map.of())));
            WebShellEnvelope opened = surface.awaitResponse("open-browse");
            assertNull(opened.error());
            assertEquals(projectRoot.toString(), ((Map<?, ?>) opened.payload().get("project")).get("path"));
            assertEquals("Open Project", pickerTitles.get(1));

            selected.set(null);
            assertNull(surface.handler("workspace", "chooseDirectory").handle(
                    WebShellEnvelope.request("workspace", "chooseDirectory", "create-cancel", Map.of())));
            assertEquals(Boolean.TRUE, surface.awaitResponse("create-cancel").payload().get("cancelled"));
            assertNull(surface.handler("workspace", "openProject").handle(
                    WebShellEnvelope.request("workspace", "openProject", "open-cancel", Map.of())));
            assertEquals(Boolean.TRUE, surface.awaitResponse("open-cancel").payload().get("cancelled"));

            selected.set(invalid);
            assertNull(surface.handler("workspace", "openProject").handle(
                    WebShellEnvelope.request("workspace", "openProject", "open-invalid", Map.of())));
            assertEquals("INVALID_PROJECT", surface.awaitResponse("open-invalid").error().code());
            Map<?, ?> current = (Map<?, ?>) surface.call("workspace", "snapshot", Map.of()).payload().get("project");
            assertEquals(projectRoot.toString(), current.get("path"));
            assertEquals(List.of("Choose Project Location", "Open Project", "Choose Project Location",
                    "Open Project", "Open Project"), pickerTitles);
        }
    }

    @Test
    void startupProjectUsesTheWorkspaceOpenFlowAndBecomesActive() throws Exception {
        Path root = Files.createDirectory(temp.resolve("external-project"));
        Files.writeString(root.resolve("pom.xml"), "<project/>");
        Surface surface = new Surface();
        try (var runtime = WebShellWorkspaceComposition.create(surface)) {
            runtime.openProjectAtStartup(root);

            Map<?, ?> project = (Map<?, ?>) surface.call("workspace", "snapshot", Map.of())
                    .payload().get("project");
            assertEquals(root.toAbsolutePath().normalize().toString(), project.get("path"));
            assertTrue(surface.events.stream().anyMatch(event -> event.name().equals("changed")
                    && "workspace".equals(event.channel())));
        }
    }

    @Test
    void openEditConflictSaveRenameAndDeletePreservePayloadsAndEvents() throws Exception {
        Path root = Files.createDirectory(temp.resolve("project"));
        Files.writeString(root.resolve("pom.xml"), "<project/>");
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
        Files.writeString(root.resolve("pom.xml"), "<project/>");
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
            assertEquals(2, entries.size());
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

        WebShellMessageHandler handler(String channel, String name) {
            return handlers.get(channel + "/" + name);
        }

        WebShellEnvelope awaitResponse(String requestId) throws InterruptedException {
            for (int attempt = 0; attempt < 200; attempt++) {
                WebShellEnvelope response = events.stream()
                        .filter(event -> event.kind() == WebShellEnvelope.Kind.RESPONSE)
                        .filter(event -> requestId.equals(event.requestId()))
                        .findFirst().orElse(null);
                if (response != null) return response;
                Thread.sleep(10);
            }
            fail("No response for " + requestId);
            return null;
        }
    }
}
