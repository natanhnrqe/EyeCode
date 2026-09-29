package com.eyecode.documentation.content;

public record DocumentationCatalogEntry(
        String id,
        String title,
        String type,
        String summary,
        String level,
        Integer duration,
        String branch,
        String subgroup) {

    public static DocumentationCatalogEntry from(DocumentationFrontMatter metadata) {
        return new DocumentationCatalogEntry(metadata.id(), metadata.title(),
                metadata.type() == null ? DocumentationFrontMatter.TYPE_CONCEPT : metadata.type(),
                metadata.summary(), metadata.level(), metadata.duration(),
                branchOf(metadata.id()), subgroupOf(metadata.id()));
    }

    public static String branchOf(String id) {
        String[] segments = id.split("/");
        return segments.length >= 2 ? segments[1] : "";
    }

    public static String subgroupOf(String id) {
        String[] segments = id.split("/");
        return segments.length >= 4 ? segments[2] : null;
    }
}
