package com.eyecode.language;

import java.util.Optional;

public interface DocumentLanguageResolver {
    Optional<LanguageId> resolve(LanguageDocument document);
}
