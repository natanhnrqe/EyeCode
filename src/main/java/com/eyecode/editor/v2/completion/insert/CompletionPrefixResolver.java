package com.eyecode.editor.v2.completion.insert;

import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.language.LanguageContextQueries;

public final class CompletionPrefixResolver {

    public String resolve(LanguageContext context) {
        return LanguageContextQueries.getCurrentWordPrefix(context);
    }
}
