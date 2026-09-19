package com.eyecode.language.completion;

import com.eyecode.language.DocumentLanguageResolver;
import com.eyecode.language.LanguageId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CompletionService {
    private final DocumentLanguageResolver languageResolver;
    private final Map<LanguageId, CompletionProvider> providers;

    public CompletionService(DocumentLanguageResolver languageResolver, List<CompletionProvider> providers) {
        this.languageResolver = languageResolver;
        Map<LanguageId, CompletionProvider> registered = new LinkedHashMap<>();
        for (CompletionProvider provider : providers) {
            if (registered.putIfAbsent(provider.languageId(), provider) != null) {
                throw new IllegalArgumentException("Duplicate completion provider: " + provider.languageId());
            }
        }
        this.providers = Map.copyOf(registered);
    }

    public CompletionResult complete(CompletionRequest request) {
        if (request == null) throw new IllegalArgumentException("request must not be null");
        return languageResolver.resolve(request.document()).map(providers::get)
                .map(provider -> provider.complete(request)).orElseGet(CompletionResult::empty);
    }
}
