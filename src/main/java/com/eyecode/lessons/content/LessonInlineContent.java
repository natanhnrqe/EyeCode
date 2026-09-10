package com.eyecode.lessons.content;

public record LessonInlineContent(LessonInlineContentType type, String text, String url) {
    public LessonInlineContent {
        if (type == null || text == null || text.isEmpty()) throw new IllegalArgumentException("Conteúdo inline inválido");
        if (type == LessonInlineContentType.LINK && (url == null || url.isBlank())) {
            throw new IllegalArgumentException("Link inline sem URL");
        }
        if (type != LessonInlineContentType.LINK) url = null;
    }
}
