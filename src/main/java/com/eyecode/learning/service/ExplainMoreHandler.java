package com.eyecode.learning.service;

import com.eyecode.learning.model.LearningConcept;

public interface ExplainMoreHandler {

    void explain(LearningConcept concept);

    static ExplainMoreHandler delegatingTo(DocumentationOpener opener) {
        if (opener == null) {
            return NoopExplainMoreHandler.INSTANCE;
        }
        return new DocumentationExplainMoreHandler(opener);
    }

    final class NoopExplainMoreHandler implements ExplainMoreHandler {
        static final NoopExplainMoreHandler INSTANCE = new NoopExplainMoreHandler();

        @Override
        public void explain(LearningConcept concept) {
        }
    }

    final class DocumentationExplainMoreHandler implements ExplainMoreHandler {
        private final DocumentationOpener opener;

        DocumentationExplainMoreHandler(DocumentationOpener opener) {
            this.opener = opener;
        }

        @Override
        public void explain(LearningConcept concept) {
            if (concept != null) {
                opener.open(concept);
            }
        }
    }
}
