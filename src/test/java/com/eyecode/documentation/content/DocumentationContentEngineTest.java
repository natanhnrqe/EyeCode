package com.eyecode.documentation.content;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentationContentEngineTest {

    private final DocumentationContentEngine engine = new DocumentationContentEngine();

    @Test
    void catalogExposesEveryPageWithItsBranch() {
        List<DocumentationCatalogEntry> catalog = engine.catalog();

        assertEquals(29, catalog.size());
        DocumentationCatalogEntry first = catalog.getFirst();
        assertEquals("java/jdk/fundamentos/variables", first.id());
        assertEquals("Variáveis em Java", first.title());
        assertEquals("jdk", first.branch());
        assertEquals("fundamentos", first.subgroup());
        assertEquals("spring", branchOf(catalog, "java/spring/boot-basics"));
        assertEquals("junit", branchOf(catalog, "java/junit/first-test"));
    }

    @Test
    void everyCatalogPageRendersToHtml() {
        List<DocumentationCatalogEntry> catalog = engine.catalog();

        assertEquals(29, catalog.size());
        for (DocumentationCatalogEntry entry : catalog) {
            DocumentationPage page = engine.page(entry.id());
            assertTrue(page.html().contains("<h2 id="), () -> "sem seções: " + entry.id());
            assertFalse(page.html().contains("> [!INFO]"), () -> "callout cru: " + entry.id());
            assertFalse(page.html().contains("> [!WARNING]"), () -> "callout cru: " + entry.id());
            assertNotNull(page.metadata().officialDocs(), () -> "sem officialDocs: " + entry.id());
            assertFalse(page.metadata().related().isEmpty(), () -> "sem related: " + entry.id());
            for (String related : page.metadata().related()) {
                assertEquals(related, engine.page(related).metadata().id(),
                        () -> "related inexistente em " + entry.id() + ": " + related);
            }
        }
    }

    @Test
    void pageRendersMarkdownIntoDidacticHtml() {
        DocumentationPage page = engine.page("java/jdk/fundamentos/variables");
        String html = page.html();

        assertTrue(html.contains("<h2 id=\"por-que-existe\">"), () -> "sem âncora: " + html.substring(0, Math.min(400, html.length())));
        assertTrue(html.contains("<h2 id=\"profundidade\">"));
        assertTrue(html.contains("<pre>"));
        assertTrue(html.contains("<table>"));
        assertTrue(html.contains("class=\"docs-callout docs-callout-info\""));
        assertFalse(html.contains("> [!INFO]"));
        assertFalse(html.contains("> [!WARNING]"));
    }

    @Test
    void pageCarriesFrontMatterMetadata() {
        DocumentationPage page = engine.page("java/jdk/java.lang/string");

        assertEquals("java/jdk/java.lang/string", page.metadata().id());
        assertEquals("String", page.metadata().title());
        assertEquals("api", page.metadata().type());
        assertEquals("API java.lang.String", page.metadata().officialDocs().label());
        assertTrue(page.metadata().related().contains("java/jdk/fundamentos/variables"));
    }

    @Test
    void relatedPagesResolveThroughTheEngine() {
        DocumentationPage page = engine.page("java/spring/boot-basics");

        List<String> related = page.metadata().related();
        for (String id : related) {
            assertEquals(id, engine.page(id).metadata().id());
        }
    }

    @Test
    void duplicateHeadingSlugsGetSuffixes() {
        String html = engine.render("## Seção\n\n## Seção\n\n## Outra");

        assertTrue(html.contains("id=\"secao\""));
        assertTrue(html.contains("id=\"secao-2\""));
        assertTrue(html.contains("id=\"outra\""));
    }

    @Test
    void headingInsideInlineMarkupIsStrippedForSlug() {
        String html = engine.render("## Anatomia da `sintaxe`");

        assertTrue(html.contains("<h2 id=\"anatomia-da-sintaxe\">"));
    }

    @Test
    void unknownPageThrows() {
        assertThrows(IllegalArgumentException.class, () -> engine.page("java/jdk/nao-existe"));
    }

    @Test
    void pageWithMismatchedFrontMatterIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> engine.page("java/jdk/variaveis"));
    }

    @Test
    void slugifyNormalizesText() {
        assertEquals("por-que-existe", DocumentationContentEngine.slugify("Por que existe?"));
        assertEquals("armadilhas-comuns", DocumentationContentEngine.slugify("  Armadilhas  comuns! "));
        assertEquals("", DocumentationContentEngine.slugify("???"));
    }

    @Test
    void renderIsDeterministicForTheSameInput() {
        String markdown = "## Título\n\nTexto com **negrito**.";
        assertEquals(engine.render(markdown), engine.render(markdown));
    }

    @Test
    void calloutRendersInlineMarkdown() {
        String html = engine.render("> [!INFO] Toda janela é um **Stage** contendo uma **Scene**.");

        assertTrue(html.contains("class=\"docs-callout docs-callout-info\""));
        assertTrue(html.contains("<strong>Scene</strong>"), () -> "sem negrito: " + html);
        assertFalse(html.contains("[!INFO]"));
    }

    @Test
    void markerOnlyCalloutRendersFollowingLineMarkdown() {
        String html = engine.render("> [!WARNING]\n> Use **.equals()** para comparar texto.");

        assertTrue(html.contains("class=\"docs-callout docs-callout-warning\""));
        assertTrue(html.contains("<strong>.equals()</strong>"), () -> "sem negrito: " + html);
    }

    @Test
    void regularBlockquoteStaysBlockquote() {
        String html = engine.render("> Uma citação comum.");

        assertTrue(html.contains("<blockquote>"));
        assertFalse(html.contains("docs-callout"));
    }

    @Test
    void fencedJavaCodeBlockCarriesLanguageClass() {
        String html = engine.render("```java\nint x = 1;\n```");

        assertTrue(html.contains("<code class=\"language-java\">"), () -> html);
        assertTrue(html.contains("<pre>"));
    }

    @Test
    void catalogEntryDerivesTypeAndSubgroup() {
        DocumentationCatalogEntry api = DocumentationCatalogEntry.from(new DocumentationFrontMatter(
                "java/jdk/java.lang/string", "String", "api", "Texto", "beginner", 6, null, List.of()));
        assertEquals("java.lang", api.subgroup());
        assertEquals("api", api.type());
        assertEquals("jdk", api.branch());

        DocumentationCatalogEntry concept = DocumentationCatalogEntry.from(new DocumentationFrontMatter(
                "java/jdk/fundamentos/variables", "Variáveis", null, null, null, null, null, List.of()));
        assertEquals("fundamentos", concept.subgroup());
        assertEquals("concept", concept.type());

        DocumentationCatalogEntry flat = DocumentationCatalogEntry.from(new DocumentationFrontMatter(
                "java/spring/boot-basics", "Spring Boot", "concept", null, null, null, null, List.of()));
        assertNull(flat.subgroup());
        assertEquals("spring", flat.branch());
    }

    private static String branchOf(List<DocumentationCatalogEntry> catalog, String id) {
        return catalog.stream().filter(entry -> entry.id().equals(id)).findFirst().orElseThrow().branch();
    }
}
