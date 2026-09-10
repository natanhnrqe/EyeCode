package com.eyecode.lessons.content;

import java.util.List;

public record LessonContentBlock(LessonContentBlockType type, String text, String title,
                                 String language, String code, List<String> items,
                                 List<LessonInlineContent> inlineContent, boolean ordered) {
    public LessonContentBlock(LessonContentBlockType type, String text, String title,
                              String language, String code, List<String> items) {
        this(type, text, title, language, code, items, List.of(), false);
    }

    public LessonContentBlock {
        if (type == null) throw new IllegalArgumentException("Bloco de conteúdo inválido");
        items = items == null ? List.of() : List.copyOf(items);
        inlineContent = inlineContent == null ? List.of() : List.copyOf(inlineContent);
        if ((type == LessonContentBlockType.HEADING || type == LessonContentBlockType.PARAGRAPH)
                && (text == null || text.isBlank()) && inlineContent.isEmpty()) throw new IllegalArgumentException("Texto do bloco obrigatório");
        if (type == LessonContentBlockType.CODE && (code == null || code.isBlank())) {
            throw new IllegalArgumentException("Código do bloco obrigatório");
        }
        if (type == LessonContentBlockType.LIST && items.isEmpty()) throw new IllegalArgumentException("Lista vazia");
        if (type == LessonContentBlockType.CALLOUT && (text == null || text.isBlank()) && inlineContent.isEmpty()) {
            throw new IllegalArgumentException("Texto do destaque obrigatório");
        }
    }
}
