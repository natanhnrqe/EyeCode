package com.eyecode.learning.model;

public record LearningCardHeaderData(String iconKey, String title, String subtitle) {
    public LearningCardHeaderData(String title, String subtitle) {
        this("java", title, subtitle);
    }
}
