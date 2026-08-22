package com.eyecode.learning.content;

public enum LearningKind {
    CONCEPT,
    SYNTAX,
    MEMBER;

    public static LearningKind parse(String value) {
        if (value == null || value.isBlank()) {
            return CONCEPT;
        }
        return switch (value.trim().toLowerCase()) {
            case "method", "member" -> MEMBER;
            case "syntax" -> SYNTAX;
            default -> CONCEPT;
        };
    }
}
