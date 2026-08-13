package com.eyecode.language.symbol;

import com.eyecode.editor.intelligence.document.TextRange;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SymbolScopeTest {

    @Test
    void rootScopeHasNoParent() {
        SymbolScope root = SymbolScopeImpl.root();
        assertEquals(ScopeKind.ROOT, root.kind());
        assertTrue(root.parent().isEmpty());
    }

    @Test
    void childScopeHasParent() {
        SymbolScope root = SymbolScopeImpl.root();
        SymbolScope child = SymbolScopeImpl.createChild(root, ScopeKind.TYPE, TextRange.of(0, 0));
        assertTrue(child.parent().isPresent());
        assertEquals(root, child.parent().get());
    }

    @Test
    void declareAndFindLocal() {
        SymbolScopeImpl scope = SymbolScopeImpl.root();
        SymbolId id = SymbolId.of(0, 10, 20, SymbolKind.TYPE);
        Symbol symbol = new Symbol(id, SymbolKind.TYPE, "Foo",
                TextRange.of(10, 20), 0, 0, "Foo");
        scope.declare(symbol);

        Optional<Symbol> found = scope.findLocal("Foo");
        assertTrue(found.isPresent());
        assertEquals("Foo", found.get().name());
    }

    @Test
    void duplicateDeclarationThrows() {
        SymbolScopeImpl scope = SymbolScopeImpl.root();
        SymbolId id1 = SymbolId.of(0, 10, 20, SymbolKind.TYPE);
        SymbolId id2 = SymbolId.of(0, 30, 40, SymbolKind.TYPE);
        Symbol symbol1 = new Symbol(id1, SymbolKind.TYPE, "Foo",
                TextRange.of(10, 20), 0, 0, "Foo");
        Symbol symbol2 = new Symbol(id2, SymbolKind.TYPE, "Foo",
                TextRange.of(30, 40), 0, 0, "Foo");

        scope.declare(symbol1);
        assertThrows(IllegalStateException.class, () -> scope.declare(symbol2));
    }

    @Test
    void lookupFindsInParent() {
        SymbolScopeImpl root = SymbolScopeImpl.root();
        SymbolScope child = SymbolScopeImpl.createChild(root, ScopeKind.TYPE, TextRange.of(0, 0));

        SymbolId id = SymbolId.of(0, 10, 20, SymbolKind.TYPE);
        Symbol symbol = new Symbol(id, SymbolKind.TYPE, "Foo",
                TextRange.of(10, 20), 0, 0, "Foo");
        root.declare(symbol);

        Optional<Symbol> found = child.lookup("Foo");
        assertTrue(found.isPresent());
        assertEquals("Foo", found.get().name());
    }

    @Test
    void lookupDoesNotFindInSibling() {
        SymbolScopeImpl root = SymbolScopeImpl.root();
        SymbolScope child1 = SymbolScopeImpl.createChild(root, ScopeKind.TYPE, TextRange.of(0, 0));
        SymbolScope child2 = SymbolScopeImpl.createChild(root, ScopeKind.TYPE, TextRange.of(0, 0));

        SymbolId id = SymbolId.of(0, 10, 20, SymbolKind.TYPE);
        Symbol symbol = new Symbol(id, SymbolKind.TYPE, "Foo",
                TextRange.of(10, 20), 0, 0, "Foo");
        ((SymbolScopeImpl) child1).declare(symbol);

        Optional<Symbol> found = child2.lookup("Foo");
        assertFalse(found.isPresent());
    }

    @Test
    void shadowingInChildScope() {
        SymbolScopeImpl root = SymbolScopeImpl.root();
        SymbolScopeImpl child = SymbolScopeImpl.createChild(root, ScopeKind.METHOD, TextRange.of(0, 0));

        SymbolId id1 = SymbolId.of(0, 10, 20, SymbolKind.LOCAL_VARIABLE);
        SymbolId id2 = SymbolId.of(0, 30, 40, SymbolKind.LOCAL_VARIABLE);
        Symbol symbol1 = new Symbol(id1, SymbolKind.LOCAL_VARIABLE, "x",
                TextRange.of(10, 20), 0, 0, "x");
        Symbol symbol2 = new Symbol(id2, SymbolKind.LOCAL_VARIABLE, "x",
                TextRange.of(30, 40), 0, 0, "x");

        root.declare(symbol1);
        child.declare(symbol2);

        Optional<Symbol> found = child.lookup("x");
        assertTrue(found.isPresent());
        assertEquals(30, found.get().declarationRange().startOffset());
    }

    @Test
    void duplicateDeclarationDetected() {
        SymbolScopeImpl scope = SymbolScopeImpl.root();
        SymbolId id1 = SymbolId.of(0, 10, 20, SymbolKind.FIELD);
        SymbolId id2 = SymbolId.of(0, 30, 40, SymbolKind.FIELD);
        Symbol symbol1 = new Symbol(id1, SymbolKind.FIELD, "x",
                TextRange.of(10, 20), 0, 0, "x");
        Symbol symbol2 = new Symbol(id2, SymbolKind.FIELD, "x",
                TextRange.of(30, 40), 0, 0, "x");

        scope.declare(symbol1);
        assertThrows(IllegalStateException.class, () -> scope.declare(symbol2));
    }

    @Test
    void sameNameDifferentScopesAllowed() {
        SymbolScopeImpl root = SymbolScopeImpl.root();
        SymbolScopeImpl child = SymbolScopeImpl.createChild(root, ScopeKind.TYPE, TextRange.of(0, 0));

        SymbolId id1 = SymbolId.of(0, 10, 20, SymbolKind.FIELD);
        SymbolId id2 = SymbolId.of(0, 30, 40, SymbolKind.FIELD);
        Symbol symbol1 = new Symbol(id1, SymbolKind.FIELD, "x",
                TextRange.of(10, 20), 0, 0, "x");
        Symbol symbol2 = new Symbol(id2, SymbolKind.FIELD, "x",
                TextRange.of(30, 40), 0, 0, "x");

        root.declare(symbol1);
        child.declare(symbol2); // Should not throw - different scopes
    }
}