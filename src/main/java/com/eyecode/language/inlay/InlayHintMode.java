package com.eyecode.language.inlay;

import java.util.Locale;

public enum InlayHintMode {
    NAME,
    TYPE,
    BOTH;

    public static InlayHintMode parse(String value) {
        if (value == null || value.isBlank()) {
            return NAME;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "type" -> TYPE;
            case "both" -> BOTH;
            default -> NAME;
        };
    }
}
