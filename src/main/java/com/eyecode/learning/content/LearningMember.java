package com.eyecode.learning.content;

public record LearningMember(String label, String identifier) {

    public LearningMember {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Learning member label must not be blank");
        }
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Learning member identifier must not be blank");
        }
    }
}
