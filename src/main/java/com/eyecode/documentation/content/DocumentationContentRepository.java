package com.eyecode.documentation.content;

import java.util.ArrayList;
import java.util.List;

public final class DocumentationContentRepository {

    private static final String ROOT = "/documentation/content/";
    private static final String CATALOG = ROOT + "catalog.txt";

    private final DocumentationResourceLoader resourceLoader;
    private final DocumentationFrontMatterParser frontMatterParser = new DocumentationFrontMatterParser();

    public DocumentationContentRepository() {
        this(new DocumentationResourceLoader());
    }

    DocumentationContentRepository(DocumentationResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String load(String identifier) {
        return resourceLoader.load(resourcePath(identifier));
    }

    public DocumentationFrontMatter loadFrontMatter(String identifier) {
        return frontMatterParser.parse(load(identifier), identifier).metadata();
    }

    public List<String> catalogIdentifiers() {
        String raw = resourceLoader.load(CATALOG);
        List<String> identifiers = new ArrayList<>();
        for (String line : raw.split("\\R")) {
            String identifier = line.trim();
            if (identifier.isEmpty() || identifier.startsWith("#")) {
                continue;
            }
            identifiers.add(identifier);
        }
        return List.copyOf(identifiers);
    }

    public String resourcePath(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Documentation identifier must not be blank");
        }
        String normalized = identifier.replace('\\', '/').replaceAll("^/+|/+$", "");
        if (normalized.isBlank() || normalized.contains("..")) {
            throw new IllegalArgumentException("Invalid documentation identifier: " + identifier);
        }
        return ROOT + normalized + ".md";
    }
}
