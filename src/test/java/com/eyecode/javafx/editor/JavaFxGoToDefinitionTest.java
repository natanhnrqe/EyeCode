package com.eyecode.javafx.editor;

import com.eyecode.eventbus.EventBus;
import com.eyecode.filesystem.FileSystemService;
import com.eyecode.javafx.editor.view.JavaFxEditorView;
import com.eyecode.javafx.editor.view.JavaFxEditorViewFactory;
import com.eyecode.workbench.editor.EditorManager;
import com.eyecode.workbench.editor.EditorSession;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.IndexRange;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxGoToDefinitionTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void localVariable_movesCaretToDeclaration() throws Exception {
        String source = """
                class C {
                    void m() {
                        int value = 1;
                        int x = value;
                    }
                }
                """;
        runInFx(source, editor -> {
            putCaret(editor, source.indexOf("= value") + 2);
            assertTrue(editor.goToDefinition());
            assertEquals(source.indexOf("value = 1"), editor.getCodeArea().getCaretPosition());
        });
    }

    @Test
    void parameter_movesCaretToDeclaration() throws Exception {
        String source = """
                class C {
                    void m(int value) {
                        int x = value;
                    }
                }
                """;
        runInFx(source, editor -> {
            putCaret(editor, source.indexOf("= value") + 2);
            assertTrue(editor.goToDefinition());
            assertEquals(source.indexOf("int value)"), editor.getCodeArea().getCaretPosition());
        });
    }

    @Test
    void field_movesCaretToDeclaration() throws Exception {
        String source = """
                class Example {
                    int value;

                    void test() {
                        value++;
                    }
                }
                """;
        runInFx(source, editor -> {
            putCaret(editor, source.lastIndexOf("value"));
            assertTrue(editor.goToDefinition());
            assertEquals(source.indexOf("int value;"), editor.getCodeArea().getCaretPosition());
        });
    }

    @Test
    void methodCall_movesCaretToDeclaration() throws Exception {
        String source = """
                class C {
                    void hello() { }
                    void run() {
                        hello();
                    }
                }
                """;
        runInFx(source, editor -> {
            putCaret(editor, source.lastIndexOf("hello();"));
            assertTrue(editor.goToDefinition());
            assertEquals(source.indexOf("void hello()"), editor.getCodeArea().getCaretPosition());
        });
    }

    @Test
    void thisMethod_movesCaretToDeclaration() throws Exception {
        String source = """
                class C {
                    void hello() { }
                    void run() {
                        this.hello();
                    }
                }
                """;
        runInFx(source, editor -> {
            putCaret(editor, source.indexOf("hello();", source.indexOf("this.")));
            assertTrue(editor.goToDefinition());
            assertEquals(source.indexOf("void hello()"), editor.getCodeArea().getCaretPosition());
        });
    }

    @Test
    void staticMethod_movesCaretToDeclaration() throws Exception {
        String source = """
                class Helper {
                    static void hello() { }
                }
                class C {
                    void run() {
                        Helper.hello();
                    }
                }
                """;
        runInFx(source, editor -> {
            putCaret(editor, source.lastIndexOf("hello();"));
            assertTrue(editor.goToDefinition());
            assertEquals(source.indexOf("static void hello()"), editor.getCodeArea().getCaretPosition());
        });
    }

    @Test
    void constructorCall_movesCaretToDeclaration() throws Exception {
        String source = """
                class Foo {
                    Foo() { }
                }
                class C {
                    void run() {
                        new Foo();
                    }
                }
                """;
        runInFx(source, editor -> {
            putCaret(editor, source.lastIndexOf("Foo();"));
            assertTrue(editor.goToDefinition());
            assertEquals(source.indexOf("Foo()"), editor.getCodeArea().getCaretPosition());
        });
    }

    @Test
    void unresolvedSymbol_doesNothing() throws Exception {
        String source = "class C { void m() { missing(); } }";
        runInFx(source, editor -> {
            int caret = source.indexOf("missing");
            putCaret(editor, caret);
            assertFalse(editor.goToDefinition());
            assertEquals(caret, editor.getCodeArea().getCaretPosition());
        });
    }

    @Test
    void whitespace_doesNothing() throws Exception {
        String source = "class C { void m() { int x = 1; } }";
        runInFx(source, editor -> {
            int caret = source.indexOf(" { ");
            putCaret(editor, caret + 1);
            assertFalse(editor.goToDefinition());
            assertEquals(caret + 1, editor.getCodeArea().getCaretPosition());
        });
    }

    @Test
    void comment_doesNothing() throws Exception {
        String source = """
                class C {
                    // comment text
                    void m() { }
                }
                """;
        runInFx(source, editor -> {
            int caret = source.indexOf("comment");
            putCaret(editor, caret + 2);
            assertFalse(editor.goToDefinition());
            assertEquals(caret + 2, editor.getCodeArea().getCaretPosition());
        });
    }

    @Test
    void string_doesNothing() throws Exception {
        String source = "class C { String s = \"hello\"; }";
        runInFx(source, editor -> {
            int caret = source.indexOf("hello");
            putCaret(editor, caret + 2);
            assertFalse(editor.goToDefinition());
            assertEquals(caret + 2, editor.getCodeArea().getCaretPosition());
        });
    }

    @Test
    void repeatedNavigation_isSafeAndDeterministic() throws Exception {
        String source = """
                class C {
                    int value;
                    void m() {
                        value++;
                    }
                }
                """;
        runInFx(source, editor -> {
            int ref = source.lastIndexOf("value");
            putCaret(editor, ref);
            assertTrue(editor.goToDefinition());
            int first = editor.getCodeArea().getCaretPosition();
            putCaret(editor, ref);
            assertTrue(editor.goToDefinition());
            assertEquals(first, editor.getCodeArea().getCaretPosition());
        });
    }

    @Test
    void revealOffset_movesCaretToDeclarationAndClearsSelection() throws Exception {
        String source = """
                class C {
                    int value;
                    void m() {
                        value++;
                    }
                }
                """;
        runInFx(source, editor -> {
            editor.getCodeArea().selectRange(0, 5);
            editor.revealOffset(source.indexOf("int value;"));
            assertEquals(source.indexOf("int value;"), editor.getCodeArea().getCaretPosition());
            IndexRange selection = editor.getCodeArea().getSelection();
            assertEquals(selection.getStart(), selection.getEnd());
        });
    }

    @Test
    void revealOffset_doesNotThrow() throws Exception {
        String source = "class C { int value; }";
        runInFx(source, editor -> {
            editor.revealOffset(Integer.MAX_VALUE);
            assertTrue(editor.getCodeArea().getCaretPosition() <= source.length());
            editor.revealOffset(-1);
            assertTrue(editor.getCodeArea().getCaretPosition() >= 0);
        });
    }

    @Test
    void ctrlBEvent_consumedOnlyWhenDefinitionExists() throws Exception {
        String source = """
                class C {
                    void hello() { }
                    void run() { hello(); }
                }
                """;
        runInFx(source, editor -> {
            putCaret(editor, source.lastIndexOf("hello();"));
            KeyEvent handled = ctrlB();
            assertTrue(editor.handleGoToDefinitionShortcut(handled));
            assertTrue(handled.isConsumed());

            int whitespace = source.indexOf(" { ");
            putCaret(editor, whitespace + 1);
            KeyEvent ignored = ctrlB();
            assertFalse(editor.handleGoToDefinitionShortcut(ignored));
            assertFalse(ignored.isConsumed());
        });
    }

    private static void putCaret(JavaFxEditor editor, int offset) {
        editor.getCodeArea().moveTo(offset);
    }

    private static KeyEvent ctrlB() {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "b", KeyCode.B, false, true, false, false);
    }

    private static void runInFx(String source, ThrowingConsumer<JavaFxEditor> assertions) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                EditorManager manager = new EditorManager(
                        new EventBus(),
                        new NoOpFileSystemService(),
                        new JavaFxEditorViewFactory()
                );
                EditorSession session = manager.openDocument(Path.of("Test.java"), source);
                JavaFxEditorView view = (JavaFxEditorView) manager.getView(session.getSessionId()).orElseThrow();
                JavaFxEditor editor = view.getEditor();
                assertNotNull(editor);
                new Scene(editor, 800, 600);
                editor.applyCss();
                editor.layout();
                editor.getCodeArea().requestFocus();
                assertions.accept(editor);
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(15, TimeUnit.SECONDS), "JavaFX task timed out");
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
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
