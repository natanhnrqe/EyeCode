package com.eyecode.javafx.editor;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.eventbus.EventBus;
import com.eyecode.filesystem.FileSystemService;
import com.eyecode.javafx.editor.view.JavaFxEditorView;
import com.eyecode.javafx.editor.view.JavaFxEditorViewFactory;
import com.eyecode.workbench.editor.EditorManager;
import com.eyecode.workbench.editor.EditorSession;
import javafx.application.Platform;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxIndentGuideTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void layerExposesNoDescriptorsWithoutIndentation() throws Exception {
        runInFx("class Example {}", harness ->
                assertEquals(List.of(), harness.layer().descriptorsForTest()));
    }

    @Test
    void layerBuildsDescriptorsForNestedIndentation() throws Exception {
        runInFx("""
                class Example {
                    void test() {
                        if (ok) {
                            run();
                        }
                    }
                }
                """, harness -> {
            assertEquals(List.of(
                    new IndentGuideDescriptor(4, 1, 5),
                    new IndentGuideDescriptor(8, 2, 4),
                    new IndentGuideDescriptor(12, 3, 3)
            ), harness.layer().descriptorsForTest());
        });
    }

    @Test
    void blankLineWithoutIndentationBreaksDescriptors() throws Exception {
        runInFx("""
                if (ok) {
                    foo();

                    bar();
                }
                """, harness -> {
            assertEquals(List.of(
                    new IndentGuideDescriptor(4, 1, 1),
                    new IndentGuideDescriptor(4, 3, 3)
            ), harness.layer().descriptorsForTest());
        });
    }

    @Test
    void enterUpdatesDescriptorRanges() throws Exception {
        runInFx("if (true) {\n    while (ok) {|}\n}", harness -> {
            harness.fireEnter();

            assertEquals(List.of(
                    new IndentGuideDescriptor(4, 1, 3),
                    new IndentGuideDescriptor(8, 2, 2)
            ), harness.layer().descriptorsForTest());
        });
    }

    @Test
    void backspaceDedentUpdatesDescriptorRanges() throws Exception {
        runInFx("        |value++;", harness -> {
            assertEquals(List.of(
                    new IndentGuideDescriptor(4, 0, 0),
                    new IndentGuideDescriptor(8, 0, 0)
            ), harness.layer().descriptorsForTest());

            harness.fireBackspace();

            assertEquals(List.of(new IndentGuideDescriptor(4, 0, 0)), harness.layer().descriptorsForTest());
        });
    }

    @Test
    void pasteUpdatesDescriptors() throws Exception {
        runInFx("    value++;", harness -> {
            harness.editor().getCodeArea().replaceText("        value++;");

            assertEquals(List.of(
                    new IndentGuideDescriptor(4, 0, 0),
                    new IndentGuideDescriptor(8, 0, 0)
            ), harness.layer().descriptorsForTest());
        });
    }

    @Test
    void lineDeletionRecomputesDescriptors() throws Exception {
        runInFx("""
                class Example {
                    void test() {
                        value++;
                    }
                }
                """, harness -> {
            harness.editor().getCodeArea().replaceText("""
                    class Example {
                        value++;
                    }
                    """);

            assertEquals(List.of(new IndentGuideDescriptor(4, 1, 1)), harness.layer().descriptorsForTest());
        });
    }

    @Test
    void documentSwitchKeepsIndependentDescriptorSets() throws Exception {
        runInFxWithManager(manager -> {
            EditorSession first = manager.openDocument(Path.of("First.java"), "    one();");
            EditorSession second = manager.openDocument(Path.of("Second.java"), "        two();");

            JavaFxEditorView firstView = (JavaFxEditorView) manager.getView(first.getSessionId()).orElseThrow();
            JavaFxEditorView secondView = (JavaFxEditorView) manager.getView(second.getSessionId()).orElseThrow();
            prepare(firstView.getEditor());
            prepare(secondView.getEditor());

            assertEquals(List.of(new IndentGuideDescriptor(4, 0, 0)), firstView.getEditor().indentGuideLayer().descriptorsForTest());
            assertEquals(List.of(
                    new IndentGuideDescriptor(4, 0, 0),
                    new IndentGuideDescriptor(8, 0, 0)
            ), secondView.getEditor().indentGuideLayer().descriptorsForTest());
        });
    }

    @Test
    void renderingDoesNotMutateDocumentCaretOrSelection() throws Exception {
        runInFx("        value++;", harness -> {
            harness.editor().getCodeArea().moveTo(4);
            harness.editor().getCodeArea().selectRange(1, 6);
            String beforeText = harness.editor().getText();
            int beforeCaret = harness.editor().getCodeArea().getCaretPosition();
            IndexRange beforeSelection = harness.editor().getCodeArea().getSelection();

            harness.layer().rebuildAndRepaint();

            assertEquals(beforeText, harness.editor().getText());
            assertEquals(beforeCaret, harness.editor().getCodeArea().getCaretPosition());
            assertEquals(beforeSelection, harness.editor().getCodeArea().getSelection());
        });
    }

    @Test
    void visibleProjectionPaintsOnlyIntersectingGuideSpans() {
        List<IndentGuideDescriptor> descriptors = List.of(
                new IndentGuideDescriptor(4, 0, 3),
                new IndentGuideDescriptor(8, 1, 3),
                new IndentGuideDescriptor(12, 4, 5)
        );
        List<JavaFxIndentGuideLayer.VisibleParagraphBounds> visible = List.of(
                viewport(1, new BoundingBox(40, 10, 100, 18)),
                viewport(2, new BoundingBox(40, 28, 100, 18)),
                viewport(3, new BoundingBox(40, 46, 100, 18))
        );

        List<JavaFxIndentGuideLayer.RenderedGuide> rendered = JavaFxIndentGuideLayer.projectVisibleGuides(
                descriptors,
                visible,
                1,
                8,
                7.0,
                1.0
        );

        assertEquals(2, rendered.size());
        assertEquals(new IndentGuideDescriptor(4, 0, 3), rendered.get(0).descriptor());
        assertEquals(new IndentGuideDescriptor(8, 1, 3), rendered.get(1).descriptor());
        assertTrue(rendered.get(0).endY() > rendered.get(0).startY());
        assertTrue(rendered.get(1).active());
    }

    @Test
    void projectionPositionsAreDeterministicAndMonotonic() {
        List<JavaFxIndentGuideLayer.RenderedGuide> first = JavaFxIndentGuideLayer.projectVisibleGuides(
                List.of(
                        new IndentGuideDescriptor(4, 0, 2),
                        new IndentGuideDescriptor(8, 0, 2)
                ),
                List.of(
                        viewport(0, new BoundingBox(50, 12, 100, 18)),
                        viewport(1, new BoundingBox(50, 30, 100, 18)),
                        viewport(2, new BoundingBox(50, 48, 100, 18))
                ),
                0,
                8,
                7.0,
                1.0
        );
        List<JavaFxIndentGuideLayer.RenderedGuide> second = JavaFxIndentGuideLayer.projectVisibleGuides(
                List.of(
                        new IndentGuideDescriptor(4, 0, 2),
                        new IndentGuideDescriptor(8, 0, 2)
                ),
                List.of(
                        viewport(0, new BoundingBox(50, 12, 100, 18)),
                        viewport(1, new BoundingBox(50, 30, 100, 18)),
                        viewport(2, new BoundingBox(50, 48, 100, 18))
                ),
                0,
                8,
                7.0,
                1.0
        );

        assertEquals(first, second);
        assertTrue(first.get(1).x() > first.get(0).x());
    }

    @Test
    void projectionStopsBeforeKeywordAtGuideColumn() {
        List<JavaFxIndentGuideLayer.RenderedGuide> rendered = JavaFxIndentGuideLayer.projectVisibleGuides(
                List.of(new IndentGuideDescriptor(4, 0, 4)),
                List.of(
                        viewport(0, new BoundingBox(40, 0, 100, 18), line(0, false)),
                        viewport(1, new BoundingBox(40, 18, 100, 18), line(8, false)),
                        viewport(2, new BoundingBox(40, 36, 100, 18), line(4, false)),
                        viewport(3, new BoundingBox(40, 54, 100, 18), line(8, false)),
                        viewport(4, new BoundingBox(40, 72, 100, 18), line(0, false))
                ),
                1, 4, 7.0, 1.0
        );

        assertEquals(2, rendered.size());
        assertEquals(18.0, rendered.get(0).startY());
        assertEquals(36.0, rendered.get(0).endY());
        assertEquals(54.0, rendered.get(1).startY());
        assertEquals(72.0, rendered.get(1).endY());
    }

    @Test
    void projectionPreservesGuidesThroughIndentedBlankLines() {
        List<JavaFxIndentGuideLayer.RenderedGuide> rendered = JavaFxIndentGuideLayer.projectVisibleGuides(
                List.of(new IndentGuideDescriptor(4, 0, 4)),
                List.of(
                        viewport(0, new BoundingBox(40, 0, 100, 18), line(0, false)),
                        viewport(1, new BoundingBox(40, 18, 100, 18), line(8, false)),
                        viewport(2, new BoundingBox(40, 36, 100, 18), line(4, true)),
                        viewport(3, new BoundingBox(40, 54, 100, 18), line(8, false)),
                        viewport(4, new BoundingBox(40, 72, 100, 18), line(0, false))
                ),
                1, 4, 7.0, 1.0
        );

        assertEquals(1, rendered.size());
        assertEquals(18.0, rendered.getFirst().startY());
        assertEquals(72.0, rendered.getFirst().endY());
    }

    private static JavaFxIndentGuideLayer.VisibleParagraphBounds viewport(int paragraphIndex, Bounds bounds) {
        return JavaFxIndentGuideLayer.visibleParagraphBoundsForTest(paragraphIndex, bounds);
    }

    private static JavaFxIndentGuideLayer.VisibleParagraphBounds viewport(int paragraphIndex,
                                                                           Bounds bounds,
                                                                           IndentGuideLine line) {
        return JavaFxIndentGuideLayer.visibleParagraphBoundsForTest(paragraphIndex, bounds, line);
    }

    private static IndentGuideLine line(int leadingIndentColumn, boolean blank) {
        return new IndentGuideLine(List.of(), leadingIndentColumn, blank);
    }

    private static void runInFx(String sourceWithCaret, ThrowingConsumer<TestHarness> assertions) throws Exception {
        int caretOffset = sourceWithCaret.indexOf('|');
        String source = caretOffset >= 0
                ? sourceWithCaret.substring(0, caretOffset) + sourceWithCaret.substring(caretOffset + 1)
                : sourceWithCaret;
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
                prepare(view.getEditor());
                if (caretOffset >= 0) {
                    view.getEditor().getCodeArea().moveTo(caretOffset);
                    view.getEditor().getCodeArea().requestFocus();
                }
                assertions.accept(new TestHarness(view, manager.getBuffer(session.getSessionId()).orElseThrow()));
            } catch (Throwable throwable) {
                failure.set(throwable);
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

    private static void runInFxWithManager(ThrowingConsumer<EditorManager> assertions) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                EditorManager manager = new EditorManager(
                        new EventBus(),
                        new NoOpFileSystemService(),
                        new JavaFxEditorViewFactory()
                );
                assertions.accept(manager);
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(15, TimeUnit.SECONDS), "JavaFX task timed out");
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    private static void prepare(JavaFxEditor editor) {
        new Scene(editor, 900, 600);
        editor.applyCss();
        editor.layout();
        editor.getCodeArea().applyCss();
        editor.getCodeArea().layout();
    }

    private record TestHarness(JavaFxEditorView view, EditorBuffer buffer) {
        JavaFxEditor editor() {
            return view.getEditor();
        }

        JavaFxIndentGuideLayer layer() {
            return editor().indentGuideLayer();
        }

        void fireEnter() {
            editor().getCodeArea().fireEvent(new KeyEvent(
                    KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false
            ));
        }

        void fireBackspace() {
            editor().getCodeArea().fireEvent(new KeyEvent(
                    KeyEvent.KEY_PRESSED, "", "", KeyCode.BACK_SPACE, false, false, false, false
            ));
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
