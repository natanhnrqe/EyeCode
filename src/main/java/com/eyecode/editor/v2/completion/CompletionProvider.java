package com.eyecode.editor.v2.completion;

import com.eyecode.editor.v2.language.LanguageContext;

public interface CompletionProvider {

    default boolean supports(CompletionContextKind contextKind) {
        return true;
    }

    CompletionSnapshot complete(LanguageContext context);

    default CompletionSnapshot complete(LanguageContext context, boolean manual) {
        return complete(context);
    }
}
