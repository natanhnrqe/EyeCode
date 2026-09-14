package com.eyecode.lessons.content;

public record LessonFile(String id, String name, String language, String starterCode, boolean readOnly) {
    public LessonFile {
        if (id == null || id.isBlank() || name == null || name.isBlank()
                || language == null || language.isBlank() || starterCode == null) {
            throw new IllegalArgumentException("Arquivo de prática inválido");
        }
    }
}
