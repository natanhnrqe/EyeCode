package com.eyecode.javafx;

import com.eyecode.javafx.explorer.ExplorerTreeView;
import com.eyecode.javafx.explorer.JavaFxExplorer;
import com.eyecode.javafx.explorer.ProjectNode;
import com.eyecode.javafx.explorer.ProjectNodeType;
import com.eyecode.javafx.ui.FxRootLayout;
import com.eyecode.javafx.ui.toolwindow.content.DependenciesToolWindowContent;
import com.eyecode.javafx.ui.toolwindow.content.DocumentationToolWindowContent;
import com.eyecode.javafx.ui.toolwindow.content.GenericToolWindowContent;
import com.eyecode.javafx.ui.toolwindow.content.LearnToolWindowContent;
import com.eyecode.javafx.ui.toolwindow.content.PreviewToolWindowContent;
import com.eyecode.javafx.ui.toolwindow.content.ProjectToolWindowContent;
import com.eyecode.javafx.ui.toolwindow.content.RoadmapToolWindowContent;
import com.eyecode.javafx.ui.toolwindow.content.SearchToolWindowContent;
import com.eyecode.javafx.ui.toolwindow.content.SettingsToolWindowContent;
import com.eyecode.javafx.ui.toolwindow.content.WorkspaceContentFactory;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Sprint33IntegrationTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void workspaceContentFactoryCachesAndProvidesRealViews() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        final Throwable[] error = new Throwable[1];

        Platform.runLater(() -> {
            try {
                WorkspaceContentFactory factory = new WorkspaceContentFactory();

                assertTrue(factory.supports("project"), "project ausente");
                assertTrue(factory.supports("learn"), "learn ausente");
                assertTrue(factory.supports("roadmap"), "roadmap ausente");
                assertTrue(factory.supports("documentation"), "documentation ausente");
                assertTrue(factory.supports("search"), "search ausente");
                assertTrue(factory.supports("preview"), "preview ausente");
                assertTrue(factory.supports("dependencies"), "dependencies ausente");
                assertTrue(factory.supports("settings"), "settings ausente");
                assertTrue(factory.supports("terminal"), "terminal ausente");

                assertSame(factory.createContent("project"),
                        factory.createContent("project"), "cache não reutiliza Project");
                assertSame(factory.createContent("learn"),
                        factory.createContent("learn"), "cache não reutiliza Learn");
                assertSame(factory.createContent("project"), factory.cached("project"),
                        "cached() deve devolver o mesmo nó");

                Node project = factory.createContent("project");
                assertTrue(project instanceof ProjectToolWindowContent,
                        "Project deve usar o Explorer JavaFX nativo");
                assertTrue(hasNativeExplorer((ProjectToolWindowContent) project),
                        "Project deve conter o JavaFxExplorer nativo");
                assertTrue(!hasSwingNode((ProjectToolWindowContent) project),
                        "Project não deve usar SwingNode");
                ExplorerTreeView treeView = nativeTreeView((ProjectToolWindowContent) project);
                assertNotNull(treeView.getRoot(), "árvore do Project sem raiz");
                assertNotNull(treeView.getRoot().getValue(), "raiz do Project sem valor");
                assertTrue(treeView.getRoot().isExpanded(), "raiz do Project deve vir expandida");
                assertTrue(treeView.getRoot().getChildren().size() > 0,
                        "raiz do Project deve carregar o primeiro nível");

                assertTrue(factory.createContent("learn") instanceof LearnToolWindowContent);
                assertTrue(factory.createContent("roadmap") instanceof RoadmapToolWindowContent);
                assertTrue(factory.createContent("documentation") instanceof DocumentationToolWindowContent);
                assertTrue(factory.createContent("search") instanceof SearchToolWindowContent);
                assertTrue(factory.createContent("preview") instanceof PreviewToolWindowContent);
                assertTrue(factory.createContent("dependencies") instanceof DependenciesToolWindowContent);
                assertTrue(factory.createContent("settings") instanceof SettingsToolWindowContent);
                assertTrue(factory.createContent("terminal") instanceof GenericToolWindowContent);
                assertTrue(factory.createContent("extensions") instanceof GenericToolWindowContent);
                assertTrue(factory.createContent("profile") instanceof GenericToolWindowContent);
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                done.countDown();
            }
        });

        assertTrue(done.await(30, TimeUnit.SECONDS), "tempo esgotado no FX thread");
        if (error[0] != null) {
            throw new AssertionError("Falha na Sprint 3.3 (factory)", error[0]);
        }
    }

    @Test
    void rootLayoutSwitchesWithoutRecreatingContent() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        final Throwable[] error = new Throwable[1];
        final StringBuilder report = new StringBuilder();

        ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
        PrintStream oldErr = System.err;
        System.setErr(new PrintStream(errBuf, true, StandardCharsets.UTF_8));

        Platform.runLater(() -> {
            try {
                FxRootLayout root = new FxRootLayout(() -> {});
                Scene scene = new Scene(root, 1200, 800);
                scene.getStylesheets().add(
                        getClass().getResource("/javafx/style/eyecode.css").toExternalForm());
                root.applyCss();
                root.layout();

                Node project1 = root.getLeftToolWindow().getCurrentContent();
                assertNotNull(project1, "conteúdo inicial ausente");
                assertTrue(project1 instanceof ProjectToolWindowContent,
                        "Project ToolWindow deve usar o Explorer JavaFX nativo");

                ExplorerTreeView treeView = nativeTreeView((ProjectToolWindowContent) project1);
                TreeItem<ProjectNode> directory = firstDirectory(treeView.getRoot());
                if (directory != null) {
                    TreeItem<ProjectNode> before = directory.getChildren().isEmpty()
                            ? null : directory.getChildren().get(0);
                    directory.setExpanded(true);
                    assertTrue(directory.getChildren().size() > 0,
                            "expansão lazy deve carregar os filhos da pasta");
                    if (before == null || before.getValue() == null) {
                        assertNotNull(directory.getChildren().get(0).getValue(),
                                "expansão lazy deve substituir o placeholder por nós reais");
                    }
                }

                root.getToolWindowManager().activate("learn");
                Node learn1 = root.getLeftToolWindow().getCurrentContent();
                assertTrue(learn1 instanceof LearnToolWindowContent);

                root.getToolWindowManager().activate("project");
                assertSame(project1, root.getLeftToolWindow().getCurrentContent(),
                        "trocar de volta não deve recriar o Project");

                root.getToolWindowManager().activate("learn");
                assertSame(learn1, root.getLeftToolWindow().getCurrentContent(),
                        "trocar de volta não deve recriar o Learn");
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                System.setErr(oldErr);
                report.append(errBuf.toString(StandardCharsets.UTF_8));
                done.countDown();
            }
        });

        assertTrue(done.await(30, TimeUnit.SECONDS), "tempo esgotado no FX thread");
        String err = report.toString().toLowerCase();
        assertTrue(!err.contains("css error") && !err.contains("error parsing"),
                "Erros de parse de CSS:\n" + report);
        if (error[0] != null) {
            throw new AssertionError("Falha na Sprint 3.3 (layout)", error[0]);
        }
    }

    private boolean hasNativeExplorer(ProjectToolWindowContent content) {
        return nativeTreeView(content) != null;
    }

    private boolean hasSwingNode(ProjectToolWindowContent content) {
        return content.getChildren().stream().anyMatch(n -> n.getClass().getName().contains("SwingNode"));
    }

    private ExplorerTreeView nativeTreeView(ProjectToolWindowContent content) {
        return content.getChildren().stream()
                .filter(n -> n instanceof JavaFxExplorer)
                .map(n -> ((JavaFxExplorer) n).getTreeView())
                .findFirst()
                .orElse(null);
    }

    private TreeItem<ProjectNode> firstDirectory(TreeItem<ProjectNode> parent) {
        return parent.getChildren().stream()
                .filter(item -> item.getValue() != null
                        && item.getValue().type() == ProjectNodeType.DIRECTORY)
                .findFirst()
                .orElse(null);
    }
}
