package com.eyecode.lessons.content;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LessonMarkdownNormalizerTest {
    private final LessonMarkdownNormalizer normalizer = new LessonMarkdownNormalizer();

    @Test void normalizesSupportedBlocksAndInlineContent() {
        List<LessonContentBlock> blocks = normalizer.normalize("# Título\n\nTexto com **forte**, *ênfase* e `código`.\n\n- um\n- dois\n\n1. primeiro\n2. segundo\n\n```java\nint score = 100;\n```\n\n> Nota importante");

        assertEquals(LessonContentBlockType.HEADING, blocks.get(0).type());
        assertEquals("Título", blocks.get(0).text());
        assertEquals(List.of(LessonInlineContentType.TEXT, LessonInlineContentType.STRONG,
                        LessonInlineContentType.TEXT, LessonInlineContentType.EMPHASIS,
                        LessonInlineContentType.TEXT, LessonInlineContentType.CODE, LessonInlineContentType.TEXT),
                blocks.get(1).inlineContent().stream().map(LessonInlineContent::type).toList());
        assertFalse(blocks.get(2).ordered());
        assertEquals(List.of("um", "dois"), blocks.get(2).items());
        assertTrue(blocks.get(3).ordered());
        assertEquals("java", blocks.get(4).language());
        assertEquals("int score = 100;\n", blocks.get(4).code());
        assertEquals(LessonContentBlockType.CALLOUT, blocks.get(5).type());
    }

    @Test void preservesSafeLinksAndDropsUnsafeMarkup() {
        List<LessonContentBlock> blocks = normalizer.normalize("[Documentação](https://example.com/docs) e [perigo](javascript:alert(1)).\n\n<script>alert('x')</script>\n\nOlá, compilação e execução.");

        LessonContentBlock links = blocks.getFirst();
        assertEquals("https://example.com/docs", links.inlineContent().stream()
                .filter(part -> part.type() == LessonInlineContentType.LINK).findFirst().orElseThrow().url());
        assertFalse(links.inlineContent().stream().anyMatch(part -> part.type() == LessonInlineContentType.LINK
                && part.url().startsWith("javascript:")));
        assertEquals("Olá, compilação e execução.", blocks.getLast().text());
        assertFalse(blocks.stream().anyMatch(block -> (block.text() != null && block.text().contains("<script>"))
                || (block.code() != null && block.code().contains("<script>"))));
    }
}
