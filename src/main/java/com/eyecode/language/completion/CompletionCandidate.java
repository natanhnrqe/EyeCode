package com.eyecode.language.completion;

import java.util.List;

public record CompletionCandidate(String label, String kind, String detail, String documentation,
                                  String insertText, String filterText, boolean snippet,
                                  int replaceStart, int replaceEnd, int sortKey,
                                  String signature, String returnType, String owner,
                                  String example, String category, List<Integer> matchIndices,
                                  String resolveId) {
    public CompletionCandidate(String label, String kind, String detail, String documentation,
                               String insertText, String filterText, boolean snippet,
                               int replaceStart, int replaceEnd, int sortKey,
                               String signature, String returnType, String owner,
                               String example, String category, List<Integer> matchIndices) {
        this(label, kind, detail, documentation, insertText, filterText, snippet,
                replaceStart, replaceEnd, sortKey, signature, returnType, owner,
                example, category, matchIndices, "");
    }

    public CompletionCandidate {
        label = label == null ? "" : label;
        kind = kind == null ? "VARIABLE" : kind;
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
        resolveId = resolveId == null ? "" : resolveId;
    }
}
