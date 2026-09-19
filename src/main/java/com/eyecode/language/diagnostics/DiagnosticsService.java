package com.eyecode.language.diagnostics;

import com.eyecode.language.DocumentLanguageResolver;
import com.eyecode.language.LanguageId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DiagnosticsService {
    private final DocumentLanguageResolver languageResolver;
    private final Map<LanguageId, DiagnosticsProvider> providers;

    public DiagnosticsService(DocumentLanguageResolver languageResolver, List<DiagnosticsProvider> providers) {
        this.languageResolver = languageResolver;
        Map<LanguageId, DiagnosticsProvider> registered = new LinkedHashMap<>();
        for (DiagnosticsProvider provider : providers) {
            if (registered.putIfAbsent(provider.languageId(), provider) != null) {
                throw new IllegalArgumentException("Duplicate diagnostics provider: " + provider.languageId());
            }
        }
        this.providers = Map.copyOf(registered);
    }

    public DiagnosticsResult analyze(DiagnosticsRequest request) {
        if (request == null) throw new IllegalArgumentException("request must not be null");
        return languageResolver.resolve(request.document()).map(providers::get)
                .map(provider -> provider.analyze(request)).orElseGet(DiagnosticsResult::empty);
    }
}
