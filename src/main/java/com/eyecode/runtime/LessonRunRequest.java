package com.eyecode.runtime;

import java.util.List;

public record LessonRunRequest(List<SourceFile> files, String mainClass) {
    public record SourceFile(String name, String source) {
        public SourceFile {
            if (name == null || name.isBlank() || source == null) throw new IllegalArgumentException("Lesson source is required");
        }
    }

    public LessonRunRequest {
        files = files == null ? List.of() : List.copyOf(files);
        if (files.isEmpty() || mainClass == null || mainClass.isBlank()) {
            throw new IllegalArgumentException("Lesson workspace and main class are required");
        }
    }
}
