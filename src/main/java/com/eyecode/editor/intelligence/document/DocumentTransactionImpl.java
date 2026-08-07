package com.eyecode.editor.intelligence.document;

import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.command.CommandManager;
import com.eyecode.editor.v2.command.CompositeEditCommand;
import com.eyecode.editor.v2.command.DeleteTextCommand;
import com.eyecode.editor.v2.command.EditCommand;
import com.eyecode.editor.v2.command.InsertTextCommand;

import java.util.ArrayList;
import java.util.List;

final class DocumentTransactionImpl implements DocumentTransaction {

    private final EditorDocument document;
    private final CommandManager commandManager;
    private final List<EditCommand> edits = new ArrayList<>();
    private boolean begun;
    private boolean batched;
    private boolean finished;

    DocumentTransactionImpl(EditorDocument document, CommandManager commandManager) {
        if (document == null) throw new IllegalArgumentException("document must not be null");
        if (commandManager == null) throw new IllegalArgumentException("commandManager must not be null");
        this.document = document;
        this.commandManager = commandManager;
    }

    @Override
    public void begin() {
        checkNotFinished();
        begun = true;
    }

    @Override
    public void insert(int offset, String text) {
        ensureEditable();
        if (text == null || text.isEmpty()) return;
        validateOffset(offset);
        InsertTextCommand command = new InsertTextCommand(offset, text);
        apply(command);
    }

    @Override
    public void delete(int start, int end) {
        ensureEditable();
        validateRange(start, end);
        if (start == end) return;
        String removed = document.getText().substring(start, end);
        apply(new DeleteTextCommand(start, removed));
    }

    @Override
    public void replace(int start, int end, String text) {
        ensureEditable();
        validateRange(start, end);
        delete(start, end);
        if (text != null && !text.isEmpty()) {
            insert(start, text);
        }
    }

    @Override
    public void replace(TextRange range, String text) {
        if (range == null) throw new IllegalArgumentException("range must not be null");
        replace(range.startOffset(), range.endOffset(), text);
    }

    @Override
    public void commit() {
        checkNotFinished();
        finished = true;
        try {
            if (!edits.isEmpty()) {
                commandManager.recordGroup(new CompositeEditCommand(List.copyOf(edits)));
            }
        } finally {
            finishBatch();
        }
    }

    @Override
    public void rollback() {
        if (finished) return;
        finished = true;
        try {
            for (int i = edits.size() - 1; i >= 0; i--) {
                edits.get(i).undo(document);
            }
        } finally {
            abortBatch();
        }
    }

    @Override
    public boolean isActive() {
        return begun && !finished;
    }

    @Override
    public void close() {
        if (!finished) {
            commit();
        }
    }

    private void apply(EditCommand command) {
        ensureBatchStarted();
        command.execute(document);
        edits.add(command);
    }

    private void ensureBatchStarted() {
        if (!batched) {
            document.beginBatch();
            batched = true;
        }
    }

    private void finishBatch() {
        if (batched) {
            document.endBatch();
            batched = false;
        }
    }

    private void abortBatch() {
        if (batched) {
            document.abortBatch();
            batched = false;
        }
    }

    private void ensureEditable() {
        checkNotFinished();
        if (!begun) {
            begin();
        }
    }

    private void checkNotFinished() {
        if (finished) {
            throw new IllegalStateException("Transaction is already finished");
        }
    }

    private void validateOffset(int offset) {
        if (offset < 0 || offset > document.length()) {
            throw new IndexOutOfBoundsException("Offset out of range: " + offset);
        }
    }

    private void validateRange(int start, int end) {
        if (start < 0 || end < start || end > document.length()) {
            throw new IndexOutOfBoundsException("Invalid range: " + start + ".." + end);
        }
    }
}
