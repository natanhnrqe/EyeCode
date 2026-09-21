package com.eyecode.lessons.content;

public record LessonFile(String id, String name, String language, String starterCode, boolean readOnly,
                         LessonEditorRange editableRange) {
    public LessonFile(String id, String name, String language, String starterCode, boolean readOnly) {
        this(id, name, language, starterCode, readOnly, null);
    }

    public LessonFile {
        if (id == null || id.isBlank() || name == null || name.isBlank()
                || language == null || language.isBlank() || starterCode == null) {
            throw new IllegalArgumentException("Arquivo de prática inválido");
        }
    }
}
