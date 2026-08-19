package com.eyecode.javafx.editor;

import com.eyecode.editor.v2.EditorBuffer;
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
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxSemanticAutocompleteTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void manualCompletionPopulatesSnapshot() throws Exception {
        runInFx("""
                class Example {
                    void test() {
                        pri
                    }
                }
                """, "pri", harness -> {
            harness.controller().invokeCompletion(true);
            List<String> labels = harness.buffer().getCompletionSnapshot().getItems().stream()
                    .map(CompletionItem::getLabel)
                    .toList();
            assertTrue(labels.contains("private"));
        });
    }

    @Test
    void ctrlSpaceEventTriggersCompletion() throws Exception {
        runInFx("""
                class Example {
                    void test() {
                        pri
                    }
                }
                """, "pri", harness -> {
            KeyEvent ctrlSpace = ctrlSpace();
            assertTrue(harness.controller().handleCompletionEvent(ctrlSpace));
            assertFalse(harness.buffer().getCompletionSnapshot().isEmpty());
        });
    }

    @Test
    void detailPaneTracksSelectedItem() throws Exception {
        runInFx("""
                class Example {
                    void test() {
                        pri
                    }
                }
                """, "pri", harness -> {
            harness.controller().invokeCompletion(true);
            assertTrue(harness.popup().hasSuggestionList());
            assertTrue(harness.popup().hasDetailPane());
            assertFalse(harness.popup().signatureText().isBlank());

            harness.controller().handleCompletionEvent(new KeyEvent(
                    KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, false, false, false, false
            ));
            assertFalse(harness.popup().detailText().isBlank() || harness.popup().signatureText().isBlank());
        });
    }

    @Test
    void arrowKeysMoveSelectionInsideVisiblePopup() throws Exception {
        runInFx("""
                class Example {
                    void test() {
                        pri
                    }
                }
                """, "pri", harness -> {
            harness.controller().invokeCompletion(true);
            harness.primePopup();
            assertEquals(0, harness.popup().selectedIndex());

            assertTrue(harness.controller().handleCompletionEvent(harness.keyPressed(KeyCode.DOWN)));
            assertEquals(1, harness.popup().selectedIndex());

            assertTrue(harness.controller().handleCompletionEvent(harness.keyPressed(KeyCode.UP)));
            assertEquals(0, harness.popup().selectedIndex());
        });
    }

    @Test
    void escapeClosesVisiblePopup() throws Exception {
        runInFx("""
                class Example {
                    void test() {
                        pri
                    }
                }
                """, "pri", harness -> {
            harness.controller().invokeCompletion(true);
            harness.primePopup();

            assertTrue(harness.controller().handleCompletionEvent(harness.keyPressed(KeyCode.ESCAPE)));
            assertFalse(harness.popup().isShowing());
        });
    }

    @Test
    void enterAcceptsSelectedSuggestionFromPopup() throws Exception {
        runInFx("""
                class Example {
                    void test() {
                        pri
                    }
                }
                """, "pri", harness -> {
            harness.controller().invokeCompletion(true);
            harness.primePopup();
            String selected = harness.popup().getSelectedItem().getInsertText();

            assertTrue(harness.controller().handleCompletionEvent(harness.keyPressed(KeyCode.ENTER)));
            assertTrue(harness.editor().getText().contains("\n        " + selected + "\n"));
            assertFalse(harness.popup().isShowing());
        });
    }

    @Test
    void acceptanceReplacesPrefixAndMovesCaret() throws Exception {
        runInFx("""
                class Example {
                    void test() {
                        int value = 10;
                        val
                    }
                }
                """, "val", harness -> {
            harness.controller().invokeCompletion(true);
            CompletionItem item = harness.buffer().getCompletionSnapshot().getItems().stream()
                    .filter(next -> next.getLabel().equals("value"))
                    .findFirst()
                    .orElseThrow();

            harness.accept(item);

            String text = harness.editor().getText();
            int insertedOffset = text.lastIndexOf("value");
            assertEquals(insertedOffset + "value".length(), harness.editor().getCodeArea().getCaretPosition());
            assertTrue(text.contains("\n        value\n"));
        });
    }

    @Test
    void undoRestoresPreCompletionText() throws Exception {
        runInFx("""
                class Example {
                    void test() {
                        int value = 10;
                        val
                    }
                }
                """, "val", harness -> {
            String before = harness.editor().getText();
            harness.controller().invokeCompletion(true);
            CompletionItem item = harness.buffer().getCompletionSnapshot().getItems().stream()
                    .filter(next -> next.getLabel().equals("value"))
                    .findFirst()
                    .orElseThrow();

            harness.accept(item);
            assertFalse(before.equals(harness.editor().getText()));

            harness.buffer().undo();
            assertEquals(before, harness.editor().getText());
        });
    }

    @Test
    void completionUpdatesDuringTyping() throws Exception {
        runInFx("""
                class Example {
                    void test() {
                        pr
                    }
                }
                """, "pr", harness -> {
            harness.controller().invokeCompletion(true);

            int start = harness.editor().getText().lastIndexOf("pr");
            harness.editor().getCodeArea().replaceText(start, start + 2, "pri");
            harness.editor().getCodeArea().moveTo(start + 3);

            List<String> labels = harness.buffer().getCompletionSnapshot().getItems().stream()
                    .map(CompletionItem::getLabel)
                    .toList();
            assertTrue(labels.contains("private"));
            assertFalse(labels.contains("protected"));
        });
    }

    @Test
    void commentSuppressesCompletion() throws Exception {
        runInFx("""
                class Example {
                    void test() {
                        // pri
                    }
                }
                """, "pri", harness -> {
            harness.controller().invokeCompletion(true);
            assertTrue(harness.buffer().getCompletionSnapshot().isEmpty());
            assertFalse(harness.popup().isShowing());
        });
    }

    @Test
    void stringSuppressesCompletion() throws Exception {
        runInFx("""
                class Example {
                    String value = "pri";
                }
                """, "pri", harness -> {
            harness.controller().invokeCompletion(true);
            assertTrue(harness.buffer().getCompletionSnapshot().isEmpty());
            assertFalse(harness.popup().isShowing());
        });
    }

    @Test
    void nonPopupKeysAreIgnoredWhenPopupIsInactive() throws Exception {
        runInFx("""
                class Example {
                    void test() {
                        value
                    }
                }
                """, "value", harness -> {
            KeyEvent escape = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false);
            KeyEvent down = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, false, false, false, false);

            assertFalse(harness.controller().handleCompletionEvent(escape));
            assertFalse(harness.controller().handleCompletionEvent(down));
        });
    }

    private static void runInFx(String source, String caretToken, ThrowingConsumer<TestHarness> assertions) throws Exception {
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
                JavaFxEditor editor = view.getEditor();
                assertNotNull(editor);
                new Scene(editor, 800, 600);
                editor.applyCss();
                editor.layout();
                int caretOffset = editor.getText().lastIndexOf(caretToken) + caretToken.length();
                editor.getCodeArea().moveTo(caretOffset);
                editor.getCodeArea().requestFocus();
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

    private static KeyEvent ctrlSpace() {
        return new KeyEvent(KeyEvent.KEY_PRESSED, " ", " ", KeyCode.SPACE, false, true, false, false);
    }

    private record TestHarness(JavaFxEditorView view, EditorBuffer buffer) {
        JavaFxEditor editor() {
            return view.getEditor();
        }

        JavaFxEditorController controller() {
            return view.getController();
        }

        JavaFxCompletionPopup popup() {
            return controller().completionPopup();
        }

        KeyEvent keyPressed(KeyCode keyCode) {
            return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", keyCode, false, false, false, false);
        }

        void primePopup() {
            popup().showForTest(buffer().getCompletionSnapshot());
        }

        void accept(CompletionItem item) {
            try {
                Method method = JavaFxEditorController.class.getDeclaredMethod("acceptCompletion", CompletionItem.class);
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
