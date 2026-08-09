package com.eyecode.language;

import com.eyecode.language.java.LexerCache;
import com.eyecode.language.java.LexerCacheEntry;
import com.eyecode.language.java.LexerCacheStats;
import com.eyecode.language.java.LexerSessionState;
import com.eyecode.language.java.LexerSnapshot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LexerCacheTest {

    private static LexerSnapshot snapshot(long version) {
        return new LexerSnapshot(version, new com.eyecode.language.java.incremental.FullRelexStrategy()
                .lex("class A {}", version).tokens());
    }

    private static LexerCacheEntry entry(String sessionId, long version, LexerSessionState state) {
        return new LexerCacheEntry(sessionId, version, snapshot(version),
                state == LexerSessionState.ACTIVE ? "class A {}" : null, state);
    }

    @Test
    void getOnEmptyCacheIsMiss() {
        LexerCache cache = new LexerCache(10);

        assertNull(cache.get("s1", 1));
        LexerCacheStats stats = cache.stats();
        assertEquals(0, stats.hitCount());
        assertEquals(1, stats.missCount());
        assertEquals(0, stats.evictionCount());
        assertEquals(0, stats.invalidationCount());
    }

    @Test
    void putThenGetSameVersionIsHit() {
        LexerCache cache = new LexerCache(10);
        cache.put("s1", entry("s1", 1, LexerSessionState.ACTIVE));

        LexerCacheEntry found = cache.get("s1", 1);

        assertNotNull(found);
        assertEquals(1, found.version());
        assertEquals(1, cache.size());
        assertEquals(1, cache.stats().hitCount());
        assertEquals(0, cache.stats().missCount());
    }

    @Test
    void getWithDifferentVersionIsMiss() {
        LexerCache cache = new LexerCache(10);
        cache.put("s1", entry("s1", 1, LexerSessionState.ACTIVE));

        LexerCacheEntry found = cache.get("s1", 2);

        assertNotNull(found, "entry is returned so the service can full-re-lex stale versions");
        assertEquals(1, found.version());
        assertEquals(0, cache.stats().hitCount());
        assertEquals(1, cache.stats().missCount());
    }

    @Test
    void invalidateRemovesEntryAndCounts() {
        LexerCache cache = new LexerCache(10);
        cache.put("s1", entry("s1", 1, LexerSessionState.ACTIVE));
        cache.put("s2", entry("s2", 1, LexerSessionState.ACTIVE));

        cache.invalidate("s1");
        cache.invalidate("s1");

        assertEquals(1, cache.size());
        assertNull(cache.get("s1", 1));
        assertEquals(1, cache.stats().invalidationCount());
    }

    @Test
    void removeDropsEntryWithoutCountingInvalidation() {
        LexerCache cache = new LexerCache(10);
        cache.put("s1", entry("s1", 1, LexerSessionState.ACTIVE));

        cache.remove("s1");

        assertEquals(0, cache.size());
        assertEquals(0, cache.stats().invalidationCount());
    }

    @Test
    void activateAndDeactivateFlipState() {
        LexerCache cache = new LexerCache(10);
        cache.put("s1", entry("s1", 1, LexerSessionState.ACTIVE));

        cache.deactivate("s1");
        assertEquals(LexerSessionState.INACTIVE, cache.get("s1", 1).state());

        cache.activate("s1");
        assertEquals(LexerSessionState.ACTIVE, cache.get("s1", 1).state());
    }

    @Test
    void deactivateReleasesIncrementalSlotButKeepsSnapshot() {
        LexerCache cache = new LexerCache(10);
        cache.put("s1", entry("s1", 1, LexerSessionState.ACTIVE));

        cache.deactivate("s1");

        LexerCacheEntry inactive = cache.get("s1", 1);
        assertEquals(LexerSessionState.INACTIVE, inactive.state());
        assertNull(inactive.previousText());
        assertFalse(inactive.hasIncrementalSlot());
        assertNotNull(inactive.snapshot());
        assertEquals(1, inactive.version());
    }

    @Test
    void activateRestoresSlotEligibility() {
        LexerCache cache = new LexerCache(10);
        cache.put("s1", entry("s1", 1, LexerSessionState.ACTIVE));
        cache.deactivate("s1");
        cache.activate("s1");

        assertEquals(LexerSessionState.ACTIVE, cache.get("s1", 1).state());
    }

    @Test
    void sizeTracksEntries() {
        LexerCache cache = new LexerCache(10);
        assertEquals(0, cache.size());

        cache.put("s1", entry("s1", 1, LexerSessionState.ACTIVE));
        cache.put("s2", entry("s2", 1, LexerSessionState.ACTIVE));

        assertEquals(2, cache.size());
    }

    @Test
    void limitEvictsLeastRecentlyUsedActiveEntry() {
        LexerCache cache = new LexerCache(2);
        cache.put("a", entry("a", 1, LexerSessionState.ACTIVE));
        cache.put("b", entry("b", 1, LexerSessionState.ACTIVE));
        cache.get("a", 1);
        cache.put("c", entry("c", 1, LexerSessionState.ACTIVE));

        assertEquals(2, cache.size());
        assertNull(cache.get("b", 1));
        assertNotNull(cache.get("a", 1));
        assertNotNull(cache.get("c", 1));
        assertEquals(1, cache.stats().evictionCount());
    }

    @Test
    void evictionPrefersInactiveOverActive() {
        LexerCache cache = new LexerCache(2);
        cache.put("active", entry("active", 1, LexerSessionState.ACTIVE));
        cache.put("inactive", entry("inactive", 1, LexerSessionState.ACTIVE));
        cache.deactivate("inactive");
        cache.put("new", entry("new", 1, LexerSessionState.ACTIVE));

        assertEquals(2, cache.size());
        assertNull(cache.get("inactive", 1), "INACTIVE entry must be evicted first");
        assertNotNull(cache.get("active", 1));
        assertNotNull(cache.get("new", 1));
        assertEquals(1, cache.stats().evictionCount());
    }

    @Test
    void evictionPrefersOldestInactive() {
        LexerCache cache = new LexerCache(2);
        cache.put("old", entry("old", 1, LexerSessionState.ACTIVE));
        cache.put("recent", entry("recent", 1, LexerSessionState.ACTIVE));
        cache.deactivate("old");
        cache.deactivate("recent");
        cache.put("new", entry("new", 1, LexerSessionState.ACTIVE));

        assertNull(cache.get("old", 1), "oldest INACTIVE must be evicted first");
        assertNotNull(cache.get("recent", 1));
    }

    @Test
    void hitDoesNotChangeVersionOrState() {
        LexerCache cache = new LexerCache(10);
        cache.put("s1", entry("s1", 4, LexerSessionState.INACTIVE));

        LexerCacheEntry initial = cache.get("s1", 4);
        cache.get("s1", 4);

        assertEquals(4, initial.version(), "a hit must never change the stored version");
        assertEquals(LexerSessionState.INACTIVE, initial.state(),
                "a hit must never change the stored state");
        assertNull(initial.previousText());
        assertEquals(2, cache.stats().hitCount());
        assertEquals(0, cache.stats().missCount());
    }

    @Test
    void putNeverRegressesToAnOlderVersion() {
        LexerCache cache = new LexerCache(10);
        cache.put("s1", entry("s1", 5, LexerSessionState.ACTIVE));

        cache.put("s1", entry("s1", 3, LexerSessionState.ACTIVE));

        assertEquals(5, cache.get("s1", 5).version());
    }

    @Test
    void newestWinsChainPutV1PutV2PutStaleV1() {
        LexerCache cache = new LexerCache(10);
        cache.put("s1", entry("s1", 1, LexerSessionState.ACTIVE));
        cache.put("s1", entry("s1", 2, LexerSessionState.ACTIVE));
        cache.put("s1", entry("s1", 1, LexerSessionState.ACTIVE));

        LexerCacheEntry current = cache.get("s1", 2);
        assertNotNull(current, "cache must still contain v2 after a stale v1 put");
        assertEquals(2, current.version());
        assertEquals("class A {}", current.previousText());
    }

    @Test
    void statsAreMonotonicAndIndependentSnapshots() {
        LexerCache cache = new LexerCache(10);
        cache.put("s1", entry("s1", 1, LexerSessionState.ACTIVE));
        LexerCacheStats before = cache.stats();
        cache.get("s1", 1);
        LexerCacheStats after = cache.stats();

        assertTrue(after.hitCount() >= before.hitCount());
        assertTrue(after.missCount() >= before.missCount());
        assertEquals(1, after.hitCount());
    }

    @Test
    void rejectsNullSessionIds() {
        LexerCache cache = new LexerCache(10);

        assertThrows(IllegalArgumentException.class, () -> cache.get(null, 1));
        assertThrows(IllegalArgumentException.class, () -> cache.put(null, entry("s1", 1, LexerSessionState.ACTIVE)));
        assertThrows(IllegalArgumentException.class, () -> cache.invalidate(null));
        assertThrows(IllegalArgumentException.class, () -> cache.remove(null));
        assertThrows(IllegalArgumentException.class, () -> cache.activate(null));
        assertThrows(IllegalArgumentException.class, () -> cache.deactivate(null));
    }

    @Test
    void rejectsInvalidLimits() {
        assertThrows(IllegalArgumentException.class, () -> new LexerCache(0));
        assertThrows(IllegalArgumentException.class, () -> new LexerCache(-5));
    }

    @Test
    void defaultLimitIsExplicit() {
        assertEquals(100, LexerCache.DEFAULT_MAX_ENTRIES);
    }
}
