package com.eyecode.language;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.LexerCache;
import com.eyecode.language.java.LexerCacheEntry;
import com.eyecode.language.java.LexerSnapshot;
import com.eyecode.language.java.incremental.FullRelexStrategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Out-of-order version arrivals (stale event handlers, old snapshots flowing
 * back into the service) must never regress the cached state and must always
 * return the snapshot belonging to the requested version.
 */
class LexerCacheVersionRaceTest {

    private final FullRelexStrategy full = new FullRelexStrategy();

    private static DocumentSnapshot snapshot(String sessionId, long version, String text) {
        return new DocumentSnapshot(version, text, null, null, sessionId);
    }

    @Test
    void outOfOrderVersionsNeverRegressTheCache() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = new JavaLexerService(cache);

        service.lex(snapshot("s1", 1, "class A {}"));
        service.lex(snapshot("s1", 2, "class AB {}"));
        service.lex(snapshot("s1", 3, "class ABC {}"));
        service.lex(snapshot("s1", 1, "class A {}"));
        service.lex(snapshot("s1", 2, "class AB {}"));

        LexerCacheEntry entry = cache.get("s1", 3);
        assertEquals(3, entry.version(), "cache must keep the highest version seen");
        assertEquals(full.lex("class ABC {}", 3), entry.snapshot());
        assertEquals(full.lex("class ABC {}", 3), service.lex(snapshot("s1", 3, "class ABC {}")));
    }

    @Test
    void staleLexReturnsTheCorrectStaleVersion() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = new JavaLexerService(cache);

        LexerSnapshot v1 = service.lex(snapshot("s1", 1, "class A {}"));
        service.lex(snapshot("s1", 2, "class AB {}"));
        service.lex(snapshot("s1", 3, "class ABC {}"));

        LexerSnapshot stale = service.lex(snapshot("s1", 1, "class A {}"));

        assertEquals(1, stale.version(), "result must be bound to the requested version");
        assertNotEquals(3, stale.version());
        assertEquals(v1, stale, "stale result must equal a full lex of that version");
        assertEquals(full.lex("class A {}", 1), stale);
    }

    @Test
    void repeatedLatestVersionStaysHit() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = new JavaLexerService(cache);

        LexerSnapshot v3a = service.lex(snapshot("s1", 3, "class ABC {}"));
        LexerSnapshot v3b = service.lex(snapshot("s1", 3, "class ABC {}"));
        LexerSnapshot v3c = service.lex(snapshot("s1", 3, "class ABC {}"));

        assertSame(v3a, v3b);
        assertSame(v3b, v3c);
        assertEquals(full.lex("class ABC {}", 3), v3c);
        assertEquals(2, service.cacheStats().hitCount());
        assertEquals(1, service.cacheStats().missCount());
    }

    @Test
    void alternatedActivationDoesNotCorruptTheNewestVersion() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = new JavaLexerService(cache);

        service.lex(snapshot("s1", 1, "class A {}"));
        service.deactivateSession("s1");
        service.lex(snapshot("s1", 2, "class AB {}"));
        service.deactivateSession("s1");
        service.lex(snapshot("s1", 3, "class ABC {}"));
        service.deactivateSession("s1");

        LexerSnapshot v3 = service.lex(snapshot("s1", 3, "class ABC {}"));

        assertEquals(full.lex("class ABC {}", 3), v3);
        LexerCacheEntry entry = cache.get("s1", 3);
        assertEquals(3, entry.version());
        assertEquals(com.eyecode.language.java.LexerSessionState.INACTIVE, entry.state(),
                "a hit is served from the snapshot — state flips only via explicit activate/deactivate");

        LexerSnapshot v4 = service.lex(snapshot("s1", 4, "class ABCD {}"));
        assertEquals(full.lex("class ABCD {}", 4), v4);
        assertEquals(com.eyecode.language.java.LexerSessionState.ACTIVE, cache.get("s1", 4).state());
        assertTrue(cache.get("s1", 4).hasIncrementalSlot(), "newer version rebuilds the slot");
    }
}
