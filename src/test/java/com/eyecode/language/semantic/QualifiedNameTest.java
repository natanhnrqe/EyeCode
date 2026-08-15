package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.TextRange;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link QualifiedName} (Sprint 5.4b.5).
 */
class QualifiedNameTest {

    @Test
    void twoComponents() {
        QualifiedName qn = QualifiedName.of(List.of(
                QualifiedNameComponent.of("foo", TextRange.of(0, 3)),
                QualifiedNameComponent.of("bar", TextRange.of(4, 7))));
        assertEquals(2, qn.componentCount());
        assertEquals("foo", qn.component(0).name());
        assertEquals("bar", qn.component(1).name());
    }

    @Test
    void threeComponents() {
        QualifiedName qn = QualifiedName.of(List.of(
                QualifiedNameComponent.of("foo", TextRange.of(0, 3)),
                QualifiedNameComponent.of("bar", TextRange.of(4, 7)),
                QualifiedNameComponent.of("baz", TextRange.of(8, 11))));
        assertEquals(3, qn.componentCount());
        assertEquals("bar", qn.component(1).name());
    }

    @Test
    void defensiveCopy_externalMutationNotVisible() {
        List<QualifiedNameComponent> input = new java.util.ArrayList<>(Arrays.asList(
                QualifiedNameComponent.of("foo", TextRange.of(0, 3)),
                QualifiedNameComponent.of("bar", TextRange.of(4, 7))));
        QualifiedName qn = QualifiedName.of(input);
        // mutate the source list
        input.add(QualifiedNameComponent.of("baz", TextRange.of(8, 11)));
        // the QualifiedName is unaffected
        assertEquals(2, qn.componentCount());
        // mutating the exposed view is also blocked
        assertThrows(UnsupportedOperationException.class,
                () -> qn.components().add(QualifiedNameComponent.of("x", TextRange.of(0, 1))));
    }

    @Test
    void componentOrderPreserved() {
        QualifiedName qn = QualifiedName.of(List.of(
                QualifiedNameComponent.of("a", TextRange.of(0, 1)),
                QualifiedNameComponent.of("bb", TextRange.of(2, 4)),
                QualifiedNameComponent.of("ccc", TextRange.of(5, 8)),
                QualifiedNameComponent.of("dddd", TextRange.of(9, 13))));
        assertEquals("a", qn.component(0).name());
        assertEquals("bb", qn.component(1).name());
        assertEquals("ccc", qn.component(2).name());
        assertEquals("dddd", qn.component(3).name());
    }

    @Test
    void qualifier() {
        QualifiedName qn = QualifiedName.of(List.of(
                QualifiedNameComponent.of("foo", TextRange.of(0, 3)),
                QualifiedNameComponent.of("bar", TextRange.of(4, 7)),
                QualifiedNameComponent.of("baz", TextRange.of(8, 11))));
        QualifiedNameComponent q = qn.qualifier();
        assertEquals("foo", q.name());
        assertEquals(TextRange.of(0, 3), q.range());
    }

    @Test
    void terminalName() {
        QualifiedName qn = QualifiedName.of(List.of(
                QualifiedNameComponent.of("foo", TextRange.of(0, 3)),
                QualifiedNameComponent.of("bar", TextRange.of(4, 7)),
                QualifiedNameComponent.of("baz", TextRange.of(8, 11))));
        QualifiedNameComponent t = qn.terminalName();
        assertEquals("baz", t.name());
        assertEquals(TextRange.of(8, 11), t.range());
    }

    @Test
    void completeRange() {
        // foo.bar.baz : foo [0,3), bar [4,7), baz [8,11)
        QualifiedName qn = QualifiedName.of(List.of(
                QualifiedNameComponent.of("foo", TextRange.of(0, 3)),
                QualifiedNameComponent.of("bar", TextRange.of(4, 7)),
                QualifiedNameComponent.of("baz", TextRange.of(8, 11))));
        assertEquals(TextRange.of(0, 11), qn.range());

        // a.b : a [0,1), b [2,3)
        QualifiedName ab = QualifiedName.of(List.of(
                QualifiedNameComponent.of("a", TextRange.of(0, 1)),
                QualifiedNameComponent.of("b", TextRange.of(2, 3))));
        assertEquals(TextRange.of(0, 3), ab.range());
    }

    @Test
    void rejectsFewerThanTwoComponents() {
        assertThrows(IllegalArgumentException.class,
                () -> QualifiedName.of(List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> QualifiedName.of(List.of(
                        QualifiedNameComponent.of("foo", TextRange.of(0, 3)))));
        // null list
        assertThrows(NullPointerException.class,
                () -> QualifiedName.of(null));
        // null component inside list
        assertThrows(NullPointerException.class,
                () -> QualifiedName.of(Arrays.asList(
                        QualifiedNameComponent.of("foo", TextRange.of(0, 3)),
                        null)));
    }

    @Test
    void equality() {
        QualifiedName a = QualifiedName.of(List.of(
                QualifiedNameComponent.of("foo", TextRange.of(0, 3)),
                QualifiedNameComponent.of("bar", TextRange.of(4, 7))));
        QualifiedName b = QualifiedName.of(List.of(
                QualifiedNameComponent.of("foo", TextRange.of(0, 3)),
                QualifiedNameComponent.of("bar", TextRange.of(4, 7))));
        QualifiedName c = QualifiedName.of(List.of(
                QualifiedNameComponent.of("foo", TextRange.of(0, 3)),
                QualifiedNameComponent.of("baz", TextRange.of(4, 7))));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
