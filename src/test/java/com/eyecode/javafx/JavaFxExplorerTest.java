package com.eyecode.javafx;

import com.eyecode.designsystem.icon.EyeCodeIcon;
import com.eyecode.javafx.explorer.ExplorerContextMenu;
import com.eyecode.javafx.explorer.ExplorerIconResolver;
import com.eyecode.javafx.explorer.ExplorerRow;
import com.eyecode.javafx.explorer.ExplorerRowViewModel;
import com.eyecode.javafx.explorer.ExplorerState;
import com.eyecode.javafx.explorer.ExplorerTreeView;
import com.eyecode.javafx.explorer.JavaFxExplorer;
import com.eyecode.javafx.explorer.ProjectNode;
import com.eyecode.javafx.explorer.ProjectNodeType;
import com.eyecode.project.model.ProjectModel;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxExplorerTest {

    private static Path tempRoot;

    @BeforeAll
    static void startToolkit() throws Exception {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
        tempRoot = Files.createTempDirectory("eyecode-explorer-test");
        createFile(tempRoot.resolve("pom.xml"));
        createFile(tempRoot.resolve("index.html"));
        createFile(tempRoot.resolve("README.md"));
        createFile(tempRoot.resolve("app.css"));
        createFile(tempRoot.resolve("data.json"));
        createFile(tempRoot.resolve("config.xml"));
        createFile(tempRoot.resolve("logo.png"));
        createDir(tempRoot.resolve("src").resolve("main").resolve("java").resolve("com").resolve("demo"));
        createFile(tempRoot.resolve("src").resolve("main").resolve("java").resolve("com").resolve("demo").resolve("App.java"));
        createDir(tempRoot.resolve("src").resolve("main").resolve("resources"));
        createFile(tempRoot.resolve("src").resolve("main").resolve("resources").resolve("application.json"));
        createDir(tempRoot.resolve("mods"));
        createFile(tempRoot.resolve("mods").resolve("module-info.java"));
        createDir(tempRoot.resolve(".git"));
        createDir(tempRoot.resolve("target"));
        createDir(tempRoot.resolve("styles"));
    }

    @Test
    void explorerLoadsRootAndLazyExpands() throws Exception {
        runOnFx(explorer -> {
            TreeItem<ProjectNode> root = explorer.getTreeView().getRoot();
            assertNotNull(root);
            assertEquals(ProjectNodeType.PROJECT, root.getValue().type());
            assertTrue(root.isExpanded(), "raiz deve estar expandida");

            TreeItem<ProjectNode> src = findChild(root, "src");
            assertNotNull(src, "src ausente no primeiro nível");
            assertTrue(src.getValue().isDirectory());
            assertTrue(src.getChildren().size() == 1
                            && src.getChildren().get(0).getValue() == null,
                    "pasta não expandida deve ter apenas placeholder (lazy)");

            src.setExpanded(true);
            assertTrue(src.getChildren().size() > 0);
            assertNotNull(src.getChildren().get(0).getValue(),
                    "expansão lazy deve substituir o placeholder por nós reais");
            TreeItem<ProjectNode> main = findChild(src, "main");
            assertNotNull(main, "main ausente após expansão de src");

            TreeItem<ProjectNode> rootFile = findChild(root, "pom.xml");
            assertNotNull(rootFile, "pom.xml ausente");
            assertEquals(ProjectNodeType.FILE, rootFile.getValue().type());
            assertTrue(rootFile.getChildren().isEmpty(), "arquivo não deve ter filhos");
        });
    }

    @Test
    void ignoredDirectoriesAreHidden() throws Exception {
        runOnFx(explorer -> {
            TreeItem<ProjectNode> root = explorer.getTreeView().getRoot();
            assertNull(findChild(root, ".git"), ".git não deve aparecer");
            assertNull(findChild(root, "target"), "target não deve aparecer");
        });
    }

    @Test
    void iconsResolveFromEyeCodeIcon() {
        assertEquals(EyeCodeIcon.JAVA_FILE, iconOf("App.java", ProjectNodeType.FILE));
        assertEquals(EyeCodeIcon.HTML, iconOf("index.html", ProjectNodeType.FILE));
        assertEquals(EyeCodeIcon.CSS, iconOf("app.css", ProjectNodeType.FILE));
        assertEquals(EyeCodeIcon.JSON, iconOf("data.json", ProjectNodeType.FILE));
        assertEquals(EyeCodeIcon.XML, iconOf("config.xml", ProjectNodeType.FILE));
        assertEquals(EyeCodeIcon.MARKDOWN, iconOf("README.md", ProjectNodeType.FILE));
        assertEquals(EyeCodeIcon.IMAGE, iconOf("logo.png", ProjectNodeType.FILE));
        assertEquals(EyeCodeIcon.TEXT_FILE, iconOf("unknown.bin", ProjectNodeType.FILE));
        assertEquals(EyeCodeIcon.FOLDER, iconOf("styles", ProjectNodeType.DIRECTORY));
        assertEquals(EyeCodeIcon.PACKAGE, iconOf("com", ProjectNodeType.DIRECTORY, true));
        assertEquals(EyeCodeIcon.MODULE, iconOf("mods", ProjectNodeType.DIRECTORY));
        assertEquals(EyeCodeIcon.PROJECT_DIRECTORY, iconOf("demo", ProjectNodeType.PROJECT));
    }

    @Test
    void contextMenuHasPlaceholderStructure() {
        var menu = ExplorerContextMenu.create();
        assertNotNull(menu.getItems());
        var labels = menu.getItems().stream()
                .filter(i -> i instanceof javafx.scene.control.MenuItem)
                .map(i -> ((javafx.scene.control.MenuItem) i).getText())
                .toList();
        assertTrue(labels.containsAll(
                java.util.List.of("Open", "Rename", "Delete", "Copy Path", "Reveal", "New")));
    }

    @Test
    void setStateSwitchesInternalContentOnly() throws Exception {
        runOnFx(explorer -> {
            assertEquals(ExplorerState.PROJECT, explorer.getState());
            ExplorerTreeView tree = explorer.getTreeView();
            assertTrue(explorer.getChildren().contains(tree), "PROJECT deve mostrar a árvore");

            explorer.setState(ExplorerState.SEARCH);
            assertEquals(ExplorerState.SEARCH, explorer.getState());
            assertFalse(explorer.getChildren().contains(tree), "SEARCH não deve mostrar a árvore");

            explorer.setState(ExplorerState.LEARN);
            assertEquals(ExplorerState.LEARN, explorer.getState());
            explorer.setState(ExplorerState.ROADMAP);
            explorer.setState(ExplorerState.DOCUMENTATION);
            explorer.setState(ExplorerState.PREVIEW);
            assertEquals(ExplorerState.PREVIEW, explorer.getState());
            assertFalse(explorer.getChildren().contains(tree), "PREVIEW não deve mostrar a árvore");

            explorer.setState(ExplorerState.PROJECT);
            assertTrue(explorer.getChildren().contains(tree),
                    "voltar ao PROJECT deve restaurar a árvore");
            assertSame(tree, explorer.getTreeView());
        });
    }

    @Test
    void rowViewModelMapsTreeItemState() throws Exception {
        runFx(() -> {
            ProjectNode demo = new ProjectNode("demo", tempRoot, ProjectNodeType.PROJECT);
            TreeItem<ProjectNode> root = new TreeItem<>(demo);
            TreeItem<ProjectNode> src = new TreeItem<>(new ProjectNode("src", tempRoot.resolve("src"), ProjectNodeType.DIRECTORY));
            TreeItem<ProjectNode> main = new TreeItem<>(new ProjectNode("main", tempRoot.resolve("src").resolve("main"), ProjectNodeType.DIRECTORY));
            TreeItem<ProjectNode> java = new TreeItem<>(new ProjectNode("java", tempRoot.resolve("src").resolve("main").resolve("java"), ProjectNodeType.DIRECTORY));
            TreeItem<ProjectNode> com = new TreeItem<>(new ProjectNode("com", tempRoot.resolve("src").resolve("main").resolve("java").resolve("com"), ProjectNodeType.DIRECTORY));
            TreeItem<ProjectNode> file = new TreeItem<>(new ProjectNode("App.java", tempRoot.resolve("src").resolve("main").resolve("java").resolve("com").resolve("App.java"), ProjectNodeType.FILE));
            root.getChildren().add(src);
            src.getChildren().add(main);
            main.getChildren().add(java);
            java.getChildren().add(com);
            com.getChildren().add(file);
            src.setExpanded(true);

            ExplorerRowViewModel fileVm = ExplorerRowViewModel.from(file, true);
            assertEquals("App.java", fileVm.title());
            assertEquals(EyeCodeIcon.JAVA_FILE, fileVm.icon());
            assertEquals(5, fileVm.level());
            assertFalse(fileVm.expanded());
            assertTrue(fileVm.selected());
            assertFalse(fileVm.hasChildren());
            assertNull(fileVm.badge());
            assertNull(fileVm.status());

            ExplorerRowViewModel comVm = ExplorerRowViewModel.from(com, false);
            assertEquals(EyeCodeIcon.PACKAGE, comVm.icon(), "com sob source java deve ser pacote");
            assertEquals(4, comVm.level());
            assertTrue(comVm.hasChildren());
        });
    }

    @Test
    void rowRendersRegionsFromViewModel() throws Exception {
        runFx(() -> {
            ExplorerRow row = new ExplorerRow();
            ExplorerRowViewModel vm = new ExplorerRowViewModel(
                    "App.java", EyeCodeIcon.JAVA_FILE, 2, true, false, false, "modified", "ok");
            row.update(vm);
            assertEquals("App.java", row.getTitleRegion().getText());
            assertEquals("modified", row.getBadgeRegion().getText());
            assertTrue(row.getBadgeRegion().isVisible());
            assertEquals("ok", row.getStatusRegion().getText());
            assertTrue(row.getStatusRegion().isVisible());
            assertFalse(row.getIconRegion().getChildren().isEmpty());

            row.update(new ExplorerRowViewModel(
                    "App.java", EyeCodeIcon.JAVA_FILE, 2, true, false, false, null, null));
            assertFalse(row.getBadgeRegion().isVisible());
            assertFalse(row.getStatusRegion().isVisible());
        });
    }

    private JavaFxExplorer newExplorer() {
        ProjectModel model = ProjectModel.fromDirectory(tempRoot.toFile());
        return new JavaFxExplorer(model);
    }

    private EyeCodeIcon iconOf(String name, ProjectNodeType type) {
        return iconOf(name, type, false);
    }

    private EyeCodeIcon iconOf(String name, ProjectNodeType type, boolean underJavaSource) {
        ProjectNode node = new ProjectNode(name, tempRoot.resolve(name), type);
        return ExplorerIconResolver.forNode(node, underJavaSource);
    }

    private TreeItem<ProjectNode> findChild(TreeItem<ProjectNode> parent, String name) {
        return parent.getChildren().stream()
                .filter(item -> item.getValue() != null && item.getValue().name().equals(name))
                .findFirst()
                .orElse(null);
    }

    private void runOnFx(Consumer<JavaFxExplorer> body) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        final Throwable[] error = new Throwable[1];
        Platform.runLater(() -> {
            try {
                body.accept(newExplorer());
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(20, TimeUnit.SECONDS), "tempo esgotado no FX thread");
        if (error[0] != null) {
            throw new AssertionError("Falha no JavaFxExplorerTest", error[0]);
        }
    }

    private void runFx(Runnable body) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        final Throwable[] error = new Throwable[1];
        Platform.runLater(() -> {
            try {
                body.run();
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(20, TimeUnit.SECONDS), "tempo esgotado no FX thread");
        if (error[0] != null) {
            throw new AssertionError("Falha no teste de linha", error[0]);
        }
    }

    private static void createFile(Path path) throws Exception {
        Files.createDirectories(path.getParent());
        Files.createFile(path);
    }

    private static void createDir(Path path) throws Exception {
        Files.createDirectories(path);
    }
}
