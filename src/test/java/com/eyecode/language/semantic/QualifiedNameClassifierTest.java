package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.TextRange;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link QualifiedNameClassifier} — purely-syntactic
 * classification of textual references into SIMPLE_NAME, QUALIFIED_NAME,
 * or INVALID (Sprint 5.4b.4).
 */
class QualifiedNameClassifierTest {

    // 1. foo -> SIMPLE_NAME ----------------------------------------------------
    @Test
    void simpleName_noDots() {
        QualifiedNameClassifier.Result r = QualifiedNameClassifier.classify("foo");
        assertTrue(r.isSimpleName());
        assertEquals(QualifiedNameClassifier.Kind.SIMPLE_NAME, r.kind());
        assertTrue(r.qualified().isEmpty());
    }

    // 2. foo.bar -> QUALIFIED_NAME --------------------------------------------
    @Test
    void qualifiedName_twoComponents() {
        QualifiedNameClassifier.Result r = QualifiedNameClassifier.classify("foo.bar");
        assertTrue(r.isQualifiedName());
        assertEquals(QualifiedNameClassifier.Kind.QUALIFIED_NAME, r.kind());
        QualifiedNameReference ref = r.qualified().orElseThrow();
        assertEquals("foo", ref.qualifier());
        assertEquals("bar", ref.name());
        // ranges match the spec example: foo -> [0,3), bar -> [4,7)
        assertEquals(TextRange.of(0, 3), ref.qualifierRange());
        assertEquals(TextRange.of(4, 7), ref.nameRange());
    }

    // 3. foo.bar.baz -> QUALIFIED_NAME (no decomposition at level 3) -----------
    @Test
    void qualifiedName_threeComponentsClassifiedQualifierAndLastName() {
        QualifiedNameClassifier.Result r = QualifiedNameClassifier.classify("foo.bar.baz");
        assertTrue(r.isQualifiedName());
        QualifiedNameReference ref = r.qualified().orElseThrow();
        // qualifier = leftmost component, name = rightmost component;
        // the middle "bar" is not decomposed (5.4b.5+).
        assertEquals("foo", ref.qualifier());
        assertEquals("baz", ref.name());
        assertEquals(TextRange.of(0, 3), ref.qualifierRange());
        assertEquals(TextRange.of(8, 11), ref.nameRange());
    }

    // 4. this.value -> QUALIFIED_NAME ---------------------------------------------
    @Test
    void thisValue_isQualified() {
        QualifiedNameClassifier.Result r = QualifiedNameClassifier.classify("this.value");
        assertTrue(r.isQualifiedName());
        QualifiedNameReference ref = r.qualified().orElseThrow();
        assertEquals("this", ref.qualifier());
        assertEquals("value", ref.name());
    }

    // 5. super.value -> QUALIFIED_NAME --------------------------------------------
    @Test
    void superValue_isQualified() {
        QualifiedNameClassifier.Result r = QualifiedNameClassifier.classify("super.value");
        assertTrue(r.isQualifiedName());
        QualifiedNameReference ref = r.qualified().orElseThrow();
        assertEquals("super", ref.qualifier());
        assertEquals("value", ref.name());
    }

    // 6. empty input -> INVALID -------------------------------------------------
    @Test
    void emptyInput_isInvalid() {
        QualifiedNameClassifier.Result r = QualifiedNameClassifier.classify("");
        assertTrue(r.isInvalid());
        assertEquals(QualifiedNameClassifier.Kind.INVALID, r.kind());
        assertTrue(r.qualified().isEmpty());
    }

    @Test
    void nullInput_isInvalid() {
        // Convenience — null text yields INVALID rather than throwing.
        QualifiedNameClassifier.Result r = QualifiedNameClassifier.classify(null);
        assertTrue(r.isInvalid());
    }

    // 7. leading dot -> INVALID -------------------------------------------------
    @Test
    void leadingDot_isInvalid() {
        QualifiedNameClassifier.Result r = QualifiedNameClassifier.classify(".bar");
        assertTrue(r.isInvalid());
        assertTrue(r.qualified().isEmpty());
    }

    // 8. trailing / double dot -> INVALID --------------------------------------
    @Test
    void trailingOrDoubleDot_isInvalid() {
        assertTrue(QualifiedNameClassifier.classify("foo.").isInvalid());
        assertTrue(QualifiedNameClassifier.classify("foo..bar").isInvalid());
        // single dot alone
        assertTrue(QualifiedNameClassifier.classify(".").isInvalid());
    }

    // Bonus: baseOffset shifts ranges -----------------------------------------
    @Test
    void baseOffsetShiftsRanges() {
        QualifiedNameClassifier.Result r = QualifiedNameClassifier.classify("foo.bar", 100);
        assertTrue(r.isQualifiedName());
        QualifiedNameReference ref = r.qualified().orElseThrow();
        assertEquals(TextRange.of(100, 103), ref.qualifierRange());
        assertEquals(TextRange.of(104, 107), ref.nameRange());
    }

    @Test
    void negativeBaseOffsetRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> QualifiedNameClassifier.classify("foo.bar", -1));
    }

    // Classification is purely syntactic — keywords do not matter:
    @Test
    void keywordsAreTreatedSameAsIdentifiers() {
        // 'this'/'super'/'class'/'true'/'false'/'null'/'value' are all
        // identifiers syntactically; the classifier never queries a symbol table.
        assertTrue(QualifiedNameClassifier.classify("value").isSimpleName());
        assertTrue(QualifiedNameClassifier.classify("pkg.Type").isQualifiedName());
        assertTrue(QualifiedNameClassifier.classify("a.b.c").isQualifiedName());
    }
}
