package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.symbol.SymbolReference;
import com.eyecode.language.symbol.SymbolReferenceKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Sprint 5.4d.1 — ReferenceLocation immutability + structural tests.
 */
class ReferenceLocationTest {

    @Test
    void of_preservesReference_andSetsRangeFromIt() {
        SymbolReference ref = new SymbolReference(
                null, TextRange.of(10, 14), "foo", 1L, SymbolReferenceKind.SIMPLE_NAME);
        ReferenceLocation loc = ReferenceLocation.of(ref);
        assertSame(ref, loc.reference());
        assertEquals(TextRange.of(10, 14), loc.range());
    }

    @Test
    void constructor_rejectsNullReference() {
        assertThrows(NullPointerException.class,
                () -> new ReferenceLocation(null, TextRange.of(0, 0)));
    }

    @Test
    void constructor_rejectsNullRange() {
        SymbolReference ref = new SymbolReference(
                null, TextRange.of(0, 4), "foo", 1L, SymbolReferenceKind.SIMPLE_NAME);
        assertThrows(NullPointerException.class, () -> new ReferenceLocation(ref, null));
    }

    @Test
    void of_rejectsNullReference() {
        assertThrows(NullPointerException.class, () -> ReferenceLocation.of(null));
    }

    @Test
    void equals_byValue_overBothFields() {
        SymbolReference ref = new SymbolReference(
                null, TextRange.of(10, 14), "foo", 1L, SymbolReferenceKind.SIMPLE_NAME);
        ReferenceLocation a = new ReferenceLocation(ref, TextRange.of(10, 14));
        ReferenceLocation b = ReferenceLocation.of(ref);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differs_whenRangeDiffers() {
        SymbolReference ref = new SymbolReference(
                null, TextRange.of(10, 14), "foo", 1L, SymbolReferenceKind.SIMPLE_NAME);
        ReferenceLocation a = new ReferenceLocation(ref, TextRange.of(10, 14));
        ReferenceLocation b = new ReferenceLocation(ref, TextRange.of(10, 15));
        assertNotEquals(a, b);
    }

    @Test
    void equals_differs_whenReferenceDiffers() {
        SymbolReference refA = new SymbolReference(
                null, TextRange.of(10, 14), "foo", 1L, SymbolReferenceKind.SIMPLE_NAME);
        SymbolReference refB = new SymbolReference(
                null, TextRange.of(10, 14), "bar", 1L, SymbolReferenceKind.SIMPLE_NAME);
        ReferenceLocation a = new ReferenceLocation(refA, TextRange.of(10, 14));
        ReferenceLocation b = new ReferenceLocation(refB, TextRange.of(10, 14));
        assertNotEquals(a, b);
    }

    @Test
    void notEqual_toNull_orOtherType() {
        SymbolReference ref = new SymbolReference(
                null, TextRange.of(10, 14), "foo", 1L, SymbolReferenceKind.SIMPLE_NAME);
        ReferenceLocation loc = ReferenceLocation.of(ref);
        assertNotEquals(loc, null);
        assertNotEquals(loc, "not a location");
    }

    @Test
    void toString_containsNameAndRange() {
        SymbolReference ref = new SymbolReference(
                null, TextRange.of(10, 14), "foo", 1L, SymbolReferenceKind.SIMPLE_NAME);
        ReferenceLocation loc = ReferenceLocation.of(ref);
        String s = loc.toString();
        assertNotNull(s);
    }
}
