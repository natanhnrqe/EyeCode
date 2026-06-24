package com.eyecode.editor.v2.completion;

import java.util.List;

public final class CompletionSnapshot {

    private static final CompletionSnapshot EMPTY = new CompletionSnapshot(List.of());

    private final List<CompletionItem> items;

    public CompletionSnapshot(List<CompletionItem> items) {
        this.items = List.copyOf(items);
    }

    public static CompletionSnapshot empty() {
        return EMPTY;
    }

    public List<CompletionItem> getItems() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }
}
