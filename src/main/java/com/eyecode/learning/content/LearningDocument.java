package com.eyecode.learning.content;

public record LearningDocument(
        String identifier,
        LearningMetadata metadata,
        String markdownBody,
        String renderedHtml
) {

    public LearningDocument {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Learning document identifier must not be blank");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Learning document metadata must not be null");
        }
        markdownBody = markdownBody == null ? "" : markdownBody;
        renderedHtml = renderedHtml == null ? "" : renderedHtml;
    }
}
