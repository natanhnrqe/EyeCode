package com.eyecode.language.hover;

public record HoverContent(String kind, String value) {
    public HoverContent {
        kind = kind == null || kind.isBlank() ? "plaintext" : kind;
        value = value == null ? "" : value;
    }
}
