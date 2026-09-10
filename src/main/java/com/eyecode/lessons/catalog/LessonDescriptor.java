package com.eyecode.lessons.catalog;

import com.eyecode.lessons.content.LessonKind;

import java.util.List;

public record LessonDescriptor(String id, String title, String description, String categoryId,
                               String topicId, LessonDifficulty difficulty, int estimatedMinutes,
                               LessonKind kind, List<String> concepts) {
    public LessonDescriptor {
        concepts = List.copyOf(concepts);
    }
}
