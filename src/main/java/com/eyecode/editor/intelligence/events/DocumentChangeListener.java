package com.eyecode.editor.intelligence.events;

/**
 * Listener for document text mutations.
 * <p>
 * Implementations are notified synchronously after a mutation is fully applied,
 * so an event always reflects a consistent document state. A single event is
 * delivered per committed transaction regardless of how many edits it grouped.
 */
@FunctionalInterface
public interface DocumentChangeListener {

    void onTextChanged(DocumentTextChangeEvent event);
}
