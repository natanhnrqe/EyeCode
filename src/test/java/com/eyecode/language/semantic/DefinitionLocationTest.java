package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.symbol.Symbol;
import com.eyecode.language.symbol.SymbolId;
import com.eyecode.language.symbol.SymbolKind;
import com.eyecode.language.symbol.SymbolScope;
import com.eyecode.language.symbol.SymbolTable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 5.4c.1 — DefinitionLocation immutability + structural tests.
 * <p>
 * Validates the {@link DefinitionLocation} value type itself: factory
 * validation, equality, hash code, null rejection and that the
 * carried {@link TextRange} is always the symbol's
 * {@code declarationRange()}.
 */
class DefinitionLocationTest {

    /**
     * Minimal in-test {@link SymbolScope} stub so {@link Symbol} can be
     * built (its compact constructor requires a non-null range).
     */
    private static final class StubScope implements SymbolScope {
        private final long id;
        StubScope(long id) { this.id = id; }
        @Override public long id() { return id; }
        @Override public com.eyecode.language.symbol.ScopeKind kind() {
            return com.eyecode.language.symbol.ScopeKind.ROOT;
        }
        @Override public TextRange range() { return TextRange.of(0, 0); }
        @Override public Optional<SymbolScope> parent() { return Optional.empty(); }
        @Override public List<SymbolScope> children() { return List.of(); }
        @Override public List<Symbol> declaredSymbols() { return List.of(); }
        @Override public Optional<Symbol> findLocal(String name) { return Optional.empty(); }
        @Override public Optional<Symbol> lookup(String name) { return Optional.empty(); }
        @Override public boolean declares(String name) { return false; }
    }

    private static Symbol makeSymbol(String name, SymbolKind kind,
                                     TextRange declarationRange) {
        SymbolId id = SymbolId.of(0L, declarationRange, kind);
        return new Symbol(id, kind, name, declarationRange, 0L, 0L, name);
    }

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    @Test
    void factoryOf_extractsDeclarationRange() {
        Symbol s = makeSymbol("foo", SymbolKind.LOCAL_VARIABLE, TextRange.of(10, 13));
        DefinitionLocation loc = DefinitionLocation.of(s);
        assertSame(s, loc.symbol(), "symbol is preserved by identity");
        assertEquals(TextRange.of(10, 13), loc.declarationRange(),
                "declarationRange equals the symbol's declaration range");
    }

    @Test
    void explicitFactory_acceptsMatchingRange() {
        Symbol s = makeSymbol("foo", SymbolKind.FIELD, TextRange.of(20, 23));
        DefinitionLocation loc = DefinitionLocation.of(s, TextRange.of(20, 23));
        assertSame(s, loc.symbol());
        assertEquals(TextRange.of(20, 23), loc.declarationRange());
    }

    // ------------------------------------------------------------------
    // Null rejection
    // ------------------------------------------------------------------

    @Test
    void factoryOf_rejectsNullSymbol() {
        assertThrows(NullPointerException.class, () -> DefinitionLocation.of(null));
    }

    @Test
    void explicitFactory_rejectsNullSymbol() {
        assertThrows(NullPointerException.class,
                () -> DefinitionLocation.of(null, TextRange.of(0, 3)));
    }

    @Test
    void explicitFactory_rejectsNullRange() {
        Symbol s = makeSymbol("foo", SymbolKind.LOCAL_VARIABLE, TextRange.of(0, 3));
        assertThrows(NullPointerException.class,
                () -> DefinitionLocation.of(s, null));
    }

    @Test
    void explicitFactory_rejectsMismatchedRange() {
        Symbol s = makeSymbol("foo", SymbolKind.LOCAL_VARIABLE, TextRange.of(0, 3));
        assertThrows(IllegalArgumentException.class,
                () -> DefinitionLocation.of(s, TextRange.of(0, 4)));
        assertThrows(IllegalArgumentException.class,
                () -> DefinitionLocation.of(s, TextRange.of(1, 3)));
    }

    // ------------------------------------------------------------------
    // Equality / hashCode
    // ------------------------------------------------------------------

    @Test
    void equals_isByValue() {
        Symbol s = makeSymbol("foo", SymbolKind.LOCAL_VARIABLE, TextRange.of(0, 3));
        DefinitionLocation a = DefinitionLocation.of(s);
        DefinitionLocation b = DefinitionLocation.of(s);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_isNotByIdentityOfSymbol() {
        // Two Symbol records with the same content but distinct instances
        // must still produce DefinitionLocations that compare equal.
        SymbolId id = SymbolId.of(0L, TextRange.of(0, 3), SymbolKind.LOCAL_VARIABLE);
        Symbol a = new Symbol(id, SymbolKind.LOCAL_VARIABLE, "foo",
                TextRange.of(0, 3), 0L, 0L, "foo");
        Symbol b = new Symbol(id, SymbolKind.LOCAL_VARIABLE, "foo",
                TextRange.of(0, 3), 0L, 0L, "foo");
        // Different object instances — but record equality holds.
        DefinitionLocation locA = DefinitionLocation.of(a);
        DefinitionLocation locB = DefinitionLocation.of(b);
        assertEquals(locA, locB);
        assertEquals(locA.hashCode(), locB.hashCode());
    }

    @Test
    void equals_differsOnSymbolName() {
        Symbol a = makeSymbol("foo", SymbolKind.LOCAL_VARIABLE, TextRange.of(0, 3));
        Symbol b = makeSymbol("bar", SymbolKind.LOCAL_VARIABLE, TextRange.of(10, 13));
        DefinitionLocation locA = DefinitionLocation.of(a);
        DefinitionLocation locB = DefinitionLocation.of(b);
        assertNotEquals(locA, locB);
    }

    @Test
    void equals_differsOnRange() {
        Symbol s1 = makeSymbol("foo", SymbolKind.LOCAL_VARIABLE, TextRange.of(0, 3));
        Symbol s2 = makeSymbol("foo", SymbolKind.LOCAL_VARIABLE, TextRange.of(10, 13));
        DefinitionLocation loc1 = DefinitionLocation.of(s1);
        DefinitionLocation loc2 = DefinitionLocation.of(s2);
        assertNotEquals(loc1, loc2);
    }

    @Test
    void notEqualToNullOrOtherType() {
        Symbol s = makeSymbol("foo", SymbolKind.LOCAL_VARIABLE, TextRange.of(0, 3));
        DefinitionLocation loc = DefinitionLocation.of(s);
        assertNotEquals(loc, null);
        assertNotEquals(loc, s);
        assertNotEquals(loc, TextRange.of(0, 3));
    }

    // ------------------------------------------------------------------
    // Immutability — observable
    // ------------------------------------------------------------------

    @Test
    void accessors_returnConsistentResults() {
        Symbol s = makeSymbol("foo", SymbolKind.LOCAL_VARIABLE, TextRange.of(0, 3));
        DefinitionLocation loc = DefinitionLocation.of(s);
        // Call the accessors multiple times — should always return the
        // same instances (the field is final and the Symbol itself is a
        // record).
        for (int i = 0; i < 5; i++) {
            assertSame(s, loc.symbol());
            assertEquals(TextRange.of(0, 3), loc.declarationRange());
        }
    }

    @Test
    void toString_containsNameAndRange() {
        Symbol s = makeSymbol("foo", SymbolKind.LOCAL_VARIABLE, TextRange.of(0, 3));
        DefinitionLocation loc = DefinitionLocation.of(s);
        String text = loc.toString();
        assertTrue(text.contains("foo"));
        assertTrue(text.contains("0..3"));
    }
}
