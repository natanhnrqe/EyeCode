package com.eyecode.javafx.learning;

import com.eyecode.learning.content.LearningDepth;
import com.eyecode.learning.content.LearningKind;
import com.eyecode.learning.content.LearningMetadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearningCardSizingPolicyTest {

    @Test
    void quickLessonsUseACompactDeterministicSize() {
        LearningCardSizingPolicy sizing = LearningCardSizingPolicy.forDepth(LearningDepth.QUICK);

        assertEquals(600, sizing.width());
        assertEquals(300, sizing.preferredHeight());
        assertTrue(sizing.minHeight() < sizing.preferredHeight());
        assertTrue(sizing.preferredHeight() < sizing.maxHeight());
    }

    @Test
    void fullLessonsKeepTheExpandedCardSize() {
        LearningCardSizingPolicy sizing = LearningCardSizingPolicy.forDepth(LearningDepth.FULL);

        assertEquals(600, sizing.width());
        assertEquals(500, sizing.preferredHeight());
        assertTrue(sizing.minHeight() < sizing.preferredHeight());
        assertTrue(sizing.preferredHeight() < sizing.maxHeight());
    }

    @Test
    void memberLessonsUseTheMediumCardSizeEvenWhenDepthIsQuick() {
        LearningMetadata metadata = new LearningMetadata(
                "java/jdk/string/contains", "String.contains()", "string-contains", "beginner",
                1, "JAVA API", null, java.util.List.of(), null, "java/jdk/string",
                java.util.List.of(), LearningDepth.QUICK, LearningKind.MEMBER);

        LearningCardSizingPolicy sizing = LearningCardSizingPolicy.forMetadata(metadata);

        assertEquals(420, sizing.preferredHeight());
        assertTrue(sizing.preferredHeight() > LearningCardSizingPolicy.forDepth(LearningDepth.QUICK).preferredHeight());
        assertTrue(sizing.preferredHeight() < LearningCardSizingPolicy.forDepth(LearningDepth.FULL).preferredHeight());
    }

    @Test
    void metadataMapsToTheThreeOverlaySizeClasses() {
        assertEquals(LearningCardSizeClass.QUICK, LearningCardSizingPolicy.classFor(
                metadata(LearningKind.SYNTAX, LearningDepth.QUICK)));
        assertEquals(LearningCardSizeClass.MEDIUM, LearningCardSizingPolicy.classFor(
                metadata(LearningKind.MEMBER, LearningDepth.QUICK)));
        assertEquals(LearningCardSizeClass.FULL, LearningCardSizingPolicy.classFor(
                metadata(LearningKind.CONCEPT, LearningDepth.FULL)));
    }

    private static LearningMetadata metadata(LearningKind kind, LearningDepth depth) {
        return new LearningMetadata("java/test", "Test", "class", "beginner", 1,
                "JAVA", null, java.util.List.of(), null, null, java.util.List.of(), depth, kind);
    }
}
