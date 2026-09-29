package com.eyecode.ui.web;

import com.eyecode.documentation.content.DocumentationCatalogEntry;
import com.eyecode.documentation.content.DocumentationContentEngine;
import com.eyecode.documentation.content.DocumentationFrontMatter;
import com.eyecode.documentation.content.DocumentationPage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WebShellDocumentationController {
    private static final Map<String, String> SEGMENT_LABELS = Map.of(
            "java", "Java",
            "jdk", "JDK",
            "spring", "Spring Boot",
            "javafx", "JavaFX",
            "junit", "JUnit");

    private final WebShellSurface surface;
    private final DocumentationContentEngine engine = new DocumentationContentEngine();

    public WebShellDocumentationController(WebShellSurface surface) {
        this.surface = surface;
        surface.registerHandler("docs", "catalog", this::catalog);
        surface.registerHandler("docs", "read", this::read);
    }

    private WebShellEnvelope catalog(WebShellEnvelope message) {
        try {
            List<Map<String, Object>> entries = new ArrayList<>();
            for (DocumentationCatalogEntry entry : engine.catalog()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", entry.id());
                item.put("title", entry.title());
                item.put("type", entry.type());
                item.put("branch", entry.branch());
                if (entry.subgroup() != null) item.put("subgroup", entry.subgroup());
                if (entry.summary() != null) item.put("summary", entry.summary());
                if (entry.level() != null) item.put("level", entry.level());
                if (entry.duration() != null) item.put("duration", entry.duration());
                entries.add(item);
            }
            return message.response(Map.of("entries", entries));
        } catch (RuntimeException exception) {
            return message.error(new WebShellError("DOCS_CATALOG_UNAVAILABLE",
                    safeMessage(exception, "Documentation catalog is unavailable"), true));
        }
    }

    private WebShellEnvelope read(WebShellEnvelope message) {
        try {
            String id = text(message.payload(), "id");
            if (id.isBlank()) {
                return message.error(new WebShellError("DOCS_PAGE_NOT_FOUND",
                        "A documentation identifier is required", true));
            }
            DocumentationPage page = engine.page(id);
            DocumentationFrontMatter metadata = page.metadata();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("id", metadata.id());
            payload.put("title", metadata.title());
            payload.put("type", metadata.type() == null ? "concept" : metadata.type());
            payload.put("html", page.html());
            payload.put("breadcrumb", breadcrumb(metadata.id(), metadata.title()));
            if (metadata.summary() != null) payload.put("summary", metadata.summary());
            if (metadata.level() != null) payload.put("level", metadata.level());
            if (metadata.duration() != null) payload.put("duration", metadata.duration());
            if (metadata.officialDocs() != null) {
                payload.put("officialDocs", Map.of(
                        "label", metadata.officialDocs().label(),
                        "url", metadata.officialDocs().url()));
            }
            List<Map<String, Object>> related = new ArrayList<>();
            for (String relatedId : metadata.related()) {
                try {
                    DocumentationFrontMatter resolved = engine.page(relatedId).metadata();
                    related.add(Map.of("id", resolved.id(), "title", resolved.title()));
                } catch (RuntimeException ignored) {
                }
            }
            payload.put("related", related);
            return message.response(payload);
        } catch (RuntimeException exception) {
            return message.error(new WebShellError("DOCS_PAGE_NOT_FOUND",
                    safeMessage(exception, "Documentation page not found"), true));
        }
    }

    static List<String> breadcrumb(String id, String title) {
        String[] segments = id.split("/");
        List<String> crumbs = new ArrayList<>();
        for (int index = 0; index < segments.length; index++) {
            crumbs.add(index == segments.length - 1 ? title : labelFor(segments[index]));
        }
        return List.copyOf(crumbs);
    }

    private static String labelFor(String segment) {
        if (segment.indexOf('.') >= 0) {
            return segment;
        }
        String known = SEGMENT_LABELS.get(segment);
        if (known != null) return known;
        StringBuilder label = new StringBuilder();
        for (String word : segment.split("-")) {
            if (word.isEmpty()) continue;
            if (!label.isEmpty()) label.append(' ');
            label.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return label.isEmpty() ? segment : label.toString();
    }

    private static String text(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static String safeMessage(RuntimeException exception, String fallback) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? fallback : exception.getMessage();
    }
}
