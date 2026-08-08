package com.eyecode.editor.intelligence.selection;

import com.eyecode.editor.intelligence.document.TextRange;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bounded-stack semantics of {@link SelectionHistory}: LIFO pops, document
 * version invalidation, size limit and clearing. Not an undo history — it
 * stores ranges only, and stale entries are never restored.
 */
class SelectionHistoryTest {

    private final SelectionHistory history = new SelectionHistory();

    @Test
    void popReturnsLastPushedRange() {
        history.push(1, 0, new TextRange(4, 5));
        history.push(1, 1, new TextRange(4, 9));
        assertEquals(Optional.of(new TextRange(4, 9)), history.pop(1));
        assertEquals(Optional.of(new TextRange(4, 5)), history.pop(1));
        assertFalse(history.pop(1).isPresent());
    }

    @Test
    void pushClearsEntriesFromOlderVersion() {
        history.push(1, 0, new TextRange(4, 5));
        history.push(2, 1, new TextRange(4, 9));
        assertEquals(1, history.size());
        assertEquals(Optional.of(new TextRange(4, 9)), history.pop(2));
        assertFalse(history.pop(2).isPresent());
    }

    @Test
    void popWithStaleVersionInvalidatesEverything() {
        history.push(1, 0, new TextRange(4, 5));
        history.push(1, 1, new TextRange(4, 9));
        assertTrue(history.pop(2).isEmpty());
        assertTrue(history.isEmpty());
    }

    @Test
    void sizeIsBoundedByLimit() {
        for (int i = 0; i < 60; i++) {
            history.push(1, i % 8, new TextRange(i, i + 1));
        }
        assertEquals(50, history.size());
        assertEquals(Optional.of(new TextRange(59, 60)), history.pop(1));
        assertEquals(49, history.size());
    }

    @Test
    void clearDropsEverything() {
        history.push(1, 0, new TextRange(4, 5));
        history.clear();
        assertTrue(history.isEmpty());
        assertFalse(history.pop(1).isPresent());
    }

    @Test
    void topReflectsMostRecentEntryWithoutRemovingIt() {
        history.push(1, 0, new TextRange(4, 5));
        history.push(1, 1, new TextRange(4, 9));
        assertTrue(history.top().isPresent());
        assertEquals(1, history.top().get().level());
        assertEquals(2, history.size());
    }
}
