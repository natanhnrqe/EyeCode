package com.eyecode.editor.v2.completion;

import java.util.Comparator;
import java.util.List;

public final class CompletionRanking {

    public List<CompletionItem> rank(List<CompletionItem> items, String prefix) {
        String safePrefix = prefix == null ? "" : prefix;
        return items.stream()
                .sorted(Comparator
                        .comparingInt((CompletionItem item) -> -item.getPriority())
                        .thenComparing((CompletionItem item) -> !item.getLabel().equals(safePrefix))
                        .thenComparing(item -> item.getKind() == CompletionItemKind.SNIPPET ? 0 : 1)
                        .thenComparing(item -> item.getKind() == CompletionItemKind.KEYWORD ? 0 : 1)
                        .thenComparingInt(item -> item.getLabel().length())
                        .thenComparing(CompletionItem::getLabel))
                .toList();
    }
}
