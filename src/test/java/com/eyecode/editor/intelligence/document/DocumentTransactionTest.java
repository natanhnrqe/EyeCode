package com.eyecode.editor.intelligence.document;

import com.eyecode.editor.intelligence.events.DocumentChangeListener;
import com.eyecode.editor.intelligence.events.DocumentTextChangeEvent;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.command.CommandManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentTransactionTest {

    private static final class EventRecorder {
        private final List<DocumentTextChangeEvent> events = new ArrayList<>();

        DocumentChangeRecorder on(EditorDocument document) {
            DocumentChangeRecorder recorder = new DocumentChangeRecorder(this);
            document.addDocumentChangeListener(recorder);
            return recorder;
        }
    }

    private record DocumentChangeRecorder(EventRecorder owner) implements DocumentChangeListener {
        @Override
        public void onTextChanged(DocumentTextChangeEvent event) {
            owner.events.add(event);
        }
    }

    private static DocumentTransactionTestState state(String initial) {
        EditorDocument document = new EditorDocument(null, initial);
        EditorBuffer buffer = new EditorBuffer(document);
        return new DocumentTransactionTestState(document, buffer.getCommandManager(), buffer);
    }

    private record DocumentTransactionTestState(EditorDocument document,
                                                CommandManager commandManager,
                                                EditorBuffer buffer) {
    }

    @Test
    void commitAppliesAllEditsAndFiresSingleMergedEvent() {
        DocumentTransactionTestState s = state("");
        EventRecorder recorder = new EventRecorder();
        recorder.on(s.document());

        try (DocumentTransaction tx = DocumentTransaction.open(s.document(), s.commandManager())) {
            tx.insert(0, "Hello");
            tx.insert(5, " World");
            tx.insert(11, "!");
            tx.commit();
        }

        assertEquals("Hello World!", s.document().getText());
        assertEquals(1, recorder.events.size());
        DocumentTextChangeEvent event = recorder.events.get(0);
        assertTrue(event.isTransactional());
        assertEquals("", event.getBefore().getText());
        assertEquals("Hello World!", event.getAfter().getText());
    }

    @Test
    void committedTransactionUndoesAsSingleGroup() {
        DocumentTransactionTestState s = state("");
        try (DocumentTransaction tx = DocumentTransaction.open(s.document(), s.commandManager())) {
            tx.insert(0, "Hello");
            tx.insert(5, " World");
            tx.commit();
        }

        s.buffer().undo();
        assertEquals("", s.document().getText());
        s.buffer().redo();
        assertEquals("Hello World", s.document().getText());
    }

    @Test
    void rollbackRevertsAllEditsAndFiresNoEvent() {
        DocumentTransactionTestState s = state("");
        EventRecorder recorder = new EventRecorder();
        recorder.on(s.document());

        DocumentTransaction tx = DocumentTransaction.open(s.document(), s.commandManager());
        tx.begin();
        tx.insert(0, "doomed");
        tx.rollback();

        assertEquals("", s.document().getText());
        assertTrue(recorder.events.isEmpty());
        assertFalse(s.buffer().canUndo());
    }

    @Test
    void closeCommitsOutstandingEdits() {
        DocumentTransactionTestState s = state("");
        try (DocumentTransaction tx = DocumentTransaction.open(s.document(), s.commandManager())) {
            tx.insert(0, "autoclosed");
        }
        assertEquals("autoclosed", s.document().getText());
        assertTrue(s.buffer().canUndo());
    }

    @Test
    void emptyCommitRecordsNoHistoryAndFiresNoEvent() {
        DocumentTransactionTestState s = state("");
        EventRecorder recorder = new EventRecorder();
        recorder.on(s.document());

        DocumentTransaction tx = DocumentTransaction.open(s.document(), s.commandManager());
        tx.begin();
        tx.commit();

        assertFalse(s.buffer().canUndo());
        assertTrue(recorder.events.isEmpty());
    }

    @Test
    void editsBeforeExplicitBeginAutoBegin() {
        DocumentTransactionTestState s = state("");
        try (DocumentTransaction tx = DocumentTransaction.open(s.document(), s.commandManager())) {
            tx.insert(0, "abc");
            tx.insert(3, "def");
            tx.commit();
        }
        assertEquals("abcdef", s.document().getText());
        s.buffer().undo();
        assertEquals("", s.document().getText());
    }

    @Test
    void operationsAfterFinishThrow() {
        DocumentTransactionTestState s = state("");
        DocumentTransaction tx = DocumentTransaction.open(s.document(), s.commandManager());
        tx.begin();
        tx.commit();

        assertFalse(tx.isActive());
        assertThrows(IllegalStateException.class, tx::commit);
        assertThrows(IllegalStateException.class, () -> tx.insert(0, "x"));
    }

    @Test
    void rollbackAfterCommitIsNoOp() {
        DocumentTransactionTestState s = state("");
        DocumentTransaction tx = DocumentTransaction.open(s.document(), s.commandManager());
        tx.insert(0, "data");
        tx.commit();
        tx.rollback();
        assertEquals("data", s.document().getText());
    }

    @Test
    void transactionInterleavedWithNormalEditsKeepsGroupedUndo() {
        DocumentTransactionTestState s = state("");
        s.document().insert(0, "prefix ");
        try (DocumentTransaction tx = DocumentTransaction.open(s.document(), s.commandManager())) {
            tx.insert(7, "Hello");
            tx.insert(12, " World");
            tx.commit();
        }
        s.document().insert(18, " suffix");

        assertEquals("prefix Hello World suffix", s.document().getText());
        s.buffer().undo();
        assertEquals("prefix Hello World", s.document().getText());
    }

    @Test
    void replaceRangeDeletesAndInsertsInOneUnit() {
        DocumentTransactionTestState s = state("before after");
        try (DocumentTransaction tx = DocumentTransaction.open(s.document(), s.commandManager())) {
            tx.replace(new TextRange(0, 6), "middle");
            tx.commit();
        }
        assertEquals("middle after", s.document().getText());
        s.buffer().undo();
        assertEquals("before after", s.document().getText());
    }
}
