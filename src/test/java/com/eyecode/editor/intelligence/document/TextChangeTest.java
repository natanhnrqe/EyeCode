package com.eyecode.editor.intelligence.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextChangeTest {

    private static DocumentSnapshot snapshot(long version, String text) {
        return new DocumentSnapshot(version, text, LineMap.of(text), null);
    }

    @Test
    void betweenDetectsPureInsert() {
        TextChange change = TextChange.between(snapshot(1, "ab"), snapshot(2, "aXYb"));
        assertTrue(change.isInsert());
        assertEquals(new TextRange(1, 1), change.removedRange());
        assertEquals("XY", change.insertedText());
        assertEquals(new TextRange(1, 3), change.resultingRange());
    }

    @Test
    void betweenDetectsPureDelete() {
        TextChange change = TextChange.between(snapshot(1, "aXYb"), snapshot(2, "ab"));
        assertTrue(change.isDelete());
        assertEquals(new TextRange(1, 3), change.removedRange());
        assertEquals("", change.insertedText());
        assertEquals(new TextRange(1, 1), change.resultingRange());
    }

    @Test
    void betweenDetectsReplace() {
        TextChange change = TextChange.between(snapshot(1, "aXYb"), snapshot(2, "aZb"));
        assertTrue(change.isReplace());
        assertEquals(new TextRange(1, 3), change.removedRange());
        assertEquals("Z", change.insertedText());
        assertEquals(new TextRange(1, 2), change.resultingRange());
    }

    @Test
    void betweenDetectsTailReplaceWhenPrefixAndSuffixEmpty() {
        TextChange change = TextChange.between(snapshot(1, "abc"), snapshot(2, "abX"));
        assertEquals(new TextRange(2, 3), change.removedRange());
        assertEquals("X", change.insertedText());
    }

    @Test
    void betweenDetectsChangeInMiddleWithSharedSuffix() {
        TextChange change = TextChange.between(snapshot(1, "fooBarBaz"), snapshot(2, "fooQuuxBaz"));
        assertEquals(new TextRange(3, 6), change.removedRange());
        assertEquals("Quux", change.insertedText());
    }

    @Test
    void betweenOnIdenticalTextIsEmpty() {
        TextChange change = TextChange.between(snapshot(1, "abc"), snapshot(2, "abc"));
        assertTrue(change.isEmpty());
        assertEquals("", change.insertedText());
        assertEquals(new TextRange(3, 3), change.removedRange());
    }

    @Test
    void betweenCarriesAfterVersion() {
        TextChange change = TextChange.between(snapshot(7, "a"), snapshot(9, "ab"));
        assertEquals(9, change.documentVersion());
    }

    @Test
    void betweenRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> TextChange.between(null, snapshot(1, "")));
        assertThrows(IllegalArgumentException.class, () -> TextChange.between(snapshot(1, ""), null));
    }

    @Test
    void classificationFlags() {
        TextChange insert = TextChange.between(snapshot(0, ""), snapshot(1, "x"));
        assertTrue(insert.isInsert());
        assertFalse(insert.isDelete());
        assertFalse(insert.isReplace());

        TextChange delete = TextChange.between(snapshot(1, "x"), snapshot(2, ""));
        assertTrue(delete.isDelete());
        assertFalse(delete.isInsert());

        TextChange replace = TextChange.between(snapshot(1, "x"), snapshot(2, "y"));
        assertTrue(replace.isReplace());
    }

    @Test
    void deltaIsInsertedMinusRemovedLength() {
        assertEquals(2, TextChange.between(snapshot(0, ""), snapshot(1, "ab")).delta());
        assertEquals(-2, TextChange.between(snapshot(1, "ab"), snapshot(2, "")).delta());
        assertEquals(0, TextChange.between(snapshot(1, "ab"), snapshot(2, "cd")).delta());
    }
}
