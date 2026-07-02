package com.eyecode.editor.v2.completion.database;

import com.eyecode.editor.v2.completion.CompletionItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CompletionDatabase {

    private static final Map<String, CompletionItem> ENTRIES = new LinkedHashMap<>();

    static {
        registerAll(JavaLangSymbols.getAll());
        registerAll(JavaUtilSymbols.getAll());
        registerAll(JavaIoSymbols.getAll());
        registerAll(JavaTimeSymbols.getAll());
        registerAll(JavaMathSymbols.getAll());
        registerAll(JavaNioSymbols.getAll());
        registerAll(JavaNetSymbols.getAll());
        registerAll(JavaStreamSymbols.getAll());
    }

    private CompletionDatabase() {}

    private static void registerAll(List<CompletionItem> items) {
        if (items == null) return;
        for (CompletionItem item : items) {
            if (item != null && item.getLabel() != null) {
                ENTRIES.putIfAbsent(item.getLabel(), item);
            }
        }
    }

    public static List<CompletionItem> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(ENTRIES.values()));
    }

    public static CompletionItem get(String label) {
        return ENTRIES.get(label);
    }

    public static List<CompletionItem> findByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return Collections.emptyList();
        }
        List<CompletionItem> result = new ArrayList<>();
        for (CompletionItem item : ENTRIES.values()) {
            if (item.getLabel().startsWith(prefix)) {
                result.add(item);
            }
        }
        return result;
    }

    public static int size() {
        return ENTRIES.size();
    }

    public static void contribute(CompletionItem item) {
        if (item != null && item.getLabel() != null) {
            ENTRIES.putIfAbsent(item.getLabel(), item);
        }
    }

    public static void contributeAll(List<CompletionItem> items) {
        if (items != null) {
            for (CompletionItem item : items) {
                contribute(item);
            }
        }
    }
}
