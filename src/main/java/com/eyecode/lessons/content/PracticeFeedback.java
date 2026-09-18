package com.eyecode.lessons.content;

public record PracticeFeedback(String successMessage) {
    public PracticeFeedback {
        if (successMessage == null || successMessage.isBlank()) {
            throw new IllegalArgumentException("Feedback de prática inválido");
        }
    }
}
