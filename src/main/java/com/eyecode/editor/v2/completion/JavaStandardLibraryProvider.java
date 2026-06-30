package com.eyecode.editor.v2.completion;

import com.eyecode.editor.v2.completion.database.CompletionDatabase;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.language.LanguageContextQueries;

import java.util.List;

public final class JavaStandardLibraryProvider implements CompletionProvider {

    private static final List<CompletionItem> LIBRARY_ITEMS = CompletionDatabase.getAll();

    @Override
    public CompletionSnapshot complete(LanguageContext context) {
        String prefix = LanguageContextQueries.getCurrentWordPrefix(context);
        if (prefix.isEmpty()) {
            return CompletionSnapshot.empty();
        }

        List<CompletionItem> items = LIBRARY_ITEMS.stream()
                .filter(item -> item.getLabel().startsWith(prefix))
                .toList();

        return new CompletionSnapshot(items);
    }
}
