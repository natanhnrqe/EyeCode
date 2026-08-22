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
}
