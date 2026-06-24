package com.eyecode.editor.v2.completion;

import com.eyecode.editor.v2.language.LanguageContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CompletionEngine {

    private final List<CompletionProvider> providers;

    public CompletionEngine(List<CompletionProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    public CompletionSnapshot complete(LanguageContext context) {
        Map<String, CompletionItem> merged = new LinkedHashMap<>();
        for (CompletionProvider provider : providers) {
            CompletionSnapshot snapshot = provider.complete(context);
            for (CompletionItem item : snapshot.getItems()) {
                merged.putIfAbsent(item.getLabel() + "\u0000" + item.getKind(), item);
            }
        }
        return new CompletionSnapshot(new ArrayList<>(merged.values()));
    }
}
