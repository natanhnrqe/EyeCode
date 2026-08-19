package com.eyecode.javafx.editor;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.eventbus.EventBus;
import com.eyecode.filesystem.FileSystemService;
import com.eyecode.javafx.editor.view.JavaFxEditorView;
import com.eyecode.javafx.editor.view.JavaFxEditorViewFactory;
import com.eyecode.workbench.editor.EditorManager;
import com.eyecode.workbench.editor.EditorSession;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxEnterCaretRegressionTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void enterInsertsNewlineAndMovesCaretToIndentedLine() throws Exception {
        runInFx("    int x;|", harness -> {
            harness.fireEnter();
            assertEquals("    int x;\n    ", harness.editor().getText());
            assertEquals("    int x;\n    ".length(), harness.editor().getCodeArea().getCaretPosition());
        });
    }

    @Test
    void enterAtDocumentStartMovesCaretForward() throws Exception {
        runInFx("|class Example {}", harness -> {
            harness.fireEnter();
            assertEquals("\nclass Example {}", harness.editor().getText());
            assertEquals(1, harness.editor().getCodeArea().getCaretPosition());
        });
    }

    @Test
    void enterAtDocumentEndMovesCaretToNewIndentedLine() throws Exception {
        runInFx("class Example {\n    void test() {\n        int value = 1;\n    }\n}|", harness -> {
            harness.fireEnter();
            assertTrue(harness.editor().getText().endsWith("\n"));
            assertEquals(harness.editor().getText().length(), harness.editor().getCodeArea().getCaretPosition());
        });
    }

    @Test
    void smartEnterWithBracesMovesCaretInsideBlock() throws Exception {
        runInFx("void m() {|}", harness -> {
            harness.fireEnter();
            assertEquals("void m() {\n    \n}", harness.editor().getText());
            assertEquals(15, harness.editor().getCodeArea().getCaretPosition());
        });
    }

    @Test
    void nestedIndentationPlacesCaretAfterIndent() throws Exception {
        runInFx("if (true) {\n    while (ok) {|}\n}", harness -> {
            harness.fireEnter();
            assertEquals("if (true) {\n    while (ok) {\n        \n    }\n}", harness.editor().getText());
            assertEquals("if (true) {\n    while (ok) {\n        ".length(), harness.editor().getCodeArea().getCaretPosition());
        });
    }

    @Test
    void undoRestoresContentAfterEnter() throws Exception {
        runInFx("void m() {|}", harness -> {
            String before = harness.editor().getText();
            harness.fireEnter();
            harness.buffer().undo();
            assertEquals(before, harness.editor().getText());
        });
    }

    @Test
    void popupClosedEnterStillUsesSmartEditing() throws Exception {
        runInFx("void m() {|}", harness -> {
            assertFalse(harness.controller().completionPopup().isShowing());
            harness.fireEnter();
            assertEquals("void m() {\n    \n}", harness.editor().getText());
        });
    }

    @Test
    void completionAcceptanceDoesNotTriggerSmartEnter() throws Exception {
        runInFx("""
                class Example {
                    void test() {
                        int value = 10;
                        val|
                    }
                }
                """, harness -> {
            harness.controller().invokeCompletion(true);
            assertFalse(harness.buffer().getCompletionSnapshot().isEmpty());
            CompletionItem item = harness.buffer().getCompletionSnapshot().getItems().stream()
                    .filter(next -> next.getLabel().equals("value"))
                    .findFirst()
                    .orElseThrow();
            harness.accept(item);
            assertTrue(harness.editor().getText().contains("\n        value\n"));
            assertFalse(harness.editor().getText().contains("\n        val\n\n"));
        });
    }

    private static void runInFx(String sourceWithCaret, ThrowingConsumer<TestHarness> assertions) throws Exception {
        int caretOffset = sourceWithCaret.indexOf('|');
        String source = sourceWithCaret.substring(0, caretOffset) + sourceWithCaret.substring(caretOffset + 1);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            JavaFxEditorView view = null;
            try {
                EditorManager manager = new EditorManager(
                        new EventBus(),
                        new NoOpFileSystemService(),
                        new JavaFxEditorViewFactory()
                );
                EditorSession session = manager.openDocument(Path.of("Test.java"), source);
                view = (JavaFxEditorView) manager.getView(session.getSessionId()).orElseThrow();
                new Scene(view.getEditor(), 800, 600);
                view.getEditor().applyCss();
                view.getEditor().layout();
                view.getEditor().getCodeArea().moveTo(caretOffset);
                view.getEditor().getCodeArea().requestFocus();
                assertions.accept(new TestHarness(view, manager.getBuffer(session.getSessionId()).orElseThrow()));
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                try {
                    if (view != null) {
                        view.dispose();
                    }
                } finally {
                    done.countDown();
                }
            }
        });
        assertTrue(done.await(15, TimeUnit.SECONDS), "JavaFX task timed out");
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    private record TestHarness(JavaFxEditorView view, com.eyecode.editor.v2.EditorBuffer buffer) {
        JavaFxEditor editor() {
            return view.getEditor();
        }

        JavaFxEditorController controller() {
            return view.getController();
        }

        void fireEnter() {
            editor().getCodeArea().fireEvent(new KeyEvent(
                    KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false
            ));
        }

        void accept(CompletionItem item) {
            try {
                java.lang.reflect.Method method = JavaFxEditorController.class
                        .getDeclaredMethod("acceptCompletion", CompletionItem.class);
                method.setAccessible(true);
                method.invoke(controller(), item);
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError(ex);
            }
        }
    }

    @FunctionalInterface
    private interface ThrowingConsumer<T> {
        void accept(T value) throws Exception;
    }

    private static final class NoOpFileSystemService implements FileSystemService {

        @Override
        public String readFile(Path path) throws IOException {
            throw new IOException("Not used in test");
        }

        @Override
        public void writeFile(Path path, String content) {
        }

        @Override
        public boolean exists(Path path) {
            return false;
        }

        @Override
        public void createDirectories(Path path) {
        }

        @Override
        public List<Path> listFiles(Path directory) {
            return List.of();
        }

        @Override
        public List<Path> findFiles(Path root, String globPattern) {
            return List.of();
        }

        @Override
        public void copyResource(InputStream source, Path target) {
        }
    }
}
