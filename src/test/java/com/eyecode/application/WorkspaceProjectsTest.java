package com.eyecode.application;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.eventbus.EventBus;
import com.eyecode.filesystem.DefaultFileSystemService;
import com.eyecode.project.*;
import com.eyecode.runtime.RunService;
import com.eyecode.workbench.editor.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceProjectsTest {
    @TempDir Path temp;
    private EditorManager editor;
    private ProjectLifecycleService lifecycle;
    private RunService execution;
    private WorkspaceProjects projects;

    @BeforeEach
    void setUp() {
        EditorViewFactory views = new EditorViewFactory() {
            public EditorView create(EditorBuffer buffer) {
                return new EditorView() {
                    public Object getNativeView() { return null; }
                    public void refreshFromDocument() {}
                    public void dispose() {}
                };
            }
            public boolean supports(Path path) { return true; }
            public String id() { return "headless-test"; }
        };
        editor = new EditorManager(new EventBus(), new DefaultFileSystemService(), views);
        lifecycle = new ProjectLifecycleService(new ProjectService(temp.resolve("recent.dat")));
        execution = new RunService(lifecycle);
        projects = new WorkspaceProjects(lifecycle, editor, execution, new MavenProjectCreationService());
    }

    @AfterEach
    void close() {
        editor.dispose();
        execution.dispose();
        lifecycle.close();
    }

    @Test
    void openingTransitionsSessionsAndRecordsRecentProject() throws Exception {
        Path first = Files.createDirectory(temp.resolve("first"));
        Files.writeString(first.resolve("pom.xml"), "<project/>");
        projects.open(first);
        Path source = Files.writeString(first.resolve("Main.java"), "class Main {}");
        var session = editor.openDocument(source);
        var document = editor.getBuffer(session.getSessionId()).orElseThrow().getDocument();
        document.setText("class Main { int value; }");
        Path second = Files.createDirectory(temp.resolve("second"));
        Files.writeString(second.resolve("pom.xml"), "<project><modelVersion>4.0.0</modelVersion></project>");
        var opened = projects.open(second);
        assertSame(opened, projects.current());
        assertSame(opened, lifecycle.currentProject());
        assertTrue(editor.getSessions().isEmpty());
        assertEquals("class Main { int value; }", Files.readString(source));
        assertTrue(projects.recent().isEmpty());
    }

    @Test
    void creationUsesMavenServiceAndOpensTheCreatedProject() throws Exception {
        var project = projects.create(new MavenProjectCreationService.CreationRequest("Demo", temp.toString(), "example.app"));
        assertEquals(temp.resolve("Demo"), project.getRootDir());
        assertSame(project, projects.requireCurrent());
        assertTrue(Files.isRegularFile(project.getRootDir().resolve("pom.xml")));
        assertEquals("example.app.Main", projects.selectedMainClass(project).orElseThrow());
    }

    @Test
    void failedOpenOrCreationDoesNotReplaceCurrentProject() throws Exception {
        Path originalRoot = Files.createDirectory(temp.resolve("original"));
        Files.writeString(originalRoot.resolve("pom.xml"), "<project/>");
        var original = projects.open(originalRoot);
        assertThrows(IllegalArgumentException.class, () -> projects.open(temp.resolve("missing")));
        assertSame(original, projects.current());
        assertThrows(IllegalArgumentException.class, () -> projects.create(
                new MavenProjectCreationService.CreationRequest("../bad", temp.toString(), "example")));
        assertSame(original, projects.current());
    }

    @Test
    void rejectsAnUnmarkedDirectoryWithoutReplacingTheCurrentProject() throws Exception {
        Path originalRoot = Files.createDirectory(temp.resolve("original-project"));
        Files.writeString(originalRoot.resolve("pom.xml"), "<project/>");
        var original = projects.open(originalRoot);
        Path arbitrary = Files.createDirectory(temp.resolve("arbitrary-folder"));

        assertThrows(IllegalArgumentException.class, () -> projects.open(arbitrary));
        assertSame(original, projects.current());
    }

    @Test
    void opensNonemptyWorkspaceEvenWhenProjectDetectorDoesNotRecognizeIt() throws Exception {
        Path root = Files.createDirectory(temp.resolve("unmarked-workspace"));
        Files.writeString(root.resolve("README.md"), "workspace");

        var opened = projects.open(root);

        assertSame(opened, projects.current());
        assertEquals(root.toAbsolutePath().normalize(), opened.getRootDir());
    }

    @Test
    void renameFlushesAndRebindsWhileDeleteRejectsDirtySessions() throws Exception {
        Path projectRoot = Files.createDirectory(temp.resolve("project"));
        Files.writeString(projectRoot.resolve("pom.xml"), "<project/>");
        var project = projects.open(projectRoot);
        Path source = Files.writeString(project.getRootDir().resolve("note.txt"), "old");
        var session = editor.openDocument(source);
        var document = editor.getBuffer(session.getSessionId()).orElseThrow().getDocument();
        document.setText("changed");
        assertEquals(EditorManager.PathMutationResult.DIRTY_DOCUMENTS, editor.deletePathSafely(project, source));
        assertTrue(Files.exists(source));
        assertEquals(EditorManager.PathMutationResult.SUCCESS, editor.renamePathSafely(project, source, "renamed.txt"));
        assertEquals(source.resolveSibling("renamed.txt"), session.getFile());
        assertEquals("changed", Files.readString(session.getFile()));
        assertFalse(document.isDirty());
        assertEquals(EditorManager.PathMutationResult.SUCCESS, editor.deletePathSafely(project, session.getFile()));
        assertTrue(editor.getSessions().isEmpty());
    }

    @Test
    void currentProjectRequiredAndSaveDestinationCollisionIsDetected() throws Exception {
        assertThrows(IllegalArgumentException.class, projects::requireCurrent);
        Path source = Files.writeString(temp.resolve("note.txt"), "old");
        var existing = editor.openDocument(source);
        var untitled = editor.openDocument(null, "new");
        assertTrue(editor.isOpenInAnotherSession(untitled, source));
        assertFalse(editor.isOpenInAnotherSession(existing, source));
        assertTrue(editor.isExistingFile(source));
        assertFalse(editor.isExistingFile(temp));
    }
}
