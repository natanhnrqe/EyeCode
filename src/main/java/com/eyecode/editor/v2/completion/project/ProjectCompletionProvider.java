package com.eyecode.editor.v2.completion.project;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;
import com.eyecode.editor.v2.completion.CompletionProvider;
import com.eyecode.editor.v2.completion.CompletionSnapshot;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.language.LanguageContextQueries;
import com.eyecode.editor.v2.project.ProjectSymbolIndex;

import java.util.EnumSet;
import java.util.List;

public final class ProjectCompletionProvider implements CompletionProvider {

    private static final EnumSet<CompletionItemKind> TYPE_KINDS = EnumSet.of(
            CompletionItemKind.CLASS,
            CompletionItemKind.INTERFACE,
            CompletionItemKind.ENUM,
            CompletionItemKind.RECORD
    );

    private final ProjectSymbolIndex index;

    public ProjectCompletionProvider(ProjectSymbolIndex index) {
        this.index = index;
    }

    @Override
    public CompletionSnapshot complete(LanguageContext context) {
        String prefix = LanguageContextQueries.getCurrentWordPrefix(context);
        if (prefix.isEmpty()) {
            return CompletionSnapshot.empty();
        }

        List<CompletionItem> items = index.getAll().stream()
                .filter(item -> TYPE_KINDS.contains(item.getKind()))
                .filter(item -> item.getLabel().toLowerCase().startsWith(prefix.toLowerCase()))
                .toList();

        return new CompletionSnapshot(items);
    }
}
