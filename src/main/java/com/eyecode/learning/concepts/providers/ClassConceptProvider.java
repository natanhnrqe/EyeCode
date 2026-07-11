package com.eyecode.learning.concepts.providers;

import com.eyecode.editor.v2.language.java.symbols.ProjectSymbol;
import com.eyecode.editor.v2.language.java.symbols.SymbolKind;
import com.eyecode.learning.analysis.LearningAnalysisContext;
import com.eyecode.learning.concepts.LearningConceptProvider;
import com.eyecode.learning.model.ConceptType;
import com.eyecode.learning.model.DifficultyLevel;
import com.eyecode.learning.model.LearningConcept;

import java.util.Collections;
import java.util.List;

public final class ClassConceptProvider implements LearningConceptProvider {

    @Override
    public List<LearningConcept> analyze(LearningAnalysisContext context) {
        if (context == null) {
            return Collections.emptyList();
        }

        ProjectSymbol symbol = context.getCurrentSymbol();
        if (symbol == null || symbol.getKind() == null) {
            return Collections.emptyList();
        }

        SymbolKind kind = symbol.getKind();
        ConceptType conceptType = toConceptType(kind);
        if (conceptType == null) {
            return Collections.emptyList();
        }

        LearningConcept concept = new LearningConcept();
        concept.setId(kind.name().toLowerCase());
        concept.setTitle(conceptType.name());
        concept.setDescription(descriptionFor(conceptType));
        concept.setType(conceptType);
        concept.setDifficulty(difficultyFor(conceptType));

        return List.of(concept);
    }

    private static ConceptType toConceptType(SymbolKind kind) {
        return switch (kind) {
            case CLASS -> ConceptType.CLASS;
            case INTERFACE -> ConceptType.INTERFACE;
            case ENUM -> ConceptType.ENUM;
            case RECORD -> ConceptType.RECORD;
            default -> null;
        };
    }

    private static DifficultyLevel difficultyFor(ConceptType type) {
        return switch (type) {
            case CLASS, ENUM -> DifficultyLevel.BEGINNER;
            case INTERFACE, RECORD -> DifficultyLevel.INTERMEDIATE;
            default -> DifficultyLevel.BEGINNER;
        };
    }

    private static String descriptionFor(ConceptType type) {
        return switch (type) {
            case CLASS -> "A Java class defines a blueprint for creating objects.";
            case INTERFACE -> "A Java interface defines a contract that implementing classes must fulfill.";
            case ENUM -> "A Java enum defines a fixed set of named constants.";
            case RECORD -> "A Java record is a concise way to define immutable data carriers.";
            default -> "";
        };
    }
}
