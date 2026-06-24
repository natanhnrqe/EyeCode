package com.eyecode.editor.v2.completion;

import java.util.Comparator;
import java.util.List;

public final class CompletionRanking {

    public List<CompletionItem> rank(List<CompletionItem> items, String prefix) {
        String safePrefix = prefix == null ? "" : prefix;
        return items.stream()
                .sorted(Comparator
                        .comparing((CompletionItem item) -> !item.getLabel().equals(safePrefix))
                        .thenComparingInt(item -> item.getLabel().length())
                        .thenComparing(CompletionItem::getLabel))
                .toList();
    }
}
