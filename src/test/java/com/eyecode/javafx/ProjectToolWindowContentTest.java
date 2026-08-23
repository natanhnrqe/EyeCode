package com.eyecode.javafx;

import com.eyecode.javafx.ui.toolwindow.content.ProjectToolWindowContent;
import com.eyecode.project.ProjectInfo;
import com.eyecode.project.ProjectType;
import com.eyecode.project.model.ProjectModel;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProjectToolWindowContentTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @TempDir
    Path tempDir;

    @Test
    void emptyProjectSurfaceShowsRecentProjectsAndProjectSurfaceCanBeReplaced() throws Exception {
        Path project = Files.createDirectory(tempDir.resolve("project"));
        Files.writeString(project.resolve("Main.java"), "class Main {}");
        AtomicReference<Path> opened = new AtomicReference<>();
        AtomicReference<ProjectInfo> recent = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                ProjectToolWindowContent content = new ProjectToolWindowContent(null, opened::set);
                content.setRecentProjects(List.of(new ProjectInfo(
                        "project", project.toString(), ProjectType.JAVA)), recent::set);
                assertNull(content.getExplorer());
                assertNotNull(content.getChildren());

                content.setProject(ProjectModel.fromDirectory(project.toFile()));
                assertNotNull(content.getExplorer());
                assertEquals("project", content.getExplorer().getTreeView().getRoot().getValue().name());
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                done.countDown();
            }
        });

        assertEquals(true, done.await(20, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }
}
