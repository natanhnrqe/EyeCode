package com.eyecode.language.signature;

import com.eyecode.language.DocumentLanguageResolver;
import com.eyecode.language.LanguageFeatureRequest;
import com.eyecode.language.LanguageId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SignatureHelpService {
    private final DocumentLanguageResolver languageResolver;
    private final Map<LanguageId, SignatureHelpProvider> providers;

    public SignatureHelpService(DocumentLanguageResolver languageResolver, List<SignatureHelpProvider> providers) {
        this.languageResolver = languageResolver;
        Map<LanguageId, SignatureHelpProvider> registered = new LinkedHashMap<>();
        for (SignatureHelpProvider provider : providers) {
            if (registered.putIfAbsent(provider.languageId(), provider) != null) {
                throw new IllegalArgumentException("Duplicate signature help provider: " + provider.languageId());
            }
        }
        this.providers = Map.copyOf(registered);
    }

    public Optional<SignatureHelpResult> signatureHelp(LanguageFeatureRequest request) {
        if (request == null) throw new IllegalArgumentException("request must not be null");
        return languageResolver.resolve(request.document()).map(providers::get)
                .flatMap(provider -> provider == null ? Optional.empty() : provider.signatureHelp(request));
    }
}
