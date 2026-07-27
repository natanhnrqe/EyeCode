package com.eyecode.learning.service;

import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.model.RelatedConcept;
import com.eyecode.learning.model.RelatedConceptNavigator;
import com.eyecode.learning.swing.LearningCardActions;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.Objects;

public final class DocumentationLearningCardActions implements LearningCardActions {

    private final DocumentationOpener documentationOpener;
    private final ExplainMoreHandler explainMoreHandler;
    private final RelatedConceptNavigator relatedNavigator;
    private final LearningConcept concept;
    private final List<RelatedConcept> relatedConcepts;

    public DocumentationLearningCardActions(DocumentationOpener documentationOpener,
                                             ExplainMoreHandler explainMoreHandler,
                                             RelatedConceptNavigator relatedNavigator,
                                             LearningConcept concept,
                                             List<RelatedConcept> relatedConcepts) {
        this.documentationOpener = Objects.requireNonNull(documentationOpener,
                "documentationOpener must not be null");
        this.explainMoreHandler = Objects.requireNonNull(explainMoreHandler,
                "explainMoreHandler must not be null");
        this.relatedNavigator = Objects.requireNonNull(relatedNavigator,
                "relatedNavigator must not be null");
        this.concept = concept;
        this.relatedConcepts = relatedConcepts != null ? List.copyOf(relatedConcepts) : List.of();
    }

    @Override
    public void openDocumentation() {
        if (concept != null) {
            documentationOpener.open(concept);
        }
    }

    @Override
    public void explainMore() {
        if (concept != null) {
            explainMoreHandler.explain(concept);
        }
    }

    @Override
    public void copyCode(String code) {
        if (code == null || code.isEmpty()) {
            return;
        }
        StringSelection selection = new StringSelection(code);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }

    @Override
    public void showRelatedConcepts(List<RelatedConcept> concepts) {
        if (concepts == null || concepts.isEmpty()) {
            return;
        }
        navigatorLoop:
        for (RelatedConcept rc : concepts) {
            if (rc == null) continue;
            if (relatedNavigator.navigateTo(rc)) {
                break navigatorLoop;
            }
        }
    }

    public List<RelatedConcept> relatedConcepts() {
        return relatedConcepts;
    }

    public boolean hasRelatedConcepts() {
        return !relatedConcepts.isEmpty();
    }

    public boolean hasConcept() {
        return concept != null;
    }

    public LearningConcept concept() {
        return concept;
    }
}
