package com.eyecode.editor.intelligence.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LineMapTest {

    @Test
    void emptyTextHasSingleLine() {
        LineMap map = LineMap.of("");
        assertEquals(1, map.lineCount());
        assertEquals(0, map.lineStartOffset(0));
        assertEquals(0, map.lineEndOffset(0));
    }

    @Test
    void singleLineWithoutTerminator() {
        LineMap map = LineMap.of("hello");
        assertEquals(1, map.lineCount());
        assertEquals(0, map.lineStartOffset(0));
        assertEquals(5, map.lineEndOffset(0));
        assertEquals(0, map.lineOfOffset(0));
        assertEquals(0, map.lineOfOffset(5));
    }

    @Test
    void lfSeparatedLines() {
        LineMap map = LineMap.of("a\nb");
        assertEquals(2, map.lineCount());
        assertEquals(0, map.lineStartOffset(0));
        assertEquals(1, map.lineEndOffset(0));
        assertEquals(2, map.lineStartOffset(1));
        assertEquals(3, map.lineEndOffset(1));
    }

    @Test
    void trailingNewlineProducesEmptyLastLine() {
        LineMap map = LineMap.of("a\n");
        assertEquals(2, map.lineCount());
        assertEquals(0, map.lineStartOffset(0));
        assertEquals(2, map.lineStartOffset(1));
        assertEquals(2, map.lineEndOffset(1));
        assertEquals(1, map.lineOfOffset(2));
        assertEquals(0, map.columnOfOffset(2));
    }

    @Test
    void bareNewlineIsTwoEmptyLines() {
        LineMap map = LineMap.of("\n");
        assertEquals(2, map.lineCount());
        assertEquals(0, map.lineStartOffset(0));
        assertEquals(1, map.lineStartOffset(1));
    }

    @Test
    void crlfCountsAsSingleTerminator() {
        LineMap map = LineMap.of("a\r\nb");
        assertEquals(2, map.lineCount());
        assertEquals(0, map.lineStartOffset(0));
        assertEquals(1, map.lineEndOffset(0));
        assertEquals(3, map.lineStartOffset(1));
        assertEquals(4, map.lineEndOffset(1));
    }

    @Test
    void crCountsAsTerminator() {
        LineMap map = LineMap.of("a\rb");
        assertEquals(2, map.lineCount());
        assertEquals(2, map.lineStartOffset(1));
    }

    @Test
    void unicodeLineSeparatorsCountAsTerminators() {
        assertEquals(2, LineMap.of("a\u2028b").lineCount());
        assertEquals(2, LineMap.of("a\u2029b").lineCount());
        assertEquals(2, LineMap.of("a\u0085b").lineCount());
    }

    @Test
    void lineOfOffsetClampsNegativeAndBeyondEnd() {
        LineMap map = LineMap.of("a\nb");
        assertEquals(0, map.lineOfOffset(-5));
        assertEquals(1, map.lineOfOffset(99));
    }

    @Test
    void lineOfOffsetUsesBinarySearchOnManyLines() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            builder.append("line").append(i).append('\n');
        }
        LineMap map = LineMap.of(builder);
        assertEquals(1001, map.lineCount());
        for (int i = 0; i <= 1000; i++) {
            assertEquals(i, map.lineOfOffset(map.lineStartOffset(i)));
        }
        assertEquals(0, map.lineOfOffset(map.lineStartOffset(0)));
        assertEquals(1000, map.lineOfOffset(map.lineStartOffset(1000)));
    }

    @Test
    void offsetOfMapsLineAndColumn() {
        LineMap map = LineMap.of("a\nbc");
        assertEquals(0, map.offsetOf(0, 0));
        assertEquals(1, map.offsetOf(0, 1));
        assertEquals(2, map.offsetOf(1, 0));
        assertEquals(4, map.offsetOf(1, 2));
    }

    @Test
    void offsetOfClampsOutOfRangeLineAndColumn() {
        LineMap map = LineMap.of("a\nbc");
        assertEquals(2, map.offsetOf(99, 0));
        assertEquals(4, map.offsetOf(1, 99));
        assertEquals(2, map.offsetOf(1, -3));
    }

    @Test
    void columnOfOffsetIsOffsetWithinLine() {
        LineMap map = LineMap.of("a\nbc");
        assertEquals(0, map.columnOfOffset(0));
        assertEquals(1, map.columnOfOffset(1));
        assertEquals(0, map.columnOfOffset(2));
        assertEquals(2, map.columnOfOffset(4));
    }

    @Test
    void emptyFactoryMatchesEmptyText() {
        assertEquals(1, LineMap.empty().lineCount());
    }
}
