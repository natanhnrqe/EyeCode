package com.eyecode.ui.web;

import com.eyecode.documentation.content.DocumentationContentEngine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebShellDocumentationControllerTest {

    @Test
    void catalogReturnsEveryPageGroupedByBranch() {
        Surface surface = new Surface();
        new WebShellDocumentationController(surface);

        WebShellEnvelope response = surface.handler("docs", "catalog")
                .handle(WebShellEnvelope.request("docs", "catalog", "r1", Map.of()));

        assertNull(response.error());
        List<?> entries = (List<?>) response.payload().get("entries");
        assertEquals(67, entries.size());
        Map<?, ?> first = (Map<?, ?>) entries.getFirst();
        assertEquals("java/jdk/fundamentos/variables", first.get("id"));
        assertEquals("Variáveis em Java", first.get("title"));
        assertEquals("concept", first.get("type"));
        assertEquals("jdk", first.get("branch"));
        assertEquals("fundamentos", first.get("subgroup"));
        assertNotNull(first.get("summary"));
        assertEquals("beginner", first.get("level"));
        assertEquals(8, first.get("duration"));
    }

    @Test
    void catalogCarriesSubgroupForPackagePages() {
        Surface surface = new Surface();
        new WebShellDocumentationController(surface);

        WebShellEnvelope response = surface.handler("docs", "catalog")
                .handle(WebShellEnvelope.request("docs", "catalog", "r6", Map.of()));

        Map<?, ?> apiPage = ((List<?>) response.payload().get("entries")).stream()
                .map(entry -> (Map<?, ?>) entry)
                .filter(entry -> "api".equals(entry.get("type")))
                .findFirst()
                .orElse(null);
        assertNotNull(apiPage, "catalog must contain at least one type: api page");
        assertEquals("java.lang", apiPage.get("subgroup"));
    }

    @Test
    void readReturnsFullPagePayload() {
        Surface surface = new Surface();
        new WebShellDocumentationController(surface);

        WebShellEnvelope response = surface.handler("docs", "read")
                .handle(WebShellEnvelope.request("docs", "read", "r2",
                        Map.of("id", "java/jdk/fundamentos/variables")));

        assertNull(response.error());
        Map<String, Object> payload = response.payload();
        assertEquals("java/jdk/fundamentos/variables", payload.get("id"));
        assertEquals("Variáveis em Java", payload.get("title"));
        assertEquals("concept", payload.get("type"));
        String html = String.valueOf(payload.get("html"));
        assertTrue(html.contains("<h2 id=\"por-que-existe\">"));
        assertTrue(html.contains("docs-callout"));
        assertEquals(List.of("Java", "JDK", "Fundamentos", "Variáveis em Java"),
                payload.get("breadcrumb"));

        Map<?, ?> officialDocs = (Map<?, ?>) payload.get("officialDocs");
        assertNotNull(officialDocs);
        assertEquals("JLS 4 — Variables", officialDocs.get("label"));

        List<?> related = (List<?>) payload.get("related");
        assertEquals(2, related.size());
        assertEquals("String", ((Map<?, ?>) related.getFirst()).get("title"));
    }

    @Test
    void readUnknownPageReturnsNotFoundError() {
        Surface surface = new Surface();
        new WebShellDocumentationController(surface);

        WebShellEnvelope response = surface.handler("docs", "read")
                .handle(WebShellEnvelope.request("docs", "read", "r3",
                        Map.of("id", "java/jdk/nao-existe")));

        assertNotNull(response.error());
        assertEquals("DOCS_PAGE_NOT_FOUND", response.error().code());
    }

    @Test
    void readWithoutIdentifierReturnsError() {
        Surface surface = new Surface();
        new WebShellDocumentationController(surface);

        WebShellEnvelope response = surface.handler("docs", "read")
                .handle(WebShellEnvelope.request("docs", "read", "r4", Map.of()));

        assertNotNull(response.error());
        assertEquals("DOCS_PAGE_NOT_FOUND", response.error().code());
    }

    @Test
    void readMismatchedFrontMatterIdReturnsError() {
        Surface surface = new Surface();
        new WebShellDocumentationController(surface);

        WebShellEnvelope response = surface.handler("docs", "read")
                .handle(WebShellEnvelope.request("docs", "read", "r5",
                        Map.of("id", "java/jdk/errado")));

        assertNotNull(response.error());
        assertEquals("DOCS_PAGE_NOT_FOUND", response.error().code());
    }

    @Test
    void breadcrumbMapsKnownSegmentsAndFallsBackToTitleCase() {
        assertEquals(List.of("Java", "Spring Boot", "Primeiros passos"),
                WebShellDocumentationController.breadcrumb("java/spring/boot-basics", "Primeiros passos"));
        assertEquals(List.of("Java", "JDK", "Variáveis"),
                WebShellDocumentationController.breadcrumb("java/jdk/variables", "Variáveis"));
        assertEquals(List.of("Java", "Meu Topico", "Guia"),
                WebShellDocumentationController.breadcrumb("java/meu-topico/guia", "Guia"));
        assertEquals(List.of("Java", "JDK", "java.lang", "String"),
                WebShellDocumentationController.breadcrumb("java/jdk/java.lang/string", "String"));
    }

    @Test
    void engineCatalogIsStableAcrossControllerInstances() {
        Surface first = new Surface();
        Surface second = new Surface();
        new WebShellDocumentationController(first);
        new WebShellDocumentationController(second);

        List<?> a = (List<?>) first.handler("docs", "catalog")
                .handle(WebShellEnvelope.request("docs", "catalog", "a", Map.of()))
                .payload().get("entries");
        List<?> b = (List<?>) second.handler("docs", "catalog")
                .handle(WebShellEnvelope.request("docs", "catalog", "b", Map.of()))
                .payload().get("entries");

        assertEquals(a.size(), b.size());
        assertEquals(new DocumentationContentEngine().catalog().size(), a.size());
    }

    private static final class Surface implements WebShellSurface {
        private final Map<String, WebShellMessageHandler> handlers = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public void send(WebShellEnvelope message) {
        }

        @Override
        public void registerHandler(String channel, String name, WebShellMessageHandler handler) {
            handlers.put(channel + '/' + name, handler);
        }

        private WebShellMessageHandler handler(String channel, String name) {
            return handlers.get(channel + '/' + name);
        }
    }
}
