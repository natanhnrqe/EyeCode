package com.eyecode.lessons.content;

import java.util.List;
import java.util.Objects;

public record LessonWorkspace(List<LessonFile> files, String entryFileId) {
    public LessonWorkspace {
        files = files == null ? List.of() : List.copyOf(files);
        if (files.isEmpty() || files.stream().anyMatch(Objects::isNull)
                || files.stream().map(LessonFile::id).distinct().count() != files.size()
                || files.stream().noneMatch(file -> file.id().equals(entryFileId))) {
            throw new IllegalArgumentException("Workspace de aula inválido");
        }
    }

    static LessonWorkspace from(LessonPractice practice) {
        return new LessonWorkspace(practice.files(), practice.entryFileId());
    }
}
