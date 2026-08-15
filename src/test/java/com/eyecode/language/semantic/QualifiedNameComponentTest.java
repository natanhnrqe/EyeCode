package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.TextRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link QualifiedNameComponent} (Sprint 5.4b.5).
 */
class QualifiedNameComponentTest {

    @Test
    void construction_preservesFields() {
        QualifiedNameComponent c = QualifiedNameComponent.of("foo", TextRange.of(0, 3));
        assertEquals("foo", c.name());
        assertEquals(TextRange.of(0, 3), c.range());
    }

    @Test
    void namePreserved_distinctObjects() {
        QualifiedNameComponent a = QualifiedNameComponent.of("bar", TextRange.of(4, 7));
        QualifiedNameComponent b = QualifiedNameComponent.of("bar", TextRange.of(4, 7));
        assertEquals("bar", a.name());
        assertEquals("bar", b.name());
    }

    @Test
    void rangePreserved() {
        QualifiedNameComponent c = QualifiedNameComponent.of("baz", TextRange.of(100, 103));
        assertEquals(100, c.range().startOffset());
        assertEquals(103, c.range().endOffset());
    }

    @Test
    void nullArguments_rejected() {
        assertThrows(NullPointerException.class,
                () -> QualifiedNameComponent.of(null, TextRange.of(0, 0)));
        assertThrows(NullPointerException.class,
                () -> QualifiedNameComponent.of("foo", null));
    }

    @Test
    void emptyNameAndMismatchedRangeLengths_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> QualifiedNameComponent.of("", TextRange.of(0, 0)));
        // range length 3 vs name "foo" (length 3) is OK, but a mismatch is invalid
        assertThrows(IllegalArgumentException.class,
                () -> QualifiedNameComponent.of("foo", TextRange.of(0, 4)));
        assertThrows(IllegalArgumentException.class,
                () -> QualifiedNameComponent.of("foo", TextRange.of(0, 2)));
    }

    @Test
    void equality_andImmutability() {
        QualifiedNameComponent a = QualifiedNameComponent.of("foo", TextRange.of(0, 3));
        QualifiedNameComponent b = QualifiedNameComponent.of("foo", TextRange.of(0, 3));
        QualifiedNameComponent c = QualifiedNameComponent.of("bar", TextRange.of(0, 3));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
