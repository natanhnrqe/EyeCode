package com.eyecode.javafx.monaco;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;

import java.util.List;

public record MonacoCompletionItem(
        String label,
        CompletionItemKind kind,
        String detail,
        String documentation,
        String insertText,
        String filterText,
        boolean snippet,
        int replaceStart,
        int replaceEnd,
        int sortKey,
        String signature,
        String returnType,
        String owner,
        String example,
        String category,
        List<Integer> matchIndices
) {
    public MonacoCompletionItem {
        label = label == null ? "" : label;
        kind = kind == null ? CompletionItemKind.VARIABLE : kind;
        detail = detail == null ? "" : detail;
        documentation = documentation == null ? "" : documentation;
        insertText = insertText == null ? label : insertText;
        filterText = filterText == null || filterText.isBlank() ? label : filterText;
        signature = signature == null ? "" : signature;
        returnType = returnType == null ? "" : returnType;
        owner = owner == null ? "" : owner;
        example = example == null ? "" : example;
        category = category == null ? "" : category;
        matchIndices = matchIndices == null ? List.of() : List.copyOf(matchIndices);
    }

    public MonacoCompletionItem(String label, CompletionItemKind kind, String detail,
                                String documentation, String insertText,
                                int replaceStart, int replaceEnd, int sortKey) {
        this(label, kind, detail, documentation, insertText, label, false,
                replaceStart, replaceEnd, sortKey, "", "", "", "", "", List.of());
    }

    public static MonacoCompletionItem from(CompletionItem item, int replaceStart, int replaceEnd) {
        return from(item, replaceStart, replaceEnd, List.of());
    }

    public static MonacoCompletionItem from(CompletionItem item, int replaceStart, int replaceEnd,
                                            List<Integer> matchIndices) {
        return new MonacoCompletionItem(
                item.getLabel(), item.getKind(), item.getDetail(), item.getDocumentation(),
                item.getInsertText(), item.getLabel(),
                item.getKind() == CompletionItemKind.SNIPPET && item.getInsertText().contains("${"),
                replaceStart, replaceEnd, item.getPriority(), item.getSignature(), item.getReturnType(),
                item.getOwner(), item.getExample(), item.getCategory(), matchIndices);
    }
}
