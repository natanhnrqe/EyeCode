package com.eyecode.language.hover;

import com.eyecode.language.LanguageFeatureRequest;
import com.eyecode.language.LanguageId;

import java.util.Optional;

public interface HoverProvider {
    LanguageId languageId();

    Optional<HoverResult> hover(LanguageFeatureRequest request);
}
