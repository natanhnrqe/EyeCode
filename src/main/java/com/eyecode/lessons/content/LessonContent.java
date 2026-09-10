package com.eyecode.lessons.content;

import java.util.HashSet;
import java.util.List;

public record LessonContent(String id, int version, LessonKind kind, String title, List<LessonStep> steps) {
    public LessonContent {
        if (id == null || id.isBlank() || version < 1 || kind == null || title == null || title.isBlank() || steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("Conteúdo de aula inválido");
        }
        steps = List.copyOf(steps);
        if (steps.stream().map(LessonStep::id).count() != new HashSet<>(steps.stream().map(LessonStep::id).toList()).size()) {
            throw new IllegalArgumentException("IDs de etapas duplicados");
        }
    }
}
