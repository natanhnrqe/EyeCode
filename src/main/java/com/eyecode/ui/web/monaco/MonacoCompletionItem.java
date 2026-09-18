package com.eyecode.ui.web.monaco;

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
        List<Integer> matchIndices,
        List<CompletionDetailSection> detailSections,
        String exampleLabel
) {

    public record CompletionDetailSection(
            String title,
            List<CompletionDetailEntry> entries
    ) {
        public CompletionDetailSection {
            title = title == null ? "" : title;
            entries = entries == null ? List.of() : List.copyOf(entries);
        }
    }

    public record CompletionDetailEntry(
            String name,
            String type,
            String description
    ) {
        public CompletionDetailEntry {
            name = name == null ? "" : name;
            type = type == null ? "" : type;
            description = description == null ? "" : description;
        }
    }

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
        detailSections = detailSections == null ? List.of() : List.copyOf(detailSections);
        exampleLabel = exampleLabel == null ? "" : exampleLabel;
    }

    public MonacoCompletionItem(
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
        this(
                label,
                kind,
                detail,
                documentation,
                insertText,
                filterText,
                snippet,
                replaceStart,
                replaceEnd,
                sortKey,
                signature,
                returnType,
                owner,
                example,
                category,
                matchIndices,
                List.of(),
                ""
        );
    }

    public MonacoCompletionItem(
            String label,
            CompletionItemKind kind,
            String detail,
            String documentation,
            String insertText,
            int replaceStart,
            int replaceEnd,
            int sortKey
    ) {
        this(
                label,
                kind,
                detail,
                documentation,
                insertText,
                label,
                false,
                replaceStart,
                replaceEnd,
                sortKey,
                "",
                "",
                "",
                "",
                "",
                List.of(),
                List.of(),
                ""

        );
    }

    public static MonacoCompletionItem from(
            CompletionItem item,
            int replaceStart,
            int replaceEnd
    ) {
        return from(item, replaceStart, replaceEnd, List.of());

    }

    public static MonacoCompletionItem from(
            CompletionItem item,
            int replaceStart,
            int replaceEnd,
            List<Integer> matchIndices
    ) {
        return new MonacoCompletionItem(
                item.getLabel(),
                item.getKind(),
                item.getDetail(),
                item.getDocumentation(),
                item.getInsertText(),
                item.getLabel(),
                item.getKind() == CompletionItemKind.SNIPPET
                        && item.getInsertText().contains("${"),
                replaceStart,
                replaceEnd,
                item.getPriority(),
                item.getSignature(),
                item.getReturnType(),
                item.getOwner(),
                item.getExample(),
                item.getCategory(),
                matchIndices,
                List.of(),
                ""
        );
    }
}