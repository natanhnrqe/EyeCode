package com.eyecode.language.symbol;

import com.eyecode.editor.intelligence.document.TextRange;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SymbolModifierMetadataTest {

    private static final TextRange RANGE = TextRange.of(10, 20);
    private static final SymbolId ID = SymbolId.of(1, RANGE, SymbolKind.FIELD);

    @Test
    void symbolWithoutModifiersHasEmptySet() {
        assertTrue(symbol(Set.of()).modifiers().isEmpty());
    }

    @Test
    void symbolCanCarryStaticModifier() {
        assertEquals(Set.of(SymbolModifier.STATIC), symbol(Set.of(SymbolModifier.STATIC)).modifiers());
    }

    @Test
    void symbolCanCarryMultipleModifiers() {
        Set<SymbolModifier> modifiers = Set.of(
                SymbolModifier.PUBLIC, SymbolModifier.STATIC, SymbolModifier.FINAL);

        assertEquals(modifiers, symbol(modifiers).modifiers());
    }

    @Test
    void modifiersAreDefensivelyCopiedAndImmutable() {
        Set<SymbolModifier> source = new HashSet<>();
        source.add(SymbolModifier.STATIC);
        Symbol symbol = symbol(source);
        source.add(SymbolModifier.FINAL);

        assertEquals(Set.of(SymbolModifier.STATIC), symbol.modifiers());
        assertThrows(UnsupportedOperationException.class,
                () -> symbol.modifiers().add(SymbolModifier.PUBLIC));
    }

    @Test
    void modifierOrderDoesNotAffectEquality() {
        Set<SymbolModifier> first = new java.util.LinkedHashSet<>();
        first.add(SymbolModifier.PUBLIC);
        first.add(SymbolModifier.STATIC);
        Set<SymbolModifier> second = new java.util.LinkedHashSet<>();
        second.add(SymbolModifier.STATIC);
        second.add(SymbolModifier.PUBLIC);

        assertEquals(symbol(first), symbol(second));
    }

    @Test
    void nullModifiersAreRejected() {
        assertThrows(NullPointerException.class, () -> symbol(null));
    }

    @Test
    void nullModifierElementIsRejected() {
        Set<SymbolModifier> modifiers = new HashSet<>();
        modifiers.add(null);

        assertThrows(NullPointerException.class, () -> symbol(modifiers));
    }

    @Test
    void duplicateModifiersAreCollapsed() {
        Set<SymbolModifier> modifiers = new HashSet<>();
        modifiers.add(SymbolModifier.STATIC);
        modifiers.add(SymbolModifier.STATIC);

        assertEquals(1, symbol(modifiers).modifiers().size());
    }

    @Test
    void modifiersDoNotChangeSymbolId() {
        assertEquals(symbol(Set.of()).id(), symbol(Set.of(SymbolModifier.STATIC)).id());
    }

    @Test
    void modifiersParticipateInSymbolEquality() {
        assertNotEquals(symbol(Set.of()), symbol(Set.of(SymbolModifier.STATIC)));
        assertEquals(symbol(Set.of(SymbolModifier.STATIC)), symbol(Set.of(SymbolModifier.STATIC)));
    }

    @Test
    void equalModifierSetsProduceEqualHashCodes() {
        assertEquals(
                symbol(Set.of(SymbolModifier.PUBLIC, SymbolModifier.STATIC)).hashCode(),
                symbol(Set.of(SymbolModifier.STATIC, SymbolModifier.PUBLIC)).hashCode());
    }

    @Test
    void legacyConstructorDefaultsToEmptyModifiers() {
        Symbol symbol = new Symbol(ID, SymbolKind.FIELD, "MAX", RANGE, 1, 1, "Constants.MAX");

        assertEquals(Set.of(), symbol.modifiers());
    }

    @Test
    void semanticSnapshotPreservesModifiers() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        Symbol symbol = symbol(Set.of(SymbolModifier.PUBLIC, SymbolModifier.STATIC));
        table.declareSymbol(table.rootScope(), symbol);

        SemanticModelSnapshot snapshot = table.snapshot(7, "Constants.java");

        assertEquals(symbol.modifiers(), snapshot.symbolTable().find(ID).orElseThrow().modifiers());
    }

    private static Symbol symbol(Set<SymbolModifier> modifiers) {
        return new Symbol(ID, SymbolKind.FIELD, "MAX", RANGE, 1, 1, "Constants.MAX", modifiers);
    }
}
