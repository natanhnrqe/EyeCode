package com.eyecode.learning.model;

import java.util.ArrayList;
import java.util.List;

public final class LearningCardDocumentAdapter {

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

        document.setHeader(new LearningCardHeaderData("java", concept.getTitle() != null ? concept.getTitle() : "", subtitle));
        document.addHeading(concept.getTitle() != null ? concept.getTitle() : "");
        document.addParagraph(concept.getDescription() != null ? concept.getDescription() : "");
        document.addCodeBlock("Java",
                "public class " + (concept.getTitle() != null ? concept.getTitle() : "Example") + " {\n" +
                "    // " + (concept.getDescription() != null ? concept.getDescription().replace("\n", " ") : "No description") + "\n" +
                "}\n"
        );

        if (concept.getRelatedConcepts() != null && !concept.getRelatedConcepts().isEmpty()) {
            document.addHeading("Related concepts");
            for (String related : concept.getRelatedConcepts()) {
                document.addBullet(related);
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
            result.add(RelatedConcept.fromTitle(related));
        }
        return List.copyOf(result);
    }
}
