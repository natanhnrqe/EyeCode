package com.eyecode.javafx.learning;

import com.eyecode.learning.content.DocumentationTarget;
import com.eyecode.learning.content.LearningMetadata;

import java.util.List;

record MonacoLearningOverlayPayload(
        String title,
        String subtitle,
        List<Item> breadcrumb,
        String renderedBodyHtml,
        List<Item> relatedItems,
        boolean sourceAvailable,
        boolean docsAvailable
) {
    record Item(String id, String title) { }

    static MonacoLearningOverlayPayload from(LearningMetadata metadata,
                                              List<LearningMetadata> ancestors,
                                              String bodyHtml,
                                              List<LearningMetadata> related,
                                              DocumentationTarget docs,
                                              boolean sourceAvailable) {
        String subtitle = String.join(" · ",
                metadata.category().toUpperCase(),
                metadata.level().toUpperCase(),
                metadata.duration() + " MIN");
        List<Item> breadcrumb = ancestors.stream()
                .map(item -> new Item(item.id(), item.title()))
                .toList();
        breadcrumb = java.util.stream.Stream.concat(
                        breadcrumb.stream(), java.util.stream.Stream.of(new Item(metadata.id(), metadata.title())))
                .toList();
        List<Item> relatedItems = related.stream()
                .map(item -> new Item(item.id(), item.title()))
                .toList();
        return new MonacoLearningOverlayPayload(metadata.title(), subtitle, breadcrumb,
                bodyHtml, relatedItems, sourceAvailable, docs != null);
    }

    String json() {
        StringBuilder result = new StringBuilder("{");
        field(result, "title", title).append(',');
        field(result, "subtitle", subtitle).append(',');
        result.append("\"breadcrumb\":").append(itemsJson(breadcrumb)).append(',');
        field(result, "renderedBodyHtml", renderedBodyHtml).append(',');
        result.append("\"relatedItems\":").append(itemsJson(relatedItems)).append(',');
        result.append("\"sourceAvailable\":").append(sourceAvailable).append(',');
        result.append("\"docsAvailable\":").append(docsAvailable).append('}');
        return result.toString();
    }

    private static StringBuilder field(StringBuilder result, String key, String value) {
        return result.append('"').append(escape(key)).append("\":\"").append(escape(value)).append('"');
    }

    private static String itemsJson(List<Item> items) {
        StringBuilder result = new StringBuilder("[");
        boolean first = true;
        for (Item item : items) {
            if (!first) result.append(',');
            first = false;
            result.append('{');
            field(result, "id", item.id()).append(',');
            field(result, "title", item.title());
            result.append('}');
        }
        return result.append(']').toString();
    }

    private static String escape(String value) {
        String text = value == null ? "" : value;
        StringBuilder result = new StringBuilder(text.length() + 8);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> result.append(c);
            }
        }
        return result.toString();
    }
}
