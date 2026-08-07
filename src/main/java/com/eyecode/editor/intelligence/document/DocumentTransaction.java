package com.eyecode.editor.intelligence.document;

import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.command.CommandManager;

/**
 * Groups multiple edits into one atomic unit of work.
 * <p>
 * While a transaction is active its edits are applied to the document without
 * firing intermediate change events. On {@link #commit()} a single undo entry
 * is recorded and exactly one merged {@code DocumentTextChangeEvent} is fired.
 * On {@link #rollback()} all applied edits are reverted and no event is fired.
 */
public interface DocumentTransaction extends AutoCloseable {

    static DocumentTransaction open(EditorDocument document, CommandManager commandManager) {
        return new DocumentTransactionImpl(document, commandManager);
    }

    void begin();

    void insert(int offset, String text);

    void delete(int start, int end);

    void replace(int start, int end, String text);

    void replace(TextRange range, String text);

    void commit();

    void rollback();

    boolean isActive();

    @Override
    void close();
}
