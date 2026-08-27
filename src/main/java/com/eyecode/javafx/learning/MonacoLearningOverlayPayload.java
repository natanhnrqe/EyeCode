package com.eyecode.javafx.learning;

import com.eyecode.learning.content.DocumentationTarget;
import com.eyecode.learning.content.LearningMember;
import com.eyecode.learning.content.LearningMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;

record MonacoLearningOverlayPayload(
        String title,
        String subtitle,
        String sizeClass,
        String iconKind,
        String iconUrl,
        List<Item> breadcrumb,
        String renderedBodyHtml,
        List<Item> commonMethods,
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
        return from(metadata, ancestors, bodyHtml, List.of(), related, docs, sourceAvailable);
    }

    static MonacoLearningOverlayPayload from(LearningMetadata metadata,
                                              List<LearningMetadata> ancestors,
                                              String bodyHtml,
                                              List<LearningMember> commonMethods,
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
        if (!breadcrumb.isEmpty()) {
            breadcrumb = java.util.stream.Stream.concat(
                            breadcrumb.stream(), java.util.stream.Stream.of(new Item(metadata.id(), metadata.title())))
                    .toList();
        }
        List<Item> commonMethodItems = commonMethods.stream()
                .map(item -> new Item(item.identifier(), item.label()))
                .toList();
        List<Item> relatedItems = related.stream()
                .map(item -> new Item(item.id(), item.title()))
                .toList();
        return new MonacoLearningOverlayPayload(metadata.title(), subtitle,
                LearningCardSizingPolicy.classFor(metadata).name().toLowerCase(),
                iconKind(metadata), iconUrl(iconKind(metadata)), breadcrumb,
                bodyHtml, commonMethodItems, relatedItems, sourceAvailable, docs != null);
    }

    private static String iconUrl(String kind) {
        String resource = switch (kind) {
            case "METHOD" -> "/icons/completion/method.svg";
            case "FIELD" -> "/icons/completion/field.svg";
            case "INTERFACE" -> "/icons/completion/interface.svg";
            case "KEYWORD" -> "/icons/completion/keyword.svg";
            default -> "/icons/completion/class.svg";
        };
        try (InputStream stream = MonacoLearningOverlayPayload.class.getResourceAsStream(resource)) {
            if (stream == null) return "";
            return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(stream.readAllBytes());
        } catch (IOException ignored) {
            return "";
        }
    }

    private static String iconKind(LearningMetadata metadata) {
        if (metadata.kind() == com.eyecode.learning.content.LearningKind.MEMBER) return "METHOD";
        return switch (metadata.concept().toLowerCase()) {
            case "class", "object", "string", "array-list", "linked-list", "hash-map" -> "CLASS";
            case "interface", "list", "map" -> "INTERFACE";
            case "enum" -> "ENUM";
            case "record" -> "RECORD";
            default -> metadata.kind() == com.eyecode.learning.content.LearningKind.SYNTAX
                    ? "KEYWORD" : "LEARNING";
        };
    }

    String json() {
        StringBuilder result = new StringBuilder("{");
        field(result, "title", title).append(',');
        field(result, "subtitle", subtitle).append(',');
        field(result, "sizeClass", sizeClass).append(',');
        field(result, "iconKind", iconKind).append(',');
        field(result, "iconUrl", iconUrl).append(',');
        result.append("\"breadcrumb\":").append(itemsJson(breadcrumb)).append(',');
        field(result, "renderedBodyHtml", renderedBodyHtml).append(',');
        result.append("\"commonMethods\":").append(itemsJson(commonMethods)).append(',');
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
