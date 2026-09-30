package com.eyecode.ui.web;

import com.eyecode.eventbus.EventBus;
import com.eyecode.filesystem.DefaultFileSystemService;
import com.eyecode.language.java.lsp.JdtLsProjectService;
import com.eyecode.project.ProjectFileOperationService;
import com.eyecode.project.ProjectLifecycleService;
import com.eyecode.project.ProjectRenameService;
import com.eyecode.workbench.editor.EditorIntelligence;
import com.eyecode.workbench.editor.EditorManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class WebShellRefactorControllerTest {
    @TempDir Path temporary;

    private EditorManager manager;
    private ProjectLifecycleService projects;
    private JdtLsProjectService jdt;
    private WebShellRefactorController controller;
    private Surface surface;

    @BeforeEach
    void setUp() throws Exception {
        manager = new EditorManager(new EventBus(), new DefaultFileSystemService(), new WebShellEditorViewFactory(),
                Runnable::run, new ProjectFileOperationService(), new NoOpEditorIntelligence());
        projects = new ProjectLifecycleService();
        jdt = new JdtLsProjectService(projects);
        surface = new Surface();
        controller = new WebShellRefactorController(surface, manager, projects, jdt,
                new ProjectRenameService(new ProjectFileOperationService()));
        Path file = Files.write(temporary.resolve("Main.java"), "class Main { int foo = 0; }\n".getBytes());
        projects.open(temporary);
        manager.openDocument(file);
    }

    @AfterEach
    void tearDown() {
        controller.dispose();
        jdt.close();
        manager.dispose();
        projects.close();
    }

    @Test
    void prepareRenameWithoutJdtIsUnsupported() {
        WebShellEnvelope response = surface.handler("refactor", "prepareRename").handle(
                WebShellEnvelope.request("refactor", "prepareRename", "p1", Map.of(
                        "uri", fileUri(), "line", 1, "column", 13)));
        Map<String, Object> payload = response.payload();
        assertEquals(false, payload.get("supported"));
    }

    @Test
    void renameWithoutJdtReturnsUnsuccessfulStructuredPayload() {
        WebShellEnvelope response = surface.handler("refactor", "rename").handle(
                WebShellEnvelope.request("refactor", "rename", "r1", Map.of(
                        "uri", fileUri(), "line", 1, "column", 13, "newName", "bar")));
        Map<String, Object> payload = response.payload();
        assertEquals(false, payload.get("success"));
        assertEquals(0, payload.get("applied"));
        assertEquals(List.of(), payload.get("failedFiles"));
    }

    @Test
    void renameRejectsMalformedIdentifierBeforeTouchingFiles() {
        WebShellEnvelope response = surface.handler("refactor", "rename").handle(
                WebShellEnvelope.request("refactor", "rename", "r2", Map.of(
                        "uri", fileUri(), "line", 1, "column", 13, "newName", "9bad name")));
        assertNotNull(response.error());
        assertEquals("INVALID_IDENTIFIER", response.error().code());
        try {
            assertTrue(Files.readString(temporary.resolve("Main.java")).contains("foo = 0"));
        } catch (java.io.IOException exception) {
            fail(exception);
        }
    }

    private String fileUri() {
        return temporary.resolve("Main.java").toUri().toString();
    }

    private static final class NoOpEditorIntelligence implements EditorIntelligence {
        @Override public void activated(com.eyecode.editor.intelligence.document.DocumentSnapshot document) { }
        @Override public void deactivated(com.eyecode.editor.intelligence.document.DocumentSnapshot document) { }
        @Override public void closed(com.eyecode.editor.intelligence.document.DocumentSnapshot document) { }
        @Override public java.util.Optional<com.eyecode.language.semantic.DefinitionLocation> resolveDefinition(
                com.eyecode.editor.intelligence.document.DocumentSnapshot document, int offset) {
            return java.util.Optional.empty();
        }
        @Override public void close() { }
    }

    private static final class Surface implements WebShellSurface {
        private final Map<String, WebShellMessageHandler> handlers = new ConcurrentHashMap<>();
        private final List<WebShellEnvelope> sent = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void send(WebShellEnvelope message) {
            sent.add(message);
        }

        @Override
        public void registerHandler(String channel, String name, WebShellMessageHandler handler) {
            handlers.put(channel + "\0" + name, handler);
        }

        private WebShellMessageHandler handler(String channel, String name) {
            return handlers.get(channel + "\0" + name);
        }
    }
}
