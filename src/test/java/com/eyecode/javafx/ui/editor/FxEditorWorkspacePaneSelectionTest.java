package com.eyecode.javafx.ui.editor;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.eventbus.EventBus;
import com.eyecode.filesystem.DefaultFileSystemService;
import com.eyecode.language.documentation.JdkSourceLoader;
import com.eyecode.language.documentation.JdkSourceTarget;
import com.eyecode.learning.content.DocumentationTarget;
import com.eyecode.workbench.editor.EditorManager;
import com.eyecode.workbench.editor.EditorSession;
import com.eyecode.workbench.editor.EditorView;
import com.eyecode.workbench.editor.EditorViewFactory;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxEditorWorkspacePaneSelectionTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void selectingAnAlreadyActiveProjectTabRemountsItsOwnNodeAfterSourceTab() throws Exception {
        Path zip = sourceZip();
        try {
            runInFx(() -> {
                EditorManager manager = manager();
                EditorSession shape = manager.openDocument(Path.of("Shape.java"), "class Shape {}");
                EditorSession animal = manager.openDocument(Path.of("Animal.java"), "class Animal {}");
                manager.activateSession(shape.getSessionId());
                JavaFxDocumentationWorkspace docs = documentationWorkspace();
                JavaFxJdkSourceWorkspace sources = new JavaFxJdkSourceWorkspace(new JdkSourceLoader(zip));
                FxEditorWorkspacePane pane = new FxEditorWorkspacePane(manager, docs, sources);
                JdkSourceTarget math = target("java.lang.Math", "Math.java");
                JdkSourceTarget string = target("java.lang.String", "String.java");

                sources.open(math);
                assertEquals(math.tabId(), pane.tabsForTest().selectedTabId());
                assertTrue(pane.contentPaneForTest().mountedContentForTest() instanceof Label);

                select(pane.tabsForTest(), shape.getSessionId());
                assertEquals(shape.getSessionId(), pane.tabsForTest().selectedTabId());
                assertSame(manager.getNativeView(shape.getSessionId()), pane.contentPaneForTest().mountedContentForTest());

                select(pane.tabsForTest(), animal.getSessionId());
                assertSame(manager.getNativeView(animal.getSessionId()), pane.contentPaneForTest().mountedContentForTest());
                select(pane.tabsForTest(), shape.getSessionId());
                assertSame(manager.getNativeView(shape.getSessionId()), pane.contentPaneForTest().mountedContentForTest());

                sources.open(string);
                select(pane.tabsForTest(), shape.getSessionId());
                assertSame(manager.getNativeView(shape.getSessionId()), pane.contentPaneForTest().mountedContentForTest());

                sources.open(math);
                docs.open(new DocumentationTarget("Math", "https://example.com/Math"));
                assertEquals(JavaFxDocumentationWorkspace.TAB_ID, pane.tabsForTest().selectedTabId());
                select(pane.tabsForTest(), shape.getSessionId());
                assertSame(manager.getNativeView(shape.getSessionId()), pane.contentPaneForTest().mountedContentForTest());
            });
        } finally {
            Files.deleteIfExists(zip);
        }
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
                return "test";
            }
        });
    }

    private static JavaFxDocumentationWorkspace documentationWorkspace() {
        JavaFxDocumentationSurface surface = new JavaFxDocumentationSurface((url, policy) ->
                new JavaFxDocumentationSurface.BrowserAdapter() {
                    private final Node node = new Label("documentation");

                    @Override
                    public Node node() {
                        return node;
                    }

                    @Override
                    public void navigate(String target) {
                    }

                    @Override
                    public void reload() {
                    }

                    @Override
                    public void dispose() {
                    }
                });
        return new JavaFxDocumentationWorkspace(surface);
    }

    private static JdkSourceTarget target(String qualifiedName, String displayName) {
        return new JdkSourceTarget(qualifiedName, "java.base",
                "java.base/" + qualifiedName.replace('.', '/') + ".java", displayName);
    }

    private static Path sourceZip() throws Exception {
        Path zip = Files.createTempFile("eyecode-source-tabs", ".zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip))) {
            add(output, "java.base/java/lang/Math.java", "package java.lang; public final class Math {}");
            add(output, "java.base/java/lang/String.java", "package java.lang; public final class String {}");
        }
        return zip;
    }

    private static void add(ZipOutputStream output, String path, String source) throws Exception {
        output.putNextEntry(new ZipEntry(path));
        output.write(source.getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
    }

    private static void select(FxEditorTabs tabs, String id) {
        tabs.getTabs().stream()
                .filter(tab -> id.equals(tab.getUserData()))
                .findFirst()
                .ifPresent(tab -> tabs.getSelectionModel().select(tab));
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
