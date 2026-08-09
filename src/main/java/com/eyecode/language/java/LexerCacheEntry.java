package com.eyecode.language.java;

/**
 * Immutable value stored in the {@link LexerCache} for one document/session.
 * <p>
 * A record by construction: all fields are immutable, so exposing an entry
 * never hands out mutable internal state. The incremental slot is
 * ({@code previousText} + {@code snapshot}) — the exact state the
 * {@link com.eyecode.language.java.incremental.IncrementalJavaLexer} needs to
 * continue the re-lex chain. INACTIVE entries drop the slot
 * ({@code previousText} is null) and keep only the minimal metadata.
 *
 * @param sessionId    identity of the document/session
 * @param version      last known document version
 * @param snapshot     last lexical snapshot for {@code version}
 * @param previousText text the {@code snapshot} was produced from (incremental
 *                     slot; null when INACTIVE or not yet established)
 * @param state        ACTIVE or INACTIVE
 */
public record LexerCacheEntry(String sessionId,
                              long version,
                              LexerSnapshot snapshot,
                              String previousText,
                              LexerSessionState state) {

    public LexerCacheEntry {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot must not be null");
        }
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
    }

    public boolean hasIncrementalSlot() {
        return previousText != null;
    }

    public LexerCacheEntry asInactive() {
        return new LexerCacheEntry(sessionId, version, snapshot, null, LexerSessionState.INACTIVE);
    }

    public LexerCacheEntry asActive() {
        return new LexerCacheEntry(sessionId, version, snapshot, previousText, LexerSessionState.ACTIVE);
    }
}
