package com.eyecode.learning.content;

import java.util.Optional;

public final class LearningLink {

    private static final String PREFIX = "eyecode://learn/";

    private LearningLink() {
    }

    public static String toUri(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Learning link identifier must not be blank");
        }
        return PREFIX + identifier;
    }

    public static Optional<String> identifier(String uri) {
        if (uri == null || !uri.startsWith(PREFIX)) {
            return Optional.empty();
        }
        String identifier = uri.substring(PREFIX.length());
        return identifier.isBlank() || identifier.contains("..")
                ? Optional.empty()
                : Optional.of(identifier);
    }
}
