package com.eyecode.editor.intelligence.document;

/**
 * Source of immutable {@link DocumentSnapshot} instances for a document.
 * <p>
 * The document itself is the canonical provider; every mutation advances the
 * version and the next {@link #snapshot()} reflects the new state.
 */
public interface DocumentSnapshotProvider {

    DocumentSnapshot snapshot();

    long currentVersion();
}
