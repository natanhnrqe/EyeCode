package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.TextRange;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link QualifiedNameDecomposer} (Sprint 5.4b.5).
 * <p>
 * Also includes a compatibility test showing that
 * {@link QualifiedNameClassifier} and {@link QualifiedNameDecomposer}
 * remain complementary responsibilities (spec §9).
 */
class QualifiedNameDecomposerTest {

    @Test
    void fooBar_twoComponents() {
        Optional<QualifiedName> r = QualifiedNameDecomposer.decompose("foo.bar");
        assertTrue(r.isPresent());
        QualifiedName qn = r.orElseThrow();
        assertEquals(2, qn.componentCount());
        assertEquals("foo", qn.component(0).name());
        assertEquals(TextRange.of(0, 3), qn.component(0).range());
        assertEquals("bar", qn.component(1).name());
        assertEquals(TextRange.of(4, 7), qn.component(1).range());
    }

    @Test
    void fooBarBaz_threeComponents() {
        Optional<QualifiedName> r = QualifiedNameDecomposer.decompose("foo.bar.baz");
        assertTrue(r.isPresent());
        QualifiedName qn = r.orElseThrow();
        assertEquals(3, qn.componentCount());
        assertEquals("foo", qn.component(0).name());
        assertEquals(TextRange.of(0, 3), qn.component(0).range());
        assertEquals("bar", qn.component(1).name());
        assertEquals(TextRange.of(4, 7), qn.component(1).range());
        assertEquals("baz", qn.component(2).name());
        assertEquals(TextRange.of(8, 11), qn.component(2).range());
    }

    @Test
    void abcd_fourComponents() {
        Optional<QualifiedName> r = QualifiedNameDecomposer.decompose("a.b.c.d");
        assertTrue(r.isPresent());
        QualifiedName qn = r.orElseThrow();
        assertEquals(4, qn.componentCount());
        assertEquals("a", qn.component(0).name());
        assertEquals(TextRange.of(0, 1), qn.component(0).range());
        assertEquals("b", qn.component(1).name());
        assertEquals(TextRange.of(2, 3), qn.component(1).range());
        assertEquals("c", qn.component(2).name());
        assertEquals(TextRange.of(4, 5), qn.component(2).range());
        assertEquals("d", qn.component(3).name());
        assertEquals(TextRange.of(6, 7), qn.component(3).range());
    }

    @Test
    void baseOffset_shiftsTwoComponentRanges() {
        Optional<QualifiedName> r = QualifiedNameDecomposer.decompose("foo.bar", 100);
        assertTrue(r.isPresent());
        QualifiedName qn = r.orElseThrow();
        assertEquals(TextRange.of(100, 103), qn.component(0).range());
        assertEquals(TextRange.of(104, 107), qn.component(1).range());
    }

    @Test
    void baseOffset_shiftsThreeComponentRanges() {
        Optional<QualifiedName> r = QualifiedNameDecomposer.decompose("foo.bar.baz", 50);
        assertTrue(r.isPresent());
        QualifiedName qn = r.orElseThrow();
        assertEquals(TextRange.of(50, 53), qn.component(0).range());
        assertEquals(TextRange.of(54, 57), qn.component(1).range());
        assertEquals(TextRange.of(58, 61), qn.component(2).range());
    }

    @Test
    void emptyOrNullInput_emptyOptional() {
        assertTrue(QualifiedNameDecomposer.decompose("").isEmpty());
        assertTrue(QualifiedNameDecomposer.decompose(null).isEmpty());
        // only a dot
        assertTrue(QualifiedNameDecomposer.decompose(".").isEmpty());
    }

    @Test
    void leadingDot_isInvalid() {
        assertTrue(QualifiedNameDecomposer.decompose(".bar").isEmpty());
        assertTrue(QualifiedNameDecomposer.decompose(".foo").isEmpty());
    }

    @Test
    void trailingDot_isInvalid() {
        assertTrue(QualifiedNameDecomposer.decompose("foo.").isEmpty());
        assertTrue(QualifiedNameDecomposer.decompose("foo.bar.").isEmpty());
    }

    @Test
    void doubleAndTripleDot_isInvalid() {
        assertTrue(QualifiedNameDecomposer.decompose("foo..bar").isEmpty());
        assertTrue(QualifiedNameDecomposer.decompose("foo...bar").isEmpty());
    }

    @Test
    void whitespace_isInvalid() {
        assertTrue(QualifiedNameDecomposer.decompose("foo. .bar").isEmpty());
        assertTrue(QualifiedNameDecomposer.decompose("foo bar").isEmpty());
        assertTrue(QualifiedNameDecomposer.decompose("foo\t.bar").isEmpty());
        // single identifier (no dot) is not a qualified name either
        assertTrue(QualifiedNameDecomposer.decompose("foo").isEmpty());
    }

    @Test
    void negativeBaseOffset_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> QualifiedNameDecomposer.decompose("foo.bar", -1));
    }

    // Spec §9 — classifier and decomposer are complementary ---------------
    @Test
    void classifierAndDecomposerAreComplementary() {
        // Classifier classifies the textual shape
        QualifiedNameClassifier.Result classification =
                QualifiedNameClassifier.classify("foo.bar.baz");
        assertTrue(classification.isQualifiedName());
        // Decomposer breaks the same text into individual components
        Optional<QualifiedName> decomp =
                QualifiedNameDecomposer.decompose("foo.bar.baz");
        assertTrue(decomp.isPresent());
        assertEquals(3, decomp.orElseThrow().componentCount());
        // The classifier builds its own 2-field QualifiedNameReference (leftmost/rightmost),
        // the decomposer builds the full ordered component list — each keeps its own API.
        assertEquals("foo", classification.qualified().orElseThrow().qualifier());
        assertEquals("baz", classification.qualified().orElseThrow().name());
        assertEquals("foo", decomp.orElseThrow().qualifier().name());
        assertEquals("baz", decomp.orElseThrow().terminalName().name());
    }
}
