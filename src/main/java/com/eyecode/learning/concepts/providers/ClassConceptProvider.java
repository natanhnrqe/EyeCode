package com.eyecode.learning.concepts.providers;

import com.eyecode.editor.v2.language.java.symbols.ProjectSymbol;
import com.eyecode.editor.v2.language.java.symbols.SymbolKind;
import com.eyecode.learning.analysis.LearningAnalysisContext;
import com.eyecode.learning.catalog.LearningCatalog;
import com.eyecode.learning.concepts.LearningConceptProvider;
import com.eyecode.learning.model.ConceptType;
import com.eyecode.learning.model.LearningConcept;

import java.util.Collections;
import java.util.List;

public final class ClassConceptProvider implements LearningConceptProvider {

    private final LearningCatalog catalog;

    public ClassConceptProvider(LearningCatalog catalog) {
        this.catalog = catalog;
    }

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

        LearningConcept concept = catalog.get(conceptType);
        if (concept == null) {
            return Collections.emptyList();
        }

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
}
