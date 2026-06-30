package com.eyecode.editor.v2.completion.knowledge;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionProvider;
import com.eyecode.editor.v2.completion.CompletionSnapshot;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.language.LanguageContextQueries;

import java.util.List;

public final class JavaKnowledgeBaseProvider implements CompletionProvider {

    @Override
    public CompletionSnapshot complete(LanguageContext context) {
        List<CompletionItem> items = JavaKnowledgeBase.getAll();
        return new CompletionSnapshot(items);
    }
}
