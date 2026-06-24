package com.eyecode.editor.v2.completion;

import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.language.LanguageContextQueries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CompletionEngine {

    private final List<CompletionProvider> providers;
    private final CompletionRanking ranking;

    public CompletionEngine(List<CompletionProvider> providers) {
        this.providers = List.copyOf(providers);
        this.ranking = new CompletionRanking();
    }

    public CompletionSnapshot complete(LanguageContext context) {
        Map<String, CompletionItem> merged = new LinkedHashMap<>();
        for (CompletionProvider provider : providers) {
            CompletionSnapshot snapshot = provider.complete(context);
            for (CompletionItem item : snapshot.getItems()) {
                merged.putIfAbsent(item.getLabel() + "\u0000" + item.getKind(), item);
            }
        }
        String prefix = LanguageContextQueries.getCurrentWordPrefix(context);
        List<CompletionItem> ranked = ranking.rank(new ArrayList<>(merged.values()), prefix);
        return new CompletionSnapshot(ranked);
    }
}
