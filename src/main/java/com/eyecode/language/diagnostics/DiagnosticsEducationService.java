package com.eyecode.language.diagnostics;

import com.eyecode.language.DocumentLanguageResolver;
import com.eyecode.language.LanguageDocument;
import com.eyecode.language.LanguageId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DiagnosticsEducationService {
    private final DocumentLanguageResolver languageResolver;
    private final Map<LanguageId, DiagnosticExplainer> explainers;

    public DiagnosticsEducationService(DocumentLanguageResolver languageResolver, List<DiagnosticExplainer> explainers) {
        this.languageResolver = languageResolver;
        Map<LanguageId, DiagnosticExplainer> registered = new LinkedHashMap<>();
        for (DiagnosticExplainer explainer : explainers) {
            if (registered.putIfAbsent(explainer.languageId(), explainer) != null) {
                throw new IllegalArgumentException("Duplicate diagnostic explainer: " + explainer.languageId());
            }
        }
        this.explainers = Map.copyOf(registered);
    }

    public Optional<DiagnosticEducation> explain(LanguageDocument document, Diagnostic diagnostic) {
        if (document == null || diagnostic == null) return Optional.empty();
        return languageResolver.resolve(document).map(explainers::get)
                .filter(explainer -> explainer != null)
                .flatMap(explainer -> explainer.explain(diagnostic));
    }
}
