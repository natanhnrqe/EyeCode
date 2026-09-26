package com.eyecode.language.inlay;

import com.eyecode.language.DocumentLanguageResolver;
import com.eyecode.language.LanguageId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InlayHintService {
    private final DocumentLanguageResolver languageResolver;
    private final Map<LanguageId, InlayHintProvider> providers;

    public InlayHintService(DocumentLanguageResolver languageResolver, List<InlayHintProvider> providers) {
        this.languageResolver = languageResolver;
        Map<LanguageId, InlayHintProvider> registered = new LinkedHashMap<>();
        for (InlayHintProvider provider : providers) {
            if (registered.putIfAbsent(provider.languageId(), provider) != null) {
                throw new IllegalArgumentException("Duplicate inlay hint provider: " + provider.languageId());
            }
        }
        this.providers = Map.copyOf(registered);
    }

    public Optional<InlayHintResult> inlayHints(InlayHintRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        return languageResolver.resolve(request.document())
                .map(providers::get)
                .map(provider -> provider.inlayHints(request));
    }
}
