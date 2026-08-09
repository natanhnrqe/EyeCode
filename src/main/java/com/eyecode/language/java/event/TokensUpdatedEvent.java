package com.eyecode.language.java.event;

import com.eyecode.eventbus.Event;
import com.eyecode.language.java.LexerSnapshot;

/**
 * Published when a new lexical view of a document is available.
 * <p>
 * Carries the immutable {@link LexerSnapshot} bound to the exact document
 * version that was analyzed. The event means "a fresh lexical view exists" —
 * it is not a document mutation notification; {@code DocumentTextChangeEvent}
 * remains the source of truth for mutations.
 */
public final class TokensUpdatedEvent implements Event {

    private final LexerSnapshot snapshot;

    public TokensUpdatedEvent(LexerSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot must not be null");
        }
        this.snapshot = snapshot;
    }

    public LexerSnapshot getSnapshot() {
        return snapshot;
    }

    public long getVersion() {
        return snapshot.version();
    }
}
