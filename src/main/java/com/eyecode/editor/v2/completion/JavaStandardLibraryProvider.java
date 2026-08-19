package com.eyecode.editor.v2.completion;

import com.eyecode.editor.v2.completion.database.CompletionDatabase;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.completion.insert.CompletionPrefixResolver;

import java.util.List;

public final class JavaStandardLibraryProvider implements CompletionProvider {

    private static final List<CompletionItem> LIBRARY_ITEMS = CompletionDatabase.getAll();

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

        List<CompletionItem> items = LIBRARY_ITEMS.stream()
                .filter(item -> item.getLabel().startsWith(prefix))
                .toList();

        return new CompletionSnapshot(items);
    }
}
