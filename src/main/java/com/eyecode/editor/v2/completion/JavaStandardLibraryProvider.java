package com.eyecode.editor.v2.completion;

import com.eyecode.editor.v2.completion.database.CompletionDatabase;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.completion.insert.CompletionPrefixResolver;
import java.util.List;

public final class JavaStandardLibraryProvider implements CompletionProvider {

    private static final List<CompletionItem> LIBRARY_ITEMS = CompletionDatabase.getAll();
    @Override
    public boolean supports(CompletionContextKind contextKind) {
        return contextKind != CompletionContextKind.MEMBER_ACCESS;
    }

    @Override
    public CompletionSnapshot complete(LanguageContext context) {
        return complete(context, false);
    }

    @Override
    public CompletionSnapshot complete(LanguageContext context, boolean manual) {
        String prefix = CompletionPrefixResolver.resolvePrefix(context);
        if (prefix.isEmpty()) {
            return CompletionSnapshot.empty();
        }

        List<CompletionItem> items = LIBRARY_ITEMS.stream()
                .filter(item -> item.getLabel().startsWith(prefix))
                .toList();

        return new CompletionSnapshot(items);
    }
}
