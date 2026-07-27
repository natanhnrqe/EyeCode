package com.eyecode.learning.service;

import com.eyecode.learning.content.LearningResourceLoader;
import com.eyecode.learning.model.LearningConcept;
import java.util.Optional;

public final class MarkdownResourceReader {

    private MarkdownResourceReader() {}

    public static Optional<String> readMarkdown(LearningConcept concept) {
        if (concept == null || concept.getPage() == null) {
            return Optional.empty();
        }
        String resourcePath = concept.getPage().getResourcePath();
        if (resourcePath == null || resourcePath.isBlank()) {
            return Optional.empty();
        }
        var loader = new LearningResourceLoader();
        try {
            String content = loader.load(resourcePath);
            return Optional.of(content);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
