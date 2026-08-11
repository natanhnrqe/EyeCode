package com.eyecode.language.symbol;

import com.eyecode.editor.intelligence.document.TextRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SymbolTest {

    @Test
    void immutableFields() {
        SymbolId id = new SymbolId(1, 10, 20, SymbolKind.TYPE);
        TextRange range = TextRange.of(10, 20);
        Symbol symbol = new Symbol(id, SymbolKind.TYPE, "Foo", range, 0, "Foo");

        assertEquals(id, symbol.id());
        assertEquals(SymbolKind.TYPE, symbol.kind());
        assertEquals("Foo", symbol.name());
        assertEquals(range, symbol.declarationRange());
        assertEquals(0, symbol.ownerScopeId());
        assertEquals("Foo", symbol.qualifiedName());
    }

    @Test
    void nullIdRejected() {
        TextRange range = TextRange.of(10, 20);
        assertThrows(NullPointerException.class,
                () -> new Symbol(null, SymbolKind.TYPE, "Foo", range, 0, "Foo"));
    }

    @Test
    void nullKindRejected() {
        SymbolId id = new SymbolId(1, 10, 20, SymbolKind.TYPE);
        TextRange range = TextRange.of(10, 20);
        assertThrows(NullPointerException.class,
                () -> new Symbol(id, null, "Foo", range, 0, "Foo"));
    }

    @Test
    void nullNameRejected() {
        SymbolId id = new SymbolId(1, 10, 20, SymbolKind.TYPE);
        TextRange range = TextRange.of(10, 20);
        assertThrows(NullPointerException.class,
                () -> new Symbol(id, SymbolKind.TYPE, null, range, 0, "Foo"));
    }

    @Test
    void nullRangeRejected() {
        SymbolId id = new SymbolId(1, 10, 20, SymbolKind.TYPE);
        assertThrows(NullPointerException.class,
                () -> new Symbol(id, SymbolKind.TYPE, "Foo", null, 0, "Foo"));
    }

    @Test
    void ownerScopeIdAccessible() {
        SymbolId id = new SymbolId(1, 10, 20, SymbolKind.TYPE);
        TextRange range = TextRange.of(10, 20);
        Symbol symbol = new Symbol(id, SymbolKind.TYPE, "Foo", range, 42, "Foo");
        assertEquals(42, symbol.ownerScopeId());
    }

    @Test
    void qualifiedNameAccessible() {
        SymbolId id = new SymbolId(1, 10, 20, SymbolKind.TYPE);
        TextRange range = TextRange.of(10, 20);
        Symbol symbol = new Symbol(id, SymbolKind.TYPE, "Foo", range, 0, "com.example.Foo");
        assertEquals("com.example.Foo", symbol.qualifiedName());
    }
}