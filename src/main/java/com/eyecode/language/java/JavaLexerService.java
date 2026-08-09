package com.eyecode.language.java;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.language.java.incremental.FullRelexStrategy;
import com.eyecode.language.java.incremental.IncrementalJavaLexer;
import com.eyecode.language.java.incremental.IncrementalLexResult;

/**
 * {@link LexerService} implementation backed by the per-session
 * {@link LexerCache} on top of the incremental Java lexer.
 * <p>
 * Every lex is bound to the version of the given snapshot: a snapshot lexed
 * out of order produces a stale-versioned result that can never be mistaken
 * for — or overwrite — a newer cached state ("newest wins"). Snapshots without
 * a session identity are anonymous one-shots and are always full-re-lexed,
 * never cached (version equality cannot be trusted without a session).
 * <p>
 * The cache is internal infrastructure: consumers see only
 * {@link LexerSnapshot} and this class stays the facade.
 */
public final class JavaLexerService implements LexerService {

    private final LexerCache cache;
    private final IncrementalJavaLexer incrementalLexer = new IncrementalJavaLexer();
    private final FullRelexStrategy fullRelex = new FullRelexStrategy();

    public JavaLexerService() {
        this(new LexerCache());
    }

    /**
     * Creates a service over an explicit cache (testability/observability).
     */
    public JavaLexerService(LexerCache cache) {
        if (cache == null) {
            throw new IllegalArgumentException("cache must not be null");
        }
        this.cache = cache;
    }

    @Override
    public LexerSnapshot lex(DocumentSnapshot document) {
        if (document == null) {
            throw new IllegalArgumentException("document must not be null");
        }
        String sessionId = document.sessionId();
        if (sessionId == null) {
            return fullRelex.lex(document.getText(), document.version());
        }
        long version = document.version();
        LexerCacheEntry entry = cache.get(sessionId, version);
        if (entry == null) {
            LexerSnapshot snapshot = fullRelex.lex(document.getText(), version);
            cache.put(sessionId, newEntry(sessionId, version, snapshot, document.getText()));
            return snapshot;
        }
        if (entry.version() == version) {
            return entry.snapshot();
        }
        if (entry.version() < version) {
            LexerSnapshot snapshot = incremental(entry, document.getText(), version);
            cache.put(sessionId, newEntry(sessionId, version, snapshot, document.getText()));
            return snapshot;
        }
        return fullRelex.lex(document.getText(), version);
    }

    /**
     * Removes the cached state of a closed session (document closed).
     */
    public void invalidateSession(String sessionId) {
        if (sessionId != null) {
            cache.invalidate(sessionId);
        }
    }

    /**
     * Marks a session ACTIVE (heavy incremental state restored on demand by
     * the next lex).
     */
    public void activateSession(String sessionId) {
        if (sessionId != null) {
            cache.activate(sessionId);
        }
    }

    /**
     * Marks a session INACTIVE and releases its heavy incremental structures.
     */
    public void deactivateSession(String sessionId) {
        if (sessionId != null) {
            cache.deactivate(sessionId);
        }
    }

    public LexerCacheStats cacheStats() {
        return cache.stats();
    }

    private LexerSnapshot incremental(LexerCacheEntry entry, String newText, long newVersion) {
        if (!entry.hasIncrementalSlot()) {
            return fullRelex.lex(newText, newVersion);
        }
        IncrementalLexResult result = incrementalLexer.lex(
                entry.previousText(), entry.snapshot(), newText, newVersion);
        return result.snapshot();
    }

    private static LexerCacheEntry newEntry(String sessionId, long version,
                                            LexerSnapshot snapshot, String previousText) {
        return new LexerCacheEntry(sessionId, version, snapshot, previousText,
                LexerSessionState.ACTIVE);
    }
}
