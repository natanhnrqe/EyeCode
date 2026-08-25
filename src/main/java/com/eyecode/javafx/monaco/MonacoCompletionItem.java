package com.eyecode.javafx.monaco;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;

public record MonacoCompletionItem(
        String label,
        CompletionItemKind kind,
        String detail,
        String documentation,
        String insertText,
        int replaceStart,
        int replaceEnd,
        int sortKey
) {
    public static MonacoCompletionItem from(CompletionItem item, int replaceStart, int replaceEnd) {
        return new MonacoCompletionItem(
                item.getLabel(), item.getKind(), item.getDetail(), item.getDocumentation(),
                item.getInsertText(), replaceStart, replaceEnd, item.getPriority());
    }
}
