package com.eyecode.javafx.learning;

import com.eyecode.learning.content.LearningDepth;
import com.eyecode.learning.content.LearningMetadata;
import com.eyecode.learning.content.LearningKind;

public record LearningCardSizingPolicy(
        double width,
        double minHeight,
        double preferredHeight,
        double maxHeight
) {

    private static final LearningCardSizingPolicy QUICK =
            new LearningCardSizingPolicy(600, 220, 300, 360);
    private static final LearningCardSizingPolicy FULL =
            new LearningCardSizingPolicy(600, 360, 500, 620);
    private static final LearningCardSizingPolicy MEMBER =
            new LearningCardSizingPolicy(600, 300, 420, 540);

    public static LearningCardSizingPolicy forDepth(LearningDepth depth) {
        return depth == LearningDepth.QUICK ? QUICK : FULL;
    }

    public static LearningCardSizingPolicy forMetadata(LearningMetadata metadata) {
        if (metadata != null && metadata.kind() == LearningKind.MEMBER) {
            return MEMBER;
        }
        return forDepth(metadata == null ? LearningDepth.FULL : metadata.depth());
    }
}
