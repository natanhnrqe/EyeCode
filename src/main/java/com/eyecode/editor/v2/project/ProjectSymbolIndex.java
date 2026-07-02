package com.eyecode.editor.v2.project;

import com.eyecode.editor.v2.completion.CompletionItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProjectSymbolIndex {

    private final Map<String, CompletionItem> symbols = new LinkedHashMap<>();

    public void clear() {
        symbols.clear();
    }

    public void add(CompletionItem item) {
        if (item != null && item.getLabel() != null) {
            symbols.putIfAbsent(item.getLabel(), item);
        }
    }

    public List<CompletionItem> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(symbols.values()));
    }

    public int size() {
        return symbols.size();
    }
}
