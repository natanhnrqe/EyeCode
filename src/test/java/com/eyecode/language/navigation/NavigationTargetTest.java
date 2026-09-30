package com.eyecode.language.navigation;

import com.eyecode.editor.intelligence.document.TextRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavigationTargetTest {

    @Test
    void recordKeepsUriRangeAndSelectionRange() {
        TextRange range = TextRange.of(4, 10);
        TextRange selection = TextRange.of(4, 8);

        NavigationTarget target = new NavigationTarget("file:///Main.java", range, selection);

        assertEquals("file:///Main.java", target.uri());
        assertEquals(range, target.range());
        assertEquals(selection, target.selectionRange());
    }

    @Test
    void factoryUsesTheRangeAsSelectionRange() {
        TextRange range = TextRange.of(12, 20);

        NavigationTarget target = NavigationTarget.of("file:///Main.java", range);

        assertEquals(range, target.range());
        assertEquals(range, target.selectionRange());
    }

    @Test
    void nullUriIsRejected() {
        assertThrows(NullPointerException.class,
                () -> new NavigationTarget(null, TextRange.of(0, 1), TextRange.of(0, 1)));
    }

    @Test
    void nullRangeIsRejected() {
        assertThrows(NullPointerException.class,
                () -> new NavigationTarget("file:///Main.java", null, TextRange.of(0, 1)));
    }

    @Test
    void nullSelectionRangeIsRejected() {
        assertThrows(NullPointerException.class,
                () -> new NavigationTarget("file:///Main.java", TextRange.of(0, 1), null));
    }

    @Test
    void equalityIsByValue() {
        NavigationTarget first = new NavigationTarget("file:///Main.java", TextRange.of(2, 6), TextRange.of(2, 4));
        NavigationTarget second = new NavigationTarget("file:///Main.java", TextRange.of(2, 6), TextRange.of(2, 4));

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new NavigationTarget("file:///Outro.java", TextRange.of(2, 6), TextRange.of(2, 4)));
        assertNotEquals(first, new NavigationTarget("file:///Main.java", TextRange.of(3, 6), TextRange.of(2, 4)));
        assertNotNull(first.toString());
        assertTrue(first.toString().contains("file:///Main.java"));
    }
}
