package com.eyecode.javafx.editor;

import com.eyecode.eventbus.EventBus;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.javafx.learning.JavaFxLearningWorkspace;
import com.eyecode.language.documentation.JdkSourceTarget;
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
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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

    private static KeyEvent ctrlQ() {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "q", KeyCode.Q, false, true, false, false);
    }

    @Test
    void ctrlQIsConsumedOnlyWhenDocumentationResolves() throws Exception {
        runInFx("class Demo { String value; }", editor -> {
            editor.setDocumentationAction(() -> true);
            KeyEvent handled = new KeyEvent(
                    KeyEvent.KEY_PRESSED, "", "q", KeyCode.Q, false, true, false, false);
            assertTrue(editor.handleDocumentationShortcut(handled));
            assertTrue(handled.isConsumed());

            editor.setDocumentationAction(() -> false);
            KeyEvent ignored = new KeyEvent(
                    KeyEvent.KEY_PRESSED, "", "q", KeyCode.Q, false, true, false, false);
            assertFalse(editor.handleDocumentationShortcut(ignored));
            assertFalse(ignored.isConsumed());
        });
    }

    @Test
    void ctrlAltSIsNotARegisteredEditorShortcut() throws Exception {
        runInFx("class Demo { String value; }", editor -> {
            KeyEvent event = new KeyEvent(
                    KeyEvent.KEY_PRESSED, "", "s", KeyCode.S,
                    false, true, true, false);
            editor.getCodeArea().fireEvent(event);
            assertFalse(event.isConsumed());
        });
    }

    @Test
    void editorSourceActionResolvesJdkTypeAndRejectsProjectType() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            JavaFxEditorController controller = null;
            try {
                String source = "class Demo { String value; }";
                EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, source));
                JavaFxEditor editor = new JavaFxEditor(buffer);
                List<JdkSourceTarget> opened = new ArrayList<>();
                controller = new JavaFxEditorController(
                        editor, buffer, new JavaFxLearningWorkspace(), opened::add);
                controller.loadDocument();
                new Scene(editor, 800, 600);
                editor.getCodeArea().moveTo(source.indexOf("String"));
                assertTrue(controller.openJdkSourceAtCaret());
                assertEquals("java.lang.String", opened.getFirst().qualifiedName());

                editor.getCodeArea().moveTo(source.indexOf("Demo"));
                assertFalse(controller.openJdkSourceAtCaret());
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                if (controller != null) {
                    controller.dispose();
                }
                done.countDown();
            }
        });
        assertTrue(done.await(15, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    @Test
    void ctrlBFallsBackToJdkSourceAfterProjectDefinitionMiss() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                List<JdkSourceTarget> opened = new ArrayList<>();
                JavaFxLearningWorkspace learningWorkspace = new JavaFxLearningWorkspace();
                EditorManager manager = new EditorManager(
                        new EventBus(),
                        new NoOpFileSystemService(),
                        new JavaFxEditorViewFactory(learningWorkspace, opened::add));
                String source = "import java.util.List; class Demo { String value; List<String> values; Math math; }";
                EditorSession session = manager.openDocument(Path.of("Demo.java"), source);
                JavaFxEditorView view = (JavaFxEditorView) manager.getView(session.getSessionId()).orElseThrow();
                JavaFxEditor editor = view.getEditor();
                new Scene(editor, 800, 600);
                editor.getCodeArea().moveTo(source.indexOf("String value"));
                assertTrue(editor.goToDefinition());
                assertEquals("java.lang.String", opened.getLast().qualifiedName());

                editor.getCodeArea().moveTo(source.indexOf("List<String>"));
                assertTrue(editor.goToDefinition());
                assertEquals("java.util.List", opened.getLast().qualifiedName());

                editor.getCodeArea().moveTo(source.indexOf("Math math"));
                assertTrue(editor.goToDefinition());
                assertEquals("java.lang.Math", opened.getLast().qualifiedName());
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(15, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    @Test
    void ctrlBProjectTypeWinsOverJdkFallback() throws Exception {
        String source = "package demo; class String {} class Demo { String value; }";
        runInFx(source, editor -> {
            putCaret(editor, source.lastIndexOf("String value"));
            assertTrue(editor.goToDefinition());
            assertEquals(source.indexOf("class String"), editor.getCodeArea().getCaretPosition());
        });
    }

    @Test
    void ctrlBRepeatedAtSameCaretIsDeterministic() throws Exception {
        String source = "String value; Object other; Math math;";
        List<JdkSourceTarget> opened = new ArrayList<>();
        runInFx(source, new JavaFxLearningWorkspace(), opened::add, editor -> {
            putCaret(editor, source.indexOf("Math"));
            assertTrue(editor.handleGoToDefinitionShortcut(ctrlB()));
            assertTrue(editor.handleGoToDefinitionShortcut(ctrlB()));
            assertEquals(List.of("java.lang.Math", "java.lang.Math"),
                    opened.stream().map(JdkSourceTarget::qualifiedName).toList());
        });
    }

    @Test
    void ctrlBTracksCaretMovementAcrossJdkTypes() throws Exception {
        String source = "String value; Object other; Math math;";
        List<JdkSourceTarget> opened = new ArrayList<>();
        runInFx(source, new JavaFxLearningWorkspace(), opened::add, editor -> {
            putCaret(editor, source.indexOf("String"));
            assertTrue(editor.handleGoToDefinitionShortcut(ctrlB()));
            putCaret(editor, source.indexOf("Math"));
            assertTrue(editor.handleGoToDefinitionShortcut(ctrlB()));
            assertEquals(List.of("java.lang.String", "java.lang.Math"),
                    opened.stream().map(JdkSourceTarget::qualifiedName).toList());
        });
    }

    @Test
    void ctrlQUsesCaretForDocumentationNavigation() throws Exception {
        String source = "String value; Object other; Math math;";
        List<String> opened = new ArrayList<>();
        JavaFxLearningWorkspace workspace = new JavaFxLearningWorkspace(
                target -> opened.add(target.label()));
        runInFx(source, workspace, target -> { }, editor -> {
            putCaret(editor, source.indexOf("Object"));
            assertTrue(editor.handleDocumentationShortcut(ctrlQ()));
            assertEquals("Object", opened.getLast());
        });
    }

    private static void runInFx(String source, ThrowingConsumer<JavaFxEditor> assertions) throws Exception {
        runInFx(source, new JavaFxLearningWorkspace(), target -> { }, assertions);
    }

    private static void runInFx(String source,
                                JavaFxLearningWorkspace learningWorkspace,
                                Consumer<JdkSourceTarget> sourceNavigator,
                                ThrowingConsumer<JavaFxEditor> assertions) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                EditorManager manager = new EditorManager(
                        new EventBus(),
                        new NoOpFileSystemService(),
                new JavaFxEditorViewFactory(learningWorkspace, sourceNavigator)
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
