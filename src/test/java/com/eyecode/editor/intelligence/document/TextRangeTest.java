package com.eyecode.editor.intelligence.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextRangeTest {

    @Test
    void validationRejectsNegativeStart() {
        assertThrows(IllegalArgumentException.class, () -> new TextRange(-1, 0));
    }

    @Test
    void validationRejectsEndBeforeStart() {
        assertThrows(IllegalArgumentException.class, () -> new TextRange(5, 3));
    }

    @Test
    void lengthAndEmpty() {
        TextRange range = new TextRange(2, 6);
        assertEquals(4, range.length());
        assertFalse(range.isEmpty());
        assertTrue(new TextRange(3, 3).isEmpty());
    }

    @Test
    void containsOffsetInclusive() {
        TextRange range = new TextRange(2, 6);
        assertTrue(range.contains(2));
        assertTrue(range.contains(6));
        assertTrue(range.contains(4));
        assertFalse(range.contains(1));
        assertFalse(range.contains(7));
    }

    @Test
    void containsRange() {
        TextRange range = new TextRange(2, 6);
        assertTrue(range.contains(new TextRange(3, 5)));
        assertTrue(range.contains(new TextRange(2, 6)));
        assertFalse(range.contains(new TextRange(1, 3)));
        assertFalse(range.contains(new TextRange(4, 7)));
    }

    @Test
    void intersectsOverlapOnly() {
        TextRange range = new TextRange(2, 6);
        assertTrue(range.intersects(new TextRange(4, 8)));
        assertTrue(range.intersects(new TextRange(0, 3)));
        assertFalse(range.intersects(new TextRange(6, 9)));
        assertFalse(range.intersects(new TextRange(0, 2)));
    }

    @Test
    void shiftMovesBothOffsets() {
        TextRange range = new TextRange(2, 6).shift(3);
        assertEquals(5, range.startOffset());
        assertEquals(9, range.endOffset());
    }

    @Test
    void intersectionComputesOverlap() {
        TextRange overlap = new TextRange(2, 6).intersection(new TextRange(4, 8));
        assertEquals(4, overlap.startOffset());
        assertEquals(6, overlap.endOffset());
        assertNull(new TextRange(2, 3).intersection(new TextRange(5, 7)));
    }
}
