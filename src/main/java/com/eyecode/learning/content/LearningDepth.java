package com.eyecode.learning.content;

public enum LearningDepth {
    QUICK,
    FULL;

    public static LearningDepth parse(String value) {
        if (value == null || value.isBlank()) {
            return FULL;
        }
        return value.trim().equalsIgnoreCase("quick") ? QUICK : FULL;
    }
}
