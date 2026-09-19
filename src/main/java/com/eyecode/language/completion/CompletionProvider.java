package com.eyecode.language.completion;

import com.eyecode.language.LanguageId;

public interface CompletionProvider {
    LanguageId languageId();

    CompletionResult complete(CompletionRequest request);
}
