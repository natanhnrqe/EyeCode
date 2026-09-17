package com.eyecode.lessons.content;

import java.util.List;
import java.util.Objects;

public record LessonWorkspace(List<LessonFile> files, String entryFileId, String mainClass) {
    public LessonWorkspace(List<LessonFile> files, String entryFileId) {
        this(files, entryFileId, mainClassFor(files, entryFileId));
    }
    public LessonWorkspace {
        files = files == null ? List.of() : List.copyOf(files);
        if (files.isEmpty() || files.stream().anyMatch(Objects::isNull)
                || files.stream().map(LessonFile::id).distinct().count() != files.size()
                || files.stream().noneMatch(file -> file.id().equals(entryFileId))) {
            throw new IllegalArgumentException("Workspace de aula inválido");
        }
        if (mainClass == null || mainClass.isBlank()) throw new IllegalArgumentException("Classe principal da aula inválida");
    }

    static LessonWorkspace from(LessonPractice practice) {
        return new LessonWorkspace(practice.files(), practice.entryFileId(), practice.mainClass());
    }

    private static String mainClassFor(List<LessonFile> files, String entryFileId) {
        if (files == null) return "";
        return files.stream().filter(file -> file.id().equals(entryFileId)).findFirst()
                .map(file -> file.name().replace('\\', '/').replaceAll("\\.java$", "").replace('/', '.')).orElse("");
    }
}
