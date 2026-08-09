package com.eyecode.language;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.LexerCache;
import com.eyecode.language.java.LexerCacheEntry;
import com.eyecode.language.java.LexerSnapshot;
import com.eyecode.language.java.LexerSessionState;
import com.eyecode.language.java.incremental.FullRelexStrategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaLexerServiceCacheTest {

    private final FullRelexStrategy full = new FullRelexStrategy();

    private static DocumentSnapshot snapshot(String sessionId, long version, String text) {
        return new DocumentSnapshot(version, text, null, null, sessionId);
    }

    @Test
    void firstLexIsMiss() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = new JavaLexerService(cache);

        service.lex(snapshot("s1", 1, "class A {}"));

        assertEquals(0, service.cacheStats().hitCount());
        assertEquals(1, service.cacheStats().missCount());
        assertEquals(1, cache.size());
    }

    @Test
    void secondLexSameVersionIsHit() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = new JavaLexerService(cache);

        service.lex(snapshot("s1", 1, "class A {}"));
        service.lex(snapshot("s1", 1, "class A {}"));

        assertEquals(1, service.cacheStats().hitCount());
        assertEquals(1, service.cacheStats().missCount());
    }

    @Test
    void sameVersionReusesTheCachedSnapshot() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = new JavaLexerService(cache);

        LexerSnapshot first = service.lex(snapshot("s1", 1, "class A {}"));
        LexerSnapshot second = service.lex(snapshot("s1", 1, "class A {}"));

        assertSame(first, second);
        assertEquals(full.lex("class A {}", 1), second);
    }

    @Test
    void newVersionUpdatesEntryViaIncrementalPath() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = new JavaLexerService(cache);

        service.lex(snapshot("s1", 1, "class A {}"));
        LexerSnapshot v2 = service.lex(snapshot("s1", 2, "class AB {}"));

        assertEquals(full.lex("class AB {}", 2), v2);
        LexerCacheEntry entry = cache.get("s1", 2);
        assertEquals(2, entry.version());
        assertTrue(entry.hasIncrementalSlot());
        assertEquals(LexerSessionState.ACTIVE, entry.state());
    }

    @Test
    void staleSnapshotNeverReplacesNewerCachedState() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = new JavaLexerService(cache);

        service.lex(snapshot("s1", 1, "class A {}"));
        service.lex(snapshot("s1", 2, "class AB {}"));
        service.lex(snapshot("s1", 3, "class ABC {}"));

        LexerSnapshot stale = service.lex(snapshot("s1", 1, "class A {}"));

        assertEquals(1, stale.version());
        assertEquals(full.lex("class A {}", 1), stale);
        assertEquals(3, cache.get("s1", 3).version(), "cache must stay on the newest version");
        assertEquals(full.lex("class ABC {}", 3), service.lex(snapshot("s1", 3, "class ABC {}")));
    }

    @Test
    void independentSessionsHaveIndependentCache() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = new JavaLexerService(cache);

        LexerSnapshot a = service.lex(snapshot("sA", 1, "class Alpha {}"));
        LexerSnapshot b = service.lex(snapshot("sB", 1, "class Beta {}"));

        assertEquals(full.lex("class Alpha {}", 1), a);
        assertEquals(full.lex("class Beta {}", 1), b);
        assertEquals(2, cache.size());
        assertEquals(2, service.cacheStats().missCount());
    }

    @Test
    void anonymousSnapshotsAreNeverCached() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = new JavaLexerService(cache);

        LexerSnapshot v1 = service.lex(new DocumentSnapshot(1, "class A {}", null, null));
        LexerSnapshot v2 = service.lex(new DocumentSnapshot(2, "class B {}", null, null));
        LexerSnapshot v1Again = service.lex(new DocumentSnapshot(1, "class A {}", null, null));

        assertEquals(full.lex("class A {}", 1), v1);
        assertEquals(full.lex("class B {}", 2), v2);
        assertEquals(full.lex("class A {}", 1), v1Again);
        assertEquals(0, cache.size());
        assertEquals(0, service.cacheStats().totalRequests(), "anonymous lexes must bypass the cache");
    }

    @Test
    void hundredSessionsRespectTheLimit() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = new JavaLexerService(cache);

        for (int i = 0; i < 100; i++) {
            service.lex(snapshot("s" + i, 1, "class C" + i + " {}"));
        }

        assertTrue(cache.size() <= LexerCache.DEFAULT_MAX_ENTRIES,
                "cache size " + cache.size() + " exceeds limit " + LexerCache.DEFAULT_MAX_ENTRIES);
        assertEquals(100, cache.size());
        assertEquals(0, service.cacheStats().evictionCount());
    }

    @Test
    void moreSessionsThanLimitEvictsOldest() {
        LexerCache cache = new LexerCache(50);
        JavaLexerService service = new JavaLexerService(cache);

        for (int i = 0; i < 100; i++) {
            service.lex(snapshot("s" + i, 1, "class C" + i + " {}"));
        }

        assertEquals(50, cache.size());
        assertTrue(service.cacheStats().evictionCount() > 0);
    }

    @Test
    void deactivatedSessionsReleaseIncrementalStructures() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = new JavaLexerService(cache);

        service.lex(snapshot("s1", 1, "class A {}"));
        service.deactivateSession("s1");

        LexerCacheEntry entry = cache.get("s1", 1);
        assertEquals(LexerSessionState.INACTIVE, entry.state());
        assertFalse(entry.hasIncrementalSlot());
        assertNull(entry.previousText());
        assertNotNull(entry.snapshot());

        service.activateSession("s1");
        assertEquals(LexerSessionState.ACTIVE, cache.get("s1", 1).state());
    }

    @Test
    void invalidateSessionRemovesEntry() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = new JavaLexerService(cache);

        service.lex(snapshot("s1", 1, "class A {}"));
        service.invalidateSession("s1");

        assertEquals(0, cache.size());
        assertEquals(1, service.cacheStats().invalidationCount());
    }

    @Test
    void missExecutesLexAndProducesTheFullResult() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = new JavaLexerService(cache);

        LexerSnapshot result = service.lex(snapshot("s1", 1, "class A {}"));

        assertEquals(full.lex("class A {}", 1), result);
        assertEquals(1, service.cacheStats().missCount());
    }

    @Test
    void sameTextInDifferentSessionsSharesNoMutableIncrementalState() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = new JavaLexerService(cache);

        service.lex(snapshot("sA", 1, "class A {}"));
        service.lex(snapshot("sB", 1, "class A {}"));

        LexerSnapshot a2 = service.lex(snapshot("sA", 2, "class AB {}"));
        LexerSnapshot b1Again = service.lex(snapshot("sB", 1, "class A {}"));

        assertEquals(full.lex("class AB {}", 2), a2, "sA advanced through its own slot");
        assertEquals(full.lex("class A {}", 1), b1Again, "sB untouched by sA mutations");
        assertEquals(2, cache.size());
        LexerCacheEntry aEntry = cache.get("sA", 2);
        LexerCacheEntry bEntry = cache.get("sB", 1);
        assertTrue(aEntry.hasIncrementalSlot());
        assertTrue(bEntry.hasIncrementalSlot());
        assertNotSame(aEntry.snapshot(), bEntry.snapshot(),
                "sessions must never share snapshot instances");
    }

    @Test
    void versionedChainPutV1PutV2PutStaleV1KeepsV2() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = new JavaLexerService(cache);

        service.lex(snapshot("s1", 1, "class A {}"));
        service.lex(snapshot("s1", 2, "class AB {}"));
        service.lex(snapshot("s1", 1, "class A {}"));

        assertEquals(2, cache.get("s1", 2).version(),
                "cache must still contain v2 after the stale v1 put");
        assertEquals(full.lex("class AB {}", 2), cache.get("s1", 2).snapshot());
    }
}
