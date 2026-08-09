package com.eyecode.language.java;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bounded, per-session lexer cache.
 * <p>
 * Internal infrastructure of the {@code language/java} layer — consumers must
 * never touch it; the only gate for tokens stays {@link LexerService}. Each
 * entry holds the last known version, the {@link LexerSnapshot} and the
 * incremental slot; entries are keyed by a session identity (never by file
 * path alone — the same file can have several sessions).
 * <p>
 * The cache is bounded ({@link #DEFAULT_MAX_ENTRIES} by default, configurable)
 * and evicts the least recently used entry when full, preferring an INACTIVE
 * entry (oldest first) over ACTIVE ones. No background thread: eviction runs
 * inline on {@link #put}. Versioned entries never regress: a put with an older
 * version than the cached one is ignored, so "newest wins" under any
 * interleaving. All operations are {@code synchronized} — simple and safe, no
 * executor/thread pool.
 * <p>
 * Simple monotonic counters are kept for observability
 * ({@link LexerCacheStats}); there are no cache events — consumers keep
 * observing {@link TokensUpdatedEvent} only.
 */
public final class LexerCache {

    public static final int DEFAULT_MAX_ENTRIES = 100;

    private final int maxEntries;
    private final Map<String, Slot> entries = new LinkedHashMap<>();
    private long accessCounter;
    private long hitCount;
    private long missCount;
    private long evictionCount;
    private long invalidationCount;

    public LexerCache() {
        this(DEFAULT_MAX_ENTRIES);
    }

    public LexerCache(int maxEntries) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be positive: " + maxEntries);
        }
        this.maxEntries = maxEntries;
    }

    /**
     * Returns the entry for {@code sessionId} (any version) or {@code null}
     * when absent. Counts a hit when an entry exists for the exact version,
     * otherwise a miss. Refreshes LRU recency on access.
     */
    public synchronized LexerCacheEntry get(String sessionId, long version) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }
        Slot slot = entries.get(sessionId);
        if (slot == null) {
            missCount++;
            return null;
        }
        slot.lastAccess = ++accessCounter;
        if (slot.entry.version() == version) {
            hitCount++;
        } else {
            missCount++;
        }
        return slot.entry;
    }

    /**
     * Stores an entry. A put whose version is older than the cached one is
     * ignored — the newest version always wins.
     */
    public synchronized void put(String sessionId, LexerCacheEntry entry) {
        if (sessionId == null || entry == null) {
            throw new IllegalArgumentException("sessionId and entry must not be null");
        }
        Slot existing = entries.get(sessionId);
        if (existing != null && existing.entry.version() > entry.version()) {
            return;
        }
        entries.put(sessionId, new Slot(entry, ++accessCounter));
        evictWhileOverLimit();
    }

    /**
     * Removes the entry and counts an invalidation (session removed,
     * document closed).
     */
    public synchronized void invalidate(String sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }
        if (entries.remove(sessionId) != null) {
            invalidationCount++;
        }
    }

    /**
     * Removes the entry without counting an invalidation.
     */
    public synchronized void remove(String sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }
        entries.remove(sessionId);
    }

    /**
     * Marks the session ACTIVE. The incremental slot is rebuilt on demand by
     * the next lex — no lex runs here.
     */
    public synchronized void activate(String sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }
        Slot slot = entries.get(sessionId);
        if (slot != null) {
            slot.entry = slot.entry.asActive();
            slot.lastAccess = ++accessCounter;
        }
    }

    /**
     * Marks the session INACTIVE and releases the heavy incremental structures
     * (the previous text); only the snapshot and version metadata are kept.
     */
    public synchronized void deactivate(String sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }
        Slot slot = entries.get(sessionId);
        if (slot != null) {
            slot.entry = slot.entry.asInactive();
            slot.lastAccess = ++accessCounter;
        }
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized LexerCacheStats stats() {
        return new LexerCacheStats(hitCount, missCount, evictionCount, invalidationCount);
    }

    private void evictWhileOverLimit() {
        while (entries.size() > maxEntries) {
            evictOne();
        }
    }

    private void evictOne() {
        String victimId = null;
        long victimAccess = Long.MAX_VALUE;
        boolean victimInactive = false;
        for (Map.Entry<String, Slot> e : entries.entrySet()) {
            Slot slot = e.getValue();
            boolean inactive = slot.entry.state() == LexerSessionState.INACTIVE;
            if (victimId == null) {
                victimId = e.getKey();
                victimAccess = slot.lastAccess;
                victimInactive = inactive;
                continue;
            }
            if (inactive && !victimInactive) {
                victimId = e.getKey();
                victimAccess = slot.lastAccess;
                victimInactive = true;
            } else if (inactive == victimInactive && slot.lastAccess < victimAccess) {
                victimId = e.getKey();
                victimAccess = slot.lastAccess;
            }
        }
        entries.remove(victimId);
        evictionCount++;
    }

    private static final class Slot {
        private LexerCacheEntry entry;
        private long lastAccess;

        private Slot(LexerCacheEntry entry, long lastAccess) {
            this.entry = entry;
            this.lastAccess = lastAccess;
        }
    }
}
