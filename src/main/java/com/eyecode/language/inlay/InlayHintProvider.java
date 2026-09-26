package com.eyecode.language.inlay;

import com.eyecode.language.LanguageId;

public interface InlayHintProvider {
    LanguageId languageId();

    InlayHintResult inlayHints(InlayHintRequest request);
}
