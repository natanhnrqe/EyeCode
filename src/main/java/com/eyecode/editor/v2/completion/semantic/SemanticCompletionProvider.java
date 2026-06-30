package com.eyecode.editor.v2.completion.semantic;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionProvider;
import com.eyecode.editor.v2.completion.CompletionSnapshot;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.language.LanguageContextQueries;

import java.util.List;

public final class SemanticCompletionProvider implements CompletionProvider {

    private final SemanticSymbolRegistry registry;

    public SemanticCompletionProvider(SemanticSymbolRegistry registry) {
        this.registry = registry;
    }

    @Override
    public CompletionSnapshot complete(LanguageContext context) {
        List<CompletionItem> items = registry.getSymbols().stream()
                .map(symbol -> new CompletionItem(
                        symbol.getName(),
                        symbol.getName(),
                        symbol.getDetail(),
                        symbol.getKind()
                ))
                .toList();

        return new CompletionSnapshot(items);
    }
}
