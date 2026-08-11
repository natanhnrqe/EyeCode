package com.eyecode.language.symbol;

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
        SymbolScope child = SymbolScopeImpl.createChild(root, ScopeKind.TYPE);
        assertTrue(child.parent().isPresent());
        assertEquals(root, child.parent().get());
    }

    @Test
    void declareAndFindLocal() {
        SymbolScope scope = SymbolScopeImpl.root();
        SymbolId id = new SymbolId(0, 10, 20, SymbolKind.TYPE);
        Symbol symbol = new Symbol(
                new SymbolId(0, 10, 20, SymbolKind.TYPE),
                SymbolKind.TYPE, "Foo",
                com.eyecode.editor.intelligence.document.TextRange.of(10, 20),
                0, "Foo"
        );
        scope.declare(symbol);

        Optional<Symbol> found = scope.findLocal("Foo");
        assertTrue(found.isPresent());
        assertEquals("Foo", found.get().name());
    }

    @Test
    void duplicateDeclarationThrows() {
        SymbolScope scope = SymbolScopeImpl.root();
        SymbolId id1 = new SymbolId(0, 10, 20, SymbolKind.TYPE);
        SymbolId id2 = new SymbolId(0, 30, 40, SymbolKind.TYPE);
        Symbol symbol1 = new Symbol(id1, SymbolKind.TYPE, "Foo",
                com.eyecode.editor.intelligence.document.TextRange.of(10, 20), 0, "Foo");
        Symbol symbol2 = new Symbol(id2, SymbolKind.TYPE, "Foo",
                com.eyecode.editor.intelligence.document.TextRange.of(30, 40), 0, "Foo");

        scope.declare(symbol1);
        assertThrows(IllegalStateException.class, () -> scope.declare(symbol2));
    }

    @Test
    void lookupFindsInParent() {
        SymbolScope root = SymbolScopeImpl.root();
        SymbolScope child = SymbolScopeImpl.createChild(root, ScopeKind.TYPE);

        SymbolId id = new SymbolId(0, 10, 20, SymbolKind.TYPE);
        Symbol symbol = new Symbol(
                new SymbolId(0, 10, 20, SymbolKind.TYPE),
                SymbolKind.TYPE, "Foo",
                com.eyecode.editor.intelligence.document.TextRange.of(10, 20),
                0, "Foo"
        );
        root.declare(symbol);

        Optional<Symbol> found = child.lookup("Foo");
        assertTrue(found.isPresent());
        assertEquals("Foo", found.get().name());
    }

    @Test
    void lookupDoesNotFindInSibling() {
        SymbolScope root = SymbolScopeImpl.root();
        SymbolScope child1 = SymbolScopeImpl.createChild(root, ScopeKind.TYPE);
        SymbolScope child2 = SymbolScopeImpl.createChild(root, ScopeKind.TYPE);

        SymbolId id = new SymbolId(0, 10, 20, SymbolKind.TYPE);
        Symbol symbol = new Symbol(
                new SymbolId(0, 10, 20, SymbolKind.TYPE),
                SymbolKind.TYPE, "Foo",
                com.eyecode.editor.intelligence.document.TextRange.of(10, 20),
                0, "Foo"
        );
        child1.declare(symbol);

        Optional<Symbol> found = child2.lookup("Foo");
        assertFalse(found.isPresent());
    }

    @Test
    void shadowingInChildScope() {
        SymbolScope root = SymbolScopeImpl.root();
        SymbolScope child = SymbolScopeImpl.createChild(root, ScopeKind.METHOD);

        SymbolId id1 = new SymbolId(0, 10, 20, SymbolKind.LOCAL_VARIABLE);
        SymbolId id2 = new SymbolId(0, 30, 40, SymbolKind.LOCAL_VARIABLE);
        Symbol symbol1 = new Symbol(
                new SymbolId(0, 10, 20, SymbolKind.LOCAL_VARIABLE),
                SymbolKind.LOCAL_VARIABLE, "x",
                com.eyecode.editor.intelligence.document.TextRange.of(10, 20), 0, "x"
        );
        Symbol symbol2 = new Symbol(
                new SymbolId(0, 30, 40, SymbolKind.LOCAL_VARIABLE),
                SymbolKind.LOCAL_VARIABLE, "x",
                com.eyecode.editor.intelligence.document.TextRange.of(30, 40), 0, "x"
        );

        root.declare(new Symbol(
                new SymbolId(0, 10, 20, SymbolKind.LOCAL_VARIABLE),
                SymbolKind.LOCAL_VARIABLE, "x",
                com.eyecode.editor.intelligence.document.TextRange.of(10, 20), 0, "x"
        ));
        child.declare(new Symbol(
                new SymbolId(0, 30, 40, SymbolKind.LOCAL_VARIABLE),
                SymbolKind.LOCAL_VARIABLE, "x",
                com.eyecode.editor.intelligence.document.TextRange.of(30, 40), 0, "x"
        ));

        Optional<Symbol> found = child.lookup("x");
        assertTrue(found.isPresent());
        assertEquals(30, found.get().declarationRange().startOffset());
    }

    @Test
    void duplicateDeclarationDetected() {
        SymbolScope scope = SymbolScopeImpl.root();
        SymbolId id1 = new SymbolId(0, 10, 20, SymbolKind.FIELD);
        SymbolId id2 = new SymbolId(0, 30, 40, SymbolKind.FIELD);
        Symbol symbol1 = new Symbol(
                new SymbolId(0, 10, 20, SymbolKind.FIELD),
                SymbolKind.FIELD, "x",
                com.eyecode.editor.intelligence.document.TextRange.of(10, 20), 0, "x"
        );
        Symbol symbol2 = new Symbol(
                new SymbolId(0, 30, 40, SymbolKind.FIELD),
                SymbolKind.FIELD, "x",
                com.eyecode.editor.intelligence.document.TextRange.of(30, 40), 0, "x"
        );

        scope.declare(symbol1);
        assertThrows(IllegalStateException.class, () -> scope.declare(symbol2));
    }

    @Test
    void sameNameDifferentScopesAllowed() {
        SymbolScope root = SymbolScopeImpl.root();
        SymbolScope child = SymbolScopeImpl.createChild(root, ScopeKind.TYPE);

        SymbolId id1 = new SymbolId(0, 10, 20, SymbolKind.FIELD);
        SymbolId id2 = new SymbolId(0, 30, 40, SymbolKind.FIELD);
        Symbol symbol1 = new Symbol(
                new SymbolId(0, 10, 20, SymbolKind.FIELD),
                SymbolKind.FIELD, "x",
                com.eyecode.editor.intelligence.document.TextRange.of(10, 20), 0, "x"
        );
        Symbol symbol2 = new Symbol(
                new SymbolId(0, 30, 40, SymbolKind.FIELD),
                SymbolKind.FIELD, "x",
                com.eyecode.editor.intelligence.document.TextRange.of(30, 40), 0, "x"
        );

        root.declare(symbol1);
        child.declare(symbol2); // Should not throw - different scopes
    }
}