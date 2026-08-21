package com.eyecode.learning.content;

import java.util.List;

public record LearningMetadata(
        String id,
        String title,
        String concept,
        String level,
        int duration,
        String category,
        DocumentationTarget officialDocs,
        List<String> related,
        String next
) {

    public LearningMetadata {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Learning metadata id must not be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Learning metadata title must not be blank");
        }
        if (concept == null || concept.isBlank()) {
            throw new IllegalArgumentException("Learning metadata concept must not be blank");
        }
        if (level == null || level.isBlank()) {
            throw new IllegalArgumentException("Learning metadata level must not be blank");
        }
        if (duration < 0) {
            throw new IllegalArgumentException("Learning metadata duration must not be negative");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Learning metadata category must not be blank");
        }
        related = related == null ? List.of() : List.copyOf(related);
    }
}
