package com.eyecode.javafx.ui.editor;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.eventbus.EventBus;
import com.eyecode.filesystem.DefaultFileSystemService;
import com.eyecode.project.ProjectInfo;
import com.eyecode.project.ProjectType;
import com.eyecode.workbench.editor.EditorManager;
import com.eyecode.workbench.editor.EditorSession;
import com.eyecode.workbench.editor.EditorView;
import com.eyecode.workbench.editor.EditorViewFactory;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxEditorWorkspaceEmptyStateTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void welcomeIsMountedWhenThereAreNoTabsAndReturnsAfterLastTabCloses() throws Exception {
        runInFx(() -> {
            EditorManager manager = manager();
            FxEditorWorkspacePane pane = new FxEditorWorkspacePane(
                    manager, new JavaFxDocumentationWorkspace(), new JavaFxJdkSourceWorkspace());

            assertSame(pane.welcomeSurfaceForTest(), pane.contentPaneForTest().mountedContentForTest());

            EditorSession session = manager.openDocument(Path.of("Main.java"), "class Main {}");
            assertSame(manager.getNativeView(session.getSessionId()),
                    pane.contentPaneForTest().mountedContentForTest());

            manager.closeSession(session.getSessionId());
            assertSame(pane.welcomeSurfaceForTest(), pane.contentPaneForTest().mountedContentForTest());
        });
    }

    @Test
    void welcomeActionsUseSuppliedProjectCallbacksAndNewProjectBackReturnsHome() throws Exception {
        runInFx(() -> {
            AtomicReference<String> action = new AtomicReference<>();
            AtomicReference<ProjectInfo> recent = new AtomicReference<>();
            ProjectInfo info = new ProjectInfo("Demo", "/tmp/Demo", ProjectType.JAVA);
            WelcomeProjectSurface welcome = new WelcomeProjectSurface(
                    () -> action.set("new"),
                    () -> action.set("open"),
                    () -> List.of(info),
                    recent::set);
            welcome.refreshRecentProjects();
            assertEquals(1, welcome.recentProjectsForTest().getChildren().size());

            ((javafx.scene.control.Button) welcome.recentProjectsForTest().getChildren().getFirst()).fire();
            assertSame(info, recent.get());

            NewProjectSurface newProject = new NewProjectSurface(() -> action.set("back"));
            assertEquals(List.of("Java", "Maven", "Gradle", "Spring Boot"),
                    newProject.optionTitlesForTest());
            assertEquals("TYPE", newProject.stepForTest());
            newProject.nextForTest().fire();
            assertEquals("CONFIGURATION", newProject.stepForTest());
            newProject.nextForTest().fire();
            assertEquals("PREVIEW", newProject.stepForTest());
            assertTrue(newProject.backForTest().getStyleClass().contains("eyecode-button"));
            assertTrue(newProject.nextForTest().getStyleClass().contains("eyecode-button"));
            assertTrue(newProject.createForTest().getStyleClass().contains("eyecode-button-primary"));
            newProject.backForTest().fire();
            assertEquals("CONFIGURATION", newProject.stepForTest());
            newProject.backForTest().fire();
            assertEquals("TYPE", newProject.stepForTest());
            newProject.backForTest().fire();
            assertEquals("back", action.get());
        });
    }

    @Test
    void recentProjectsUseBoundedScrollableViewport() throws Exception {
        runInFx(() -> {
            ProjectInfo first = new ProjectInfo("First", "/tmp/First", ProjectType.JAVA);
            WelcomeProjectSurface welcome = new WelcomeProjectSurface(
                    () -> { }, () -> { }, () -> List.of(first, first, first, first, first, first, first, first), project -> { });
            ScrollPane viewport = welcome.recentViewportForTest();

            assertEquals(340, viewport.getMaxHeight(), 0.01);
            assertTrue(viewport.isFitToWidth());
            assertEquals(8, welcome.recentProjectsForTest().getChildren().size());
        });
    }

    private static EditorManager manager() {
        return new EditorManager(new EventBus(), new DefaultFileSystemService(), new EditorViewFactory() {
            @Override
            public EditorView create(EditorBuffer buffer) {
                return new EditorView() {
                    private final Node node = new Label(buffer.getDocument().getText());

                    @Override
                    public Object getNativeView() {
                        return node;
                    }

                    @Override
                    public void refreshFromDocument() {
                    }

                    @Override
                    public void dispose() {
                    }
                };
            }

            @Override
            public boolean supports(Path file) {
                return true;
            }

            @Override
            public String id() {
                return "empty-state-test";
            }
        });
    }

    private static void runInFx(ThrowingRunnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(10, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
