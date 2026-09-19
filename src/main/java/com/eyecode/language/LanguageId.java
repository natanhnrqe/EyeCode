package com.eyecode.language;

import java.util.Locale;
import java.util.Optional;

public record LanguageId(String value) {
    public static final LanguageId JAVA = new LanguageId("java");

    public LanguageId {
        value = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank() || !value.matches("[a-z][a-z0-9_-]*")) {
            throw new IllegalArgumentException("Invalid language id: " + value);
        }
    }

    public static Optional<LanguageId> parse(String value) {
        try {
            return value == null || value.isBlank() ? Optional.empty() : Optional.of(new LanguageId(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
