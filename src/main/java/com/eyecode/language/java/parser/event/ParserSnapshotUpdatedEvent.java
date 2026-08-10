package com.eyecode.language.java.parser.event;

import com.eyecode.eventbus.Event;
import com.eyecode.language.java.parser.ParserSnapshot;

/**
 * Published when a new syntactic view of a document is available.
 * <p>
 * Carries the immutable {@link ParserSnapshot} bound to the exact document
 * version that was analyzed. The event means "a fresh parse exists" — it
 * is not a document mutation notification;
 * {@code DocumentTextChangeEvent} remains the source of truth for mutations.
 * <p>
 * Newest-wins: a {@link com.eyecode.language.java.parser.JavaParserService}
 * that publishes an older {@link ParserSnapshot} after a newer one will be
 * detected and filtered by the parser event bridge — consumers never see
 * snapshots out of version order.
 */
public final class ParserSnapshotUpdatedEvent implements Event {

    private final ParserSnapshot snapshot;

    public ParserSnapshotUpdatedEvent(ParserSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot must not be null");
        }
        this.snapshot = snapshot;
    }

    public ParserSnapshot getSnapshot() {
        return snapshot;
    }

    public long getVersion() {
        return snapshot.version();
    }
}
