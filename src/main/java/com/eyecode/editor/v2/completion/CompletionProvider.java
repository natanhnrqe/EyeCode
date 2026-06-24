package com.eyecode.editor.v2.completion;

import com.eyecode.editor.v2.language.LanguageContext;

public interface CompletionProvider {

    CompletionSnapshot complete(LanguageContext context);
}
