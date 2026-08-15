package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.TextRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the immutable {@link QualifiedNameReference} model
 * (Sprint 5.4b.4 — well-formed construction, preservation of values,
 * null rejection, equality).
 */
class QualifiedNameReferenceTest {

    // 1. simple construction --------------------------------------------------
    @Test
    void simpleConstruction() {
        QualifiedNameReference ref = QualifiedNameReference.of(
                "foo", "bar",
                TextRange.of(0, 3), TextRange.of(4, 7));
        assertDoesNotThrow(() -> {
            assertEquals("foo", ref.qualifier());
            assertEquals("bar", ref.name());
        });
    }

    // 2. qualifier preserved --------------------------------------------------
    @Test
    void qualifierPreserved() {
        String q = "com.eyecode.Why";
        QualifiedNameReference ref = QualifiedNameReference.of(
                q, "bar", TextRange.of(0, q.length()), TextRange.of(100, 103));
        assertEquals(q, ref.qualifier());
    }

    // 3. terminal name preserved ----------------------------------------------
    @Test
    void terminalNamePreserved() {
        String n = "finalDestination";
        QualifiedNameReference ref = QualifiedNameReference.of(
                "foo", n, TextRange.of(0, 3), TextRange.of(4, 4 + n.length()));
        assertEquals(n, ref.name());
    }

    // 4. ranges preserved -----------------------------------------------------
    @Test
    void rangesPreserved() {
        QualifiedNameReference ref = QualifiedNameReference.of(
                "foo", "bar",
                TextRange.of(0, 3), TextRange.of(4, 7));
        assertEquals(TextRange.of(0, 3), ref.qualifierRange());
        assertEquals(TextRange.of(4, 7), ref.nameRange());
    }

    @Test
    void rangesPreservedThreeComponent() {
        // foo.bar.baz — qualifier = first component "foo", name = last "baz"
        // the "bar" sits in the gap between ranges.
        QualifiedNameReference ref = QualifiedNameReference.of(
                "foo", "baz",
                TextRange.of(0, 3), TextRange.of(8, 11));
        assertEquals(TextRange.of(0, 3), ref.qualifierRange());
        assertEquals(TextRange.of(8, 11), ref.nameRange());
    }

    // 5. null rejection -------------------------------------------------------
    @Test
    void nullArgumentsRejected() {
        TextRange q = TextRange.of(0, 3);
        TextRange n = TextRange.of(4, 7);
        assertThrows(NullPointerException.class,
                () -> QualifiedNameReference.of(null, "bar", q, n));
        assertThrows(NullPointerException.class,
                () -> QualifiedNameReference.of("foo", null, q, n));
        assertThrows(NullPointerException.class,
                () -> QualifiedNameReference.of("foo", "bar", null, n));
        assertThrows(NullPointerException.class,
                () -> QualifiedNameReference.of("foo", "bar", q, null));
    }

    @Test
    void emptyNamesAndRangeMismatchRejected() {
        TextRange q = TextRange.of(0, 3);
        TextRange n = TextRange.of(4, 7);
        // empty qualifier
        assertThrows(IllegalArgumentException.class,
                () -> QualifiedNameReference.of("", "bar", TextRange.of(0, 0), n));
        // empty name
        assertThrows(IllegalArgumentException.class,
                () -> QualifiedNameReference.of("foo", "", q, TextRange.of(7, 7)));
        // qualifierRange length mismatch
        assertThrows(IllegalArgumentException.class,
                () -> QualifiedNameReference.of("foo", "bar", TextRange.of(0, 2), n));
        // nameRange length mismatch
        assertThrows(IllegalArgumentException.class,
                () -> QualifiedNameReference.of("foo", "bar", q, TextRange.of(4, 8)));
        // qualifier range ends after name range starts (overlap)
        assertThrows(IllegalArgumentException.class,
                () -> QualifiedNameReference.of("foo", "bar", TextRange.of(0, 10), TextRange.of(4, 7)));
    }

    // 6. immutable equality ---------------------------------------------------
    @Test
    void immutableEquality() {
        QualifiedNameReference a = QualifiedNameReference.of(
                "foo", "bar", TextRange.of(0, 3), TextRange.of(4, 7));
        QualifiedNameReference b = QualifiedNameReference.of(
                "foo", "bar", TextRange.of(0, 3), TextRange.of(4, 7));
        QualifiedNameReference c = QualifiedNameReference.of(
                "foo", "baz", TextRange.of(0, 3), TextRange.of(4, 7));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertTrue(a.equals(a) && !a.equals("not a reference"));
    }
}
