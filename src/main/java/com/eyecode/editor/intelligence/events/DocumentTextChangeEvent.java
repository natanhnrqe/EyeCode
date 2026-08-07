package com.eyecode.editor.intelligence.events;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextChange;
import com.eyecode.eventbus.Event;

/**
 * Published whenever a document mutation is committed.
 * <p>
 * Carries the immutable snapshot before the change, the snapshot after the
 * change, and the minimal {@link TextChange} describing the mutation. When
 * the change originated from a {@code DocumentTransaction} the
 * {@link #transactional()} flag is set, signalling that undo history was
 * already recorded for the grouped edit.
 */
public final class DocumentTextChangeEvent implements Event {

    private final DocumentSnapshot before;
    private final DocumentSnapshot after;
    private final TextChange change;
    private final boolean transactional;

    public DocumentTextChangeEvent(DocumentSnapshot before,
                                   DocumentSnapshot after,
                                   TextChange change,
                                   boolean transactional) {
        this.before = before;
        this.after = after;
        this.change = change;
        this.transactional = transactional;
    }

    public DocumentSnapshot getBefore() {
        return before;
    }

    public DocumentSnapshot getAfter() {
        return after;
    }

    public TextChange getChange() {
        return change;
    }

    public boolean isTransactional() {
        return transactional;
    }
}
