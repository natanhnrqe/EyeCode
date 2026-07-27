package com.eyecode.learning.catalog;

import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.model.RelatedConceptResolver;

import java.util.Objects;
import java.util.Optional;

public final class CatalogRelatedConceptResolver implements RelatedConceptResolver {

    private final LearningCatalog catalog;

    public CatalogRelatedConceptResolver(LearningCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
    }

    @Override
    public Optional<LearningConcept> resolve(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return catalog.allConcepts().stream()
                .filter(c -> c != null
                        && c.getId() != null
                        && id.equalsIgnoreCase(c.getId()))
                .findFirst();
    }
}
