package com.eyecode.learning.model;

import com.eyecode.learning.service.MarkdownCodeExampleExtractor;

import java.util.ArrayList;
import java.util.List;

public final class LearningCardDocumentAdapter {

    private static final com.eyecode.learning.service.CodeExampleExtractor extractor =
            new MarkdownCodeExampleExtractor();

    private LearningCardDocumentAdapter() {}

    public static LearningCardDocument fromConcept(LearningConcept concept) {
        LearningCardDocument document = new LearningCardDocument();
        if (concept == null) {
            return document;
        }

        String subtitle = "";
        if (concept.getType() != null) {
            subtitle = concept.getType().name();
            if (concept.getDifficulty() != null) {
                subtitle += " • " + concept.getDifficulty().name();
            }
        }

        document.setHeader(new LearningCardHeaderData("java",
                concept.getTitle() != null ? concept.getTitle() : "",
                subtitle));

        String title = concept.getTitle() != null ? concept.getTitle() : "";
        String description = concept.getDescription() != null ? concept.getDescription() : "";

        if (!title.isBlank()) {
            document.addHeading(title);
        }
        if (!description.isBlank()) {
            document.addParagraph(description);
        }

        if (concept.getType() != null) {
            java.util.Optional<String> realCode = extractor.extractFirstExample(concept);
            if (realCode.isPresent() && !realCode.get().isBlank()) {
                document.addCodeBlock("Java", realCode.get());
            } else {
                document.addCodeBlock("Java",
                        "public class " + (title.isBlank() ? "Example" : title) + " {\n" +
                        (description.isBlank() ? "    // no description\n" : "    // " + description.replace("\n", " ") + "\n") +
                        "}\n"
                );
            }
        }

        List<RelatedConcept> related = relatedConceptsFrom(concept);
        document.setRelatedConcepts(related);
        if (!related.isEmpty()) {
            document.addHeading("Related concepts");
            for (RelatedConcept rc : related) {
                document.addBullet(rc.title());
            }
        }

        document.setFooter(new LearningCardFooterData("Updated:", "Today"));
        return document;
    }

    public static List<RelatedConcept> relatedConceptsFrom(LearningConcept concept) {
        if (concept == null || concept.getRelatedConcepts() == null || concept.getRelatedConcepts().isEmpty()) {
            return List.of();
        }
        List<RelatedConcept> result = new ArrayList<>(concept.getRelatedConcepts().size());
        for (String related : concept.getRelatedConcepts()) {
            if (related == null || related.isBlank()) {
                continue;
            }
            result.add(RelatedConcept.fromTitle(related));
        }
        return List.copyOf(result);
    }
}
