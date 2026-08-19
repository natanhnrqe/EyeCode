package com.eyecode.javafx.editor;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;
import com.eyecode.editor.v2.completion.CompletionSnapshot;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxCompletionPopupTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void layoutAnchorsNearCaretBelowWhenRoomExists() {
        Bounds caret = new BoundingBox(300, 200, 1, 18);
        Rectangle2D screen = new Rectangle2D(0, 0, 1280, 720);

        JavaFxCompletionPopup.PopupLayout layout = JavaFxCompletionPopup.layoutFor(caret, screen, 600, 320);

        assertTrue(layout.x() <= caret.getMinX());
        assertTrue(layout.x() >= 0);
        assertTrue(layout.y() >= caret.getMaxY());
        assertFalse(layout.aboveCaret());
    }

    @Test
    void layoutMovesAboveWhenNoRoomBelow() {
        Bounds caret = new BoundingBox(300, 690, 1, 18);
        Rectangle2D screen = new Rectangle2D(0, 0, 1280, 720);

        JavaFxCompletionPopup.PopupLayout layout = JavaFxCompletionPopup.layoutFor(caret, screen, 600, 220);

        assertTrue(layout.aboveCaret());
        assertTrue(layout.y() + layout.height() <= caret.getMinY());
    }

    @Test
    void layoutShiftsLeftWhenNearRightEdge() {
        Bounds caret = new BoundingBox(1240, 200, 1, 18);
        Rectangle2D screen = new Rectangle2D(0, 0, 1280, 720);

        JavaFxCompletionPopup.PopupLayout layout = JavaFxCompletionPopup.layoutFor(caret, screen, 600, 320);

        assertTrue(layout.x() + layout.width() <= screen.getMaxX());
    }

    @Test
    void popupUsesBoundedDimensionsAndVisibleRows() throws Exception {
        runOnFx(() -> {
            JavaFxCompletionPopup popup = new JavaFxCompletionPopup();
            popup.applySnapshotForTest(new CompletionSnapshot(items(18)));

            assertEquals(600.0, popup.boundedPopupWidth());
            assertEquals(10, popup.visibleRowCount());
            assertTrue(popup.boundedPopupHeight() < 700);
            assertTrue(popup.boundedPopupHeight() > 200);
        });
    }

    @Test
    void rowGraphicIncludesIconAndMetadataHierarchy() throws Exception {
        runOnFx(() -> {
            JavaFxCompletionPopup popup = new JavaFxCompletionPopup();
            CompletionItem item = CompletionItem.builder("println", "println", CompletionItemKind.METHOD)
                    .signature("println(String value)")
                    .returnType("void")
                    .owner("java.io.PrintStream")
                    .documentation("Prints a value.")
                    .example("System.out.println(value);")
                    .build();

            JavaFxCompletionPopup.CompletionRowGraphic row = popup.rowGraphicFor(item, true, false);

            assertTrue(row.hasIcon());
            assertEquals("println", row.primaryText());
            assertEquals("println(String value)", row.secondaryText());
            assertEquals("java.io.PrintStream", row.ownerText());
            assertEquals("void", row.returnTypeText());
        });
    }

    private static List<CompletionItem> items(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> CompletionItem.builder("item" + index, "item" + index, CompletionItemKind.METHOD)
                        .signature("item" + index + "()")
                        .returnType("void")
                        .owner("pkg.Owner")
                        .documentation("Doc " + index)
                        .example("item" + index + "();")
                        .build())
                .toList();
    }

    private static void runOnFx(ThrowingRunnable action) throws Exception {
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
        assertTrue(done.await(15, TimeUnit.SECONDS), "JavaFX task timed out");
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
