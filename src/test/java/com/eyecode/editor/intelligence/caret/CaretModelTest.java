package com.eyecode.editor.intelligence.caret;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.EditorSelection;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Offset-based {@link CaretModel} invariants over an {@link EditorBuffer}:
 * clamped offsets, normalized selections, empty range = no selection, and
 * projection of buffer-native caret/selection moves.
 */
class CaretModelTest {

    private static final String TEXT = "line one\nline two";

    private final EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, TEXT));
    private final CaretModel caretModel = new DefaultCaretModel(buffer);

    private static EditorPosition pos(EditorBuffer buffer, int offset) {
        return buffer.getDocument().positionOf(offset);
    }

    @Test
    void initialCaretIsAtStartWithoutSelection() {
        assertEquals(0, caretModel.offset());
        assertFalse(caretModel.hasSelection());
        assertTrue(caretModel.selection().isEmpty());
    }

    @Test
    void moveToClampsNegativeOffset() {
        caretModel.moveTo(-5);
        assertEquals(0, caretModel.offset());
    }

    @Test
    void moveToClampsBeyondDocumentEnd() {
        caretModel.moveTo(999);
        assertEquals(TEXT.length(), caretModel.offset());
    }

    @Test
    void moveToClearsSelection() {
        caretModel.setSelection(new TextRange(0, 4));
        caretModel.moveTo(3);
        assertEquals(3, caretModel.offset());
        assertFalse(caretModel.hasSelection());
    }

    @Test
    void moveToWithKeepSelectionPreservesRange() {
        caretModel.setSelection(new TextRange(2, 6));
        caretModel.moveTo(8, true);
        assertEquals(8, caretModel.offset());
        assertEquals(Optional.of(new TextRange(2, 6)), caretModel.selection());
        assertTrue(caretModel.hasSelection());
    }

    @Test
    void selectionNormalizesReversedBufferSelection() {
        buffer.setSelection(new EditorSelection(pos(buffer, 6), pos(buffer, 2)));
        assertEquals(Optional.of(new TextRange(2, 6)), caretModel.selection());
    }

    @Test
    void setSelectionWithEmptyRangeClears() {
        caretModel.setSelection(new TextRange(3, 3));
        assertEquals(3, caretModel.offset());
        assertFalse(caretModel.hasSelection());
    }

    @Test
    void clearSelectionKeepsCaret() {
        caretModel.setSelection(new TextRange(0, 5));
        caretModel.clearSelection();
        assertEquals(5, caretModel.offset());
        assertFalse(caretModel.hasSelection());
    }

    @Test
    void selectAllCoversDocument() {
        caretModel.selectAll();
        assertEquals(TEXT.length(), caretModel.offset());
        assertEquals(Optional.of(new TextRange(0, TEXT.length())), caretModel.selection());
    }

    @Test
    void moveToReflectsBufferNativeCaretMove() {
        buffer.moveCaret(pos(buffer, 7));
        assertEquals(7, caretModel.offset());
    }

    @Test
    void selectionReflectsBufferNativeSelection() {
        buffer.setSelection(new EditorSelection(pos(buffer, 2), pos(buffer, 6)));
        assertEquals(Optional.of(new TextRange(2, 6)), caretModel.selection());
    }

    @Test
    void setSelectionBeyondDocumentEndClamps() {
        caretModel.setSelection(new TextRange(0, 999));
        assertEquals(Optional.of(new TextRange(0, TEXT.length())), caretModel.selection());
    }

    @Test
    void documentLengthMatchesText() {
        assertEquals(TEXT.length(), caretModel.documentLength());
    }
}
