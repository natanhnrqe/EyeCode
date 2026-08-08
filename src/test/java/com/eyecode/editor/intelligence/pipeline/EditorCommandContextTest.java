package com.eyecode.editor.intelligence.pipeline;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.DocumentTransaction;
import com.eyecode.editor.intelligence.events.DocumentChangeListener;
import com.eyecode.editor.intelligence.events.DocumentTextChangeEvent;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.EditorSelection;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EditorCommandContextTest {

    @Test
    void exposesImmutableSnapshotOfDocument() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, "abc"));
        EditorCommandContext context = new EditorCommandContext(buffer);

        DocumentSnapshot snapshot = context.snapshot();
        assertEquals("abc", snapshot.getText());
        assertEquals(1, snapshot.version());
    }

    @Test
    void exposesCaretAndSelectionFromBuffer() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, "abc"));
        buffer.moveCaret(new EditorPosition(0, 2));
        buffer.setSelection(new EditorSelection(new EditorPosition(0, 1), new EditorPosition(0, 2)));

        EditorCommandContext context = new EditorCommandContext(buffer);
        assertEquals(new EditorPosition(0, 2), context.caret());
        assertEquals(new EditorSelection(new EditorPosition(0, 1), new EditorPosition(0, 2)), context.selection());
    }

    @Test
    void editsAccumulateAndFireNoEventUntilCommit() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, ""));
        EditorCommandContext context = new EditorCommandContext(buffer);
        List<DocumentTextChangeEvent> events = new ArrayList<>();
        buffer.getDocument().addDocumentChangeListener((DocumentChangeListener) events::add);

        context.insertText(0, "ab");
        context.insertText(2, "cd");
        assertEquals("abcd", buffer.getDocument().getText());
        assertTrue(events.isEmpty());

        context.commit();
        assertEquals(1, events.size());
        assertTrue(events.get(0).isTransactional());
    }

    @Test
    void editsCommitAsSingleUndoableGroup() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, ""));
        EditorCommandContext context = new EditorCommandContext(buffer);
        context.insertText(0, "hello");
        context.insertText(5, " world");
        context.commit();

        assertEquals("hello world", buffer.getDocument().getText());
        buffer.undo();
        assertEquals("", buffer.getDocument().getText());
    }

    @Test
    void rollbackDiscardsEditsAndFiresNoEvent() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, ""));
        EditorCommandContext context = new EditorCommandContext(buffer);
        List<DocumentTextChangeEvent> events = new ArrayList<>();
        buffer.getDocument().addDocumentChangeListener((DocumentChangeListener) events::add);

        context.replaceText(0, 0, "doomed");
        context.rollback();

        assertEquals("", buffer.getDocument().getText());
        assertTrue(events.isEmpty());
        assertFalse(buffer.canUndo());
    }

    @Test
    void caretAndSelectionChangesAreAppliedOnCommit() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, "abc"));
        EditorCommandContext context = new EditorCommandContext(buffer);
        context.moveCaret(new EditorPosition(0, 3));
        context.setSelection(new EditorSelection(new EditorPosition(0, 0), new EditorPosition(0, 3)));

        context.commit();
        context.applyTargetState();

        assertEquals(new EditorPosition(0, 3), buffer.getCaret());
        assertEquals(new EditorSelection(new EditorPosition(0, 0), new EditorPosition(0, 3)), buffer.getSelection());
    }

    @Test
    void transactionIsLazilyCreatedAndShared() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, ""));
        EditorCommandContext context = new EditorCommandContext(buffer);

        DocumentTransaction first = context.transaction();
        DocumentTransaction second = context.transaction();
        assertSame(first, second);
        first.insert(0, "x");
        assertTrue(first.isActive());
        context.commit();
        assertEquals("x", buffer.getDocument().getText());
    }

    @Test
    void deleteAndReplaceOperateWithinTransaction() {
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, "abcdef"));
        EditorCommandContext context = new EditorCommandContext(buffer);
        context.deleteText(2, 4);
        context.replaceText(0, 1, "X");
        context.commit();

        assertEquals("Xbef", buffer.getDocument().getText());
        buffer.undo();
        assertEquals("abcdef", buffer.getDocument().getText());
    }

    @Test
    void contextRejectsNullBuffer() {
        assertThrows(IllegalArgumentException.class, () -> new EditorCommandContext(null));
    }
}
