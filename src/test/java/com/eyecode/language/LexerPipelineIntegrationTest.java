package com.eyecode.language;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.eventbus.EventBus;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.LexerCache;
import com.eyecode.language.java.LexerCacheEntry;
import com.eyecode.language.java.LexerEventBridge;
import com.eyecode.language.java.LexerSessionState;
import com.eyecode.language.java.LexerSnapshot;
import com.eyecode.language.java.event.TokensUpdatedEvent;
import com.eyecode.language.java.incremental.FullRelexStrategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end pipeline (Sprint 5.2e):
 *
 * EditorDocument → EditorBuffer → DocumentTextChangeEvent → LexerEventBridge
 * → JavaLexerService → LexerCache → TokensUpdatedEvent
 *
 * Validates that the published snapshot versions and token contents are
 * always correct, that the cache is populated per session, and that the
 * incremental path produces full-lex-equivalent tokens.
 */
class LexerPipelineIntegrationTest {

    private final FullRelexStrategy full = new FullRelexStrategy();

    private EditorDocument document;
    private EventBus eventBus;
    private LexerCache cache;
    private LexerEventBridge bridge;
    private List<TokensUpdatedEvent> received;

    @BeforeEach
    void setUp() {
        document = new EditorDocument(null, "class App { int x; }");
        eventBus = new EventBus();
        new EditorBuffer(document, eventBus);
        cache = new LexerCache();
        bridge = new LexerEventBridge(new JavaLexerService(cache), eventBus);
        received = new ArrayList<>();
        eventBus.subscribe(TokensUpdatedEvent.class, received::add);
    }

    @Test
    void pipelinePublishesCorrectVersionedContent() {
        document.insert(0, "import java.util.List;\n");
        String t1 = document.getText();
        document.insert(document.length(), "\n// end");
        String t2 = document.getText();
        document.delete(0, 8);
        String t3 = document.getText();
        document.insert(5, "void main() {}");
        String t4 = document.getText();

        assertEquals(4, received.size());
        List<String> texts = List.of(t1, t2, t3, t4);
        long expectedVersion = 2;
        for (int i = 0; i < received.size(); i++) {
            TokensUpdatedEvent event = received.get(i);
            assertEquals(expectedVersion + i, event.getVersion());
            assertEquals(full.lex(texts.get(i), event.getVersion()), event.getSnapshot());
        }
    }

    @Test
    void pipelinePopulatesAndReusesTheSessionCache() {
        String sessionId = document.sessionId();

        document.insert(document.length(), "\nint y;");

        assertEquals(1, received.size());
        LexerCacheEntry entry = cache.get(sessionId, document.currentVersion());
        assertNotNull(entry);
        assertEquals(LexerSessionState.ACTIVE, entry.state());
        assertTrue(entry.hasIncrementalSlot(), "session slot must be built through the pipeline");

        document.insert(document.length(), "\nint z;");
        LexerSnapshot after = received.get(received.size() - 1).getSnapshot();
        assertEquals(full.lex(document.getText(), after.version()), after);

        assertEquals(2, cache.stats().missCount(), "first lex per new version is a miss");
    }

    @Test
    void distinctDocumentsHaveDistinctCacheEntries() {
        EditorDocument other = new EditorDocument(null, "class Other { }");
        new EditorBuffer(other, eventBus);

        document.insert(0, "int a;");
        other.insert(0, "int b;");

        assertEquals(2, received.size());
        assertNotNull(cache.get(document.sessionId(), document.currentVersion()));
        assertNotNull(cache.get(other.sessionId(), other.currentVersion()));
    }

    @Test
    void incrementalPipelineMatchesFullRelexAtEveryStep() {
        String[] mutations = {
                "import x;\n",
                "// head\n",
                "class App { int value = 1; }",
        };
        for (String mutation : mutations) {
            document.insert(document.length(), mutation);
            TokensUpdatedEvent event = received.get(received.size() - 1);
            assertEquals(full.lex(document.getText(), event.getVersion()), event.getSnapshot(),
                    "incremental pipeline must equal full re-lex after: " + mutation);
        }
    }
}
