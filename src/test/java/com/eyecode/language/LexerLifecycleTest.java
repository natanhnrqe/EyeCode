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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ACTIVE/INACTIVE lifecycle of cached sessions: deactivation releases the
 * heavy incremental structures, activation restores state on demand, and the
 * first lex after (de)activation is always equivalent to the full lexer.
 */
class LexerLifecycleTest {

    private final FullRelexStrategy full = new FullRelexStrategy();

    private static DocumentSnapshot snapshot(String sessionId, long version, String text) {
        return new DocumentSnapshot(version, text, null, null, sessionId);
    }

    private JavaLexerService newService(LexerCache cache) {
        return new JavaLexerService(cache);
    }

    @Test
    void activeToInactiveReleasesIncrementalSlot() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = newService(cache);

        service.lex(snapshot("s1", 1, "class A {}"));
        LexerCacheEntry active = cache.get("s1", 1);
        assertTrue(active.hasIncrementalSlot());
        assertEquals(LexerSessionState.ACTIVE, active.state());

        service.deactivateSession("s1");

        LexerCacheEntry inactive = cache.get("s1", 1);
        assertEquals(LexerSessionState.INACTIVE, inactive.state());
        assertFalse(inactive.hasIncrementalSlot());
        assertNull(inactive.previousText());
        assertNotNull(inactive.snapshot(), "minimal metadata (snapshot + version) must be kept");
    }

    @Test
    void inactiveToActiveFlipsState() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = newService(cache);

        service.lex(snapshot("s1", 1, "class A {}"));
        service.deactivateSession("s1");
        service.activateSession("s1");

        assertEquals(LexerSessionState.ACTIVE, cache.get("s1", 1).state());
    }

    @Test
    void inactiveSameVersionStillServedFromSnapshot() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = newService(cache);

        LexerSnapshot v1 = service.lex(snapshot("s1", 1, "class A {}"));
        service.deactivateSession("s1");

        LexerSnapshot again = service.lex(snapshot("s1", 1, "class A {}"));

        assertEquals(full.lex("class A {}", 1), again);
        assertTrue(service.cacheStats().hitCount() >= 1);
        assertEquals(v1, again);
    }

    @Test
    void firstLexAfterInactivationEqualsFullLexer() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = newService(cache);

        service.lex(snapshot("s1", 1, "class A {}"));
        service.deactivateSession("s1");

        LexerSnapshot v2 = service.lex(snapshot("s1", 2, "class AB {}"));

        assertEquals(full.lex("class AB {}", 2), v2, "slot dropped — must be equivalent to full re-lex");
        LexerCacheEntry entry = cache.get("s1", 2);
        assertEquals(LexerSessionState.ACTIVE, entry.state(), "lex rebuilds the slot and re-activates");
        assertTrue(entry.hasIncrementalSlot());
    }

    @Test
    void reactivationRestoresStateOnDemand() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = newService(cache);

        service.lex(snapshot("s1", 1, "class A {}"));
        service.deactivateSession("s1");
        service.activateSession("s1");

        LexerSnapshot v2 = service.lex(snapshot("s1", 2, "class AB {}"));
        LexerSnapshot v3 = service.lex(snapshot("s1", 3, "class ABC {}"));

        assertEquals(full.lex("class AB {}", 2), v2);
        assertEquals(full.lex("class ABC {}", 3), v3);
        assertTrue(cache.get("s1", 3).hasIncrementalSlot());
    }

    @Test
    void deactivateOfUnknownSessionIsNoOp() {
        LexerCache cache = new LexerCache();
        JavaLexerService service = newService(cache);

        service.deactivateSession("ghost");
        service.activateSession("ghost");
        service.invalidateSession("ghost");

        assertEquals(0, cache.size());
        assertEquals(0, service.cacheStats().invalidationCount());
    }
}
