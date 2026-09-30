package com.eyecode.documentation.content;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentationFrontMatterParserTest {

    private final DocumentationFrontMatterParser parser = new DocumentationFrontMatterParser();

    @Test
    void parsesAllSupportedFields() {
        String source = """
                ---
                id: java/jdk/variables
                title: Variáveis em Java
                summary: Declaração e escopo.
                level: beginner
                duration: 8
                officialDocs:
                  label: JLS 4
                  url: https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html
                related:
                  - java/jdk/string
                  - java/jdk/classes
                ---
                Corpo da página.
                """;
        DocumentationFrontMatterParser.Parsed parsed = parser.parse(source, "java/jdk/variables");

        assertEquals("java/jdk/variables", parsed.metadata().id());
        assertEquals("Variáveis em Java", parsed.metadata().title());
        assertEquals("Declaração e escopo.", parsed.metadata().summary());
        assertEquals("beginner", parsed.metadata().level());
        assertEquals(8, parsed.metadata().duration());
        assertEquals("JLS 4", parsed.metadata().officialDocs().label());
        assertEquals("https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html",
                parsed.metadata().officialDocs().url());
        assertEquals(List.of("java/jdk/string", "java/jdk/classes"), parsed.metadata().related());
        assertEquals("Corpo da página.", parsed.body());
    }

    @Test
    void optionalFieldsMayBeAbsent() {
        String source = """
                ---
                id: java/jdk/minimal
                title: Mínimo
                ---
                Só o básico.
                """;
        DocumentationFrontMatter parsed = parser.parse(source, "java/jdk/minimal").metadata();

        assertEquals("java/jdk/minimal", parsed.id());
        assertNull(parsed.type());
        assertNull(parsed.summary());
        assertNull(parsed.level());
        assertNull(parsed.duration());
        assertNull(parsed.officialDocs());
        assertTrue(parsed.related().isEmpty());
    }

    @Test
    void parsesApiType() {
        String source = """
                ---
                id: java/jdk/java.lang/math
                title: Math
                type: api
                ---
                Métodos matemáticos.
                """;
        assertEquals("api", parser.parse(source, "java/jdk/java.lang/math").metadata().type());
    }

    @Test
    void parsesGuideType() {
        String source = """
                ---
                id: java/spring/core/dependency-injection
                title: Injeção de Dependência
                type: guide
                ---
                Guia prático.
                """;
        assertEquals("guide", parser.parse(source, "java/spring/core/dependency-injection").metadata().type());
    }

    @Test
    void invalidTypeThrows() {
        String source = """
                ---
                id: java/jdk/tipo
                title: Tipo
                type: tutorial
                ---
                """;
        assertThrows(IllegalArgumentException.class, () -> parser.parse(source, "java/jdk/tipo"));
    }

    @Test
    void missingFrontMatterThrows() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("Sem front matter.", "id"));
    }

    @Test
    void unclosedFrontMatterThrows() {
        String source = """
                ---
                id: java/jdk/aberto
                title: Aberto
                nunca fecha
                """;
        assertThrows(IllegalArgumentException.class, () -> parser.parse(source, "java/jdk/aberto"));
    }

    @Test
    void missingTitleThrows() {
        String source = """
                ---
                id: java/jdk/sem-titulo
                ---
                """;
        assertThrows(IllegalArgumentException.class, () -> parser.parse(source, "java/jdk/sem-titulo"));
    }

    @Test
    void invalidDurationThrows() {
        String source = """
                ---
                id: java/jdk/duracao
                title: Duração
                duration: oito
                ---
                """;
        assertThrows(IllegalArgumentException.class, () -> parser.parse(source, "java/jdk/duracao"));
    }

    @Test
    void officialDocsWithoutUrlThrows() {
        String source = """
                ---
                id: java/jdk/docs-incompleto
                title: Docs
                officialDocs:
                  label: Só o rótulo
                ---
                """;
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(source, "java/jdk/docs-incompleto"));
    }

    @Test
    void quotedValuesAreUnwrapped() {
        String source = """
                ---
                id: java/jdk/aspas
                title: "Título com : dois-pontos"
                ---
                """;
        assertEquals("Título com : dois-pontos",
                parser.parse(source, "java/jdk/aspas").metadata().title());
    }
}
