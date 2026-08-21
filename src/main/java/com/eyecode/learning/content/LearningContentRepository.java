package com.eyecode.learning.content;

public final class LearningContentRepository {

    private static final String ROOT = "/learning/content/";

    private final LearningResourceLoader resourceLoader;
    private final LearningFrontMatterParser frontMatterParser = new LearningFrontMatterParser();

    public LearningContentRepository() {
        this(new LearningResourceLoader());
    }

    LearningContentRepository(LearningResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String load(String identifier) {
        return resourceLoader.load(resourcePath(identifier));
    }

    public LearningDocument loadDocument(String identifier) {
        String markdown = load(identifier);
        LearningFrontMatterParser.Parsed parsed = frontMatterParser.parse(markdown, identifier);
        return new LearningDocument(identifier, parsed.metadata(), parsed.body(), "");
    }

    public String resourcePath(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Learning content identifier must not be blank");
        }
        String normalized = identifier.replace('\\', '/').replaceAll("^/+|/+$", "");
        if (normalized.isBlank() || normalized.contains("..")) {
            throw new IllegalArgumentException("Invalid learning content identifier: " + identifier);
        }
        return ROOT + normalized + ".md";
    }
}
