package com.eyecode.language.java;

/**
 * Immutable snapshot of the {@link LexerCache} counters.
 * <p>
 * All values are monotonic: counters only ever increase, so a stats snapshot
 * taken earlier never reports more work than a later one.
 */
public record LexerCacheStats(long hitCount,
                              long missCount,
                              long evictionCount,
                              long invalidationCount) {

    public long totalRequests() {
        return hitCount + missCount;
    }
}
