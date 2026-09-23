package com.eyecode.language.signature;

import com.eyecode.language.LanguageFeatureRequest;
import com.eyecode.language.LanguageId;

import java.util.Optional;

public interface SignatureHelpProvider {
    LanguageId languageId();

    Optional<SignatureHelpResult> signatureHelp(LanguageFeatureRequest request);
}
