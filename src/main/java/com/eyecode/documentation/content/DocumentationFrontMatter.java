package com.eyecode.documentation.content;

import com.eyecode.learning.content.DocumentationTarget;

import java.util.List;
import java.util.Objects;

public record DocumentationFrontMatter(
        String id,
        String title,
        String type,
        String summary,
        String level,
        Integer duration,
        DocumentationTarget officialDocs,
        List<String> related) {

    public static final String TYPE_CONCEPT = "concept";
    public static final String TYPE_API = "api";

    public DocumentationFrontMatter {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        type = normalizeType(type);
        related = related == null ? List.of() : List.copyOf(related);
    }

    private static String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        String normalized = type.trim();
        if (!TYPE_CONCEPT.equals(normalized) && !TYPE_API.equals(normalized)) {
            throw new IllegalArgumentException("Unsupported documentation type: " + normalized);
        }
        return normalized;
    }
}
