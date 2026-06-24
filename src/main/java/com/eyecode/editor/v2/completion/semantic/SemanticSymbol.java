package com.eyecode.editor.v2.completion.semantic;

import com.eyecode.editor.v2.completion.CompletionItemKind;

import java.util.Objects;

public final class SemanticSymbol {

    private final String name;
    private final CompletionItemKind kind;
    private final String detail;

    public SemanticSymbol(String name, CompletionItemKind kind, String detail) {
        this.name = Objects.requireNonNull(name);
        this.kind = Objects.requireNonNull(kind);
        this.detail = Objects.requireNonNull(detail);
    }

    public String getName() { return name; }

    public CompletionItemKind getKind() { return kind; }

    public String getDetail() { return detail; }
}
