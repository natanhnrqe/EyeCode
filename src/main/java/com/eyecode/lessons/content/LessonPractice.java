package com.eyecode.lessons.content;

import java.util.List;
import java.util.Objects;

public record LessonPractice(String id, List<LessonInlineContent> instruction, List<LessonFile> files, String entryFileId) {
    public LessonPractice {
        instruction = instruction == null ? List.of() : List.copyOf(instruction);
        files = files == null ? List.of() : List.copyOf(files);
        if (id == null || id.isBlank() || instruction.isEmpty() || files.isEmpty()
                || files.stream().anyMatch(Objects::isNull)
                || files.stream().map(LessonFile::id).distinct().count() != files.size()
                || files.stream().noneMatch(file -> file.id().equals(entryFileId))) {
            throw new IllegalArgumentException("Prática de aula inválida");
        }
    }

    public LessonPractice(String id, List<LessonInlineContent> instruction, LessonFile file) {
        this(id, instruction, List.of(Objects.requireNonNull(file)), file.id());
    }

    public LessonPractice(String id, List<LessonInlineContent> instruction, String starterCode) {
        this(id, instruction, new LessonFile("main", "Main.java", "java", starterCode, false));
    }

    public String starterCode() {
        return file().starterCode();
    }

    public LessonFile file() {
        return files.stream().filter(file -> file.id().equals(entryFileId)).findFirst().orElseThrow();
    }
}
