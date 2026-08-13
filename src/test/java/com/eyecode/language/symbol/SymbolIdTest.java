package com.eyecode.language.symbol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SymbolIdTest {

    @Test
    void equality_sameValues() {
        SymbolId id1 = SymbolId.of(1, 10, 20, SymbolKind.TYPE);
        SymbolId id2 = SymbolId.of(1, 10, 20, SymbolKind.TYPE);
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void inequality_differentOwner() {
        SymbolId id1 = SymbolId.of(1, 10, 20, SymbolKind.TYPE);
        SymbolId id2 = SymbolId.of(2, 10, 20, SymbolKind.TYPE);
        assertNotEquals(id1, id2);
    }

    @Test
    void inequality_differentRange() {
        SymbolId id1 = SymbolId.of(1, 10, 20, SymbolKind.TYPE);
        SymbolId id2 = SymbolId.of(1, 15, 25, SymbolKind.TYPE);
        assertNotEquals(id1, id2);
    }

    @Test
    void inequality_differentKind() {
        SymbolId id1 = SymbolId.of(1, 10, 20, SymbolKind.TYPE);
        SymbolId id2 = SymbolId.of(1, 10, 20, SymbolKind.FIELD);
        assertNotEquals(id1, id2);
    }

    @Test
    void determinism_sameConstructionYieldsSameId() {
        SymbolId id1 = SymbolId.of(42, 100, 200, SymbolKind.METHOD);
        SymbolId id2 = SymbolId.of(42, 100, 200, SymbolKind.METHOD);
        assertEquals(id1, id2);
    }

    @Test
    void toString_containsKindAndRange() {
        SymbolId id = SymbolId.of(5, 100, 200, SymbolKind.FIELD);
        String str = id.toString();
        assertTrue(str.contains("FIELD"));
        assertTrue(str.contains("5"));
        assertTrue(str.contains("100"));
        assertTrue(str.contains("200"));
    }
}