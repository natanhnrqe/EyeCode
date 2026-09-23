package com.eyecode.language.hover;

import com.eyecode.language.DocumentLanguageResolver;
import com.eyecode.language.LanguageFeatureRequest;
import com.eyecode.language.LanguageId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class HoverService {
    private final DocumentLanguageResolver languageResolver;
    private final Map<LanguageId, HoverProvider> providers;

    public HoverService(DocumentLanguageResolver languageResolver, List<HoverProvider> providers) {
        this.languageResolver = languageResolver;
        Map<LanguageId, HoverProvider> registered = new LinkedHashMap<>();
        for (HoverProvider provider : providers) {
            if (registered.putIfAbsent(provider.languageId(), provider) != null) {
                throw new IllegalArgumentException("Duplicate hover provider: " + provider.languageId());
            }
        }
        this.providers = Map.copyOf(registered);
    }

    public Optional<HoverResult> hover(LanguageFeatureRequest request) {
        if (request == null) throw new IllegalArgumentException("request must not be null");
        return languageResolver.resolve(request.document()).map(providers::get)
                .flatMap(provider -> provider == null ? Optional.empty() : provider.hover(request));
    }
}
