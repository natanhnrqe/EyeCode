package com.eyecode.learning.service;

import com.eyecode.learning.model.LearningConcept;
import java.util.Optional;

public final class MarkdownCodeExampleExtractor implements CodeExampleExtractor {

    @Override
    public Optional<String> extractFirstExample(LearningConcept concept) {
        if (concept == null || concept.getPage() == null) {
            return Optional.empty();
        }
        String resourcePath = concept.getPage().getResourcePath();
        if (resourcePath == null || resourcePath.isBlank()) {
            return Optional.empty();
        }
        try {
            Optional<String> mdOpt = MarkdownResourceReader.readMarkdown(concept);
            if (mdOpt.isPresent()) {
                return extractFirstJavaBlock(mdOpt.get());
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<String> extractFirstJavaBlock(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return Optional.empty();
        }
        String[] lines = markdown.split("\\n");
        StringBuilder currentBlock = new StringBuilder();
        boolean inBlock = false;
        boolean first = true;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("```java") || trimmed.startsWith("```")) {
                if (!inBlock && trimmed.startsWith("```java")) {
                    inBlock = true;
                    first = true;
                    currentBlock.setLength(0);
                } else if (inBlock && trimmed.startsWith("```")) {
                    String result = currentBlock.toString().trim();
                    if (!result.isEmpty() && first) {
                        return Optional.of(result);
                    }
                    inBlock = false;
                    currentBlock.setLength(0);
                    first = false;
                }
                continue;
            }
            if (inBlock) {
                currentBlock.append(line).append("\n");
            }
        }
        return Optional.empty();
    }
}
