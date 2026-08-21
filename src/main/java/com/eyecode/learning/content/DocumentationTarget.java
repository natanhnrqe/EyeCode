package com.eyecode.learning.content;

public record DocumentationTarget(String label, String url) {

    public DocumentationTarget {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Documentation label must not be blank");
        }
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Documentation URL must not be blank");
        }
    }
}
