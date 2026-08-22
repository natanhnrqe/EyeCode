package com.eyecode.learning.content;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class LearningFrontMatterParser {

    Parsed parse(String source, String identifier) {
        if (source == null) {
            throw new IllegalArgumentException("Learning document is missing front matter: " + identifier);
        }

        String[] lines = source.split("\\R", -1);
        if (lines.length == 0 || !lines[0].trim().equals("---")) {
            throw new IllegalArgumentException("Learning document is missing front matter: " + identifier);
        }
        int end = -1;
        for (int index = 1; index < lines.length; index++) {
            if (lines[index].trim().equals("---")) {
                end = index;
                break;
            }
        }
        if (end < 0) {
            throw new IllegalArgumentException("Learning front matter is not closed: " + identifier);
        }

        Map<String, String> values = new LinkedHashMap<>();
        List<String> related = new ArrayList<>();
        List<LearningMember> members = new ArrayList<>();
        String officialLabel = null;
        String officialUrl = null;
        String section = "";
        for (int index = 1; index < end; index++) {
            String line = lines[index];
            if (line.isBlank()) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.startsWith("- ") && section.equals("related")) {
                related.add(value(trimmed.substring(2)));
                continue;
            }
            if (trimmed.startsWith("- ") && section.equals("members")) {
                String member = value(trimmed.substring(2));
                int separator = member.indexOf(':');
                if (separator <= 0 || separator == member.length() - 1) {
                    throw new IllegalArgumentException("Malformed learning member metadata line: " + line);
                }
                members.add(new LearningMember(
                        value(member.substring(0, separator)),
                        value(member.substring(separator + 1))));
                continue;
            }
            int colon = line.indexOf(':');
            if (colon < 0) {
                throw new IllegalArgumentException("Malformed learning metadata line: " + line);
            }
            String key = line.substring(0, colon).trim();
            String value = value(line.substring(colon + 1));
            if (line.startsWith(" ") || line.startsWith("\t")) {
                if (section.equals("officialDocs") && key.equals("label")) {
                    officialLabel = value;
                } else if (section.equals("officialDocs") && key.equals("url")) {
                    officialUrl = value;
                } else {
                    throw new IllegalArgumentException("Unsupported nested learning metadata: " + key);
                }
            } else if (key.equals("officialDocs")) {
                section = key;
            } else if (key.equals("related")) {
                section = key;
            } else if (key.equals("members")) {
                section = key;
            } else {
                section = "";
                values.put(key, value);
            }
        }

        String id = required(values, "id", identifier);
        String title = required(values, "title", identifier);
        String concept = required(values, "concept", identifier);
        String level = required(values, "level", identifier);
        String category = required(values, "category", identifier);
        int duration;
        try {
            duration = Integer.parseInt(required(values, "duration", identifier));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid learning duration: " + identifier, exception);
        }
        DocumentationTarget docs = officialLabel == null && officialUrl == null
                ? null
                : new DocumentationTarget(
                        requiredValue(officialLabel, "officialDocs.label", identifier),
                        requiredValue(officialUrl, "officialDocs.url", identifier));
        String body = String.join("\n", Arrays.asList(lines).subList(end + 1, lines.length)).strip();
        return new Parsed(
                new LearningMetadata(id, title, concept, level, duration, category, docs,
                        related, values.get("next"), values.get("parent"), members,
                        LearningDepth.parse(values.get("depth")), LearningKind.parse(values.get("kind")),
                        values.get("sourceMember")),
                body);
    }

    private static String required(Map<String, String> values, String key, String identifier) {
        return requiredValue(values.get(key), key, identifier);
    }

    private static String requiredValue(String value, String key, String identifier) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing learning metadata " + key + ": " + identifier);
        }
        return value;
    }

    private static String value(String raw) {
        String value = raw.trim();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    record Parsed(LearningMetadata metadata, String body) {
    }
}
