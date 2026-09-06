package com.eyecode.editor.v2.completion.knowledge;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionProvider;
import com.eyecode.editor.v2.completion.CompletionSnapshot;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.completion.insert.CompletionPrefixResolver;

import java.util.List;

public final class JavaKnowledgeBaseProvider implements CompletionProvider {

    @Override
    public CompletionSnapshot complete(LanguageContext context) {
        return complete(context, false);
    }

    @Override
    public CompletionSnapshot complete(LanguageContext context, boolean manual) {
        if (CompletionPrefixResolver.isQualifiedContext(context)) {
            return CompletionSnapshot.empty();
        }
        String prefix = CompletionPrefixResolver.resolvePrefix(context);
        if (prefix.isEmpty() && !manual) {
            return CompletionSnapshot.empty();
        }

        return new CompletionSnapshot(JavaKnowledgeBase.getAll());
    }
}
