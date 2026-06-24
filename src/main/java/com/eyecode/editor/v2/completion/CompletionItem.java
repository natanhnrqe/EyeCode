package com.eyecode.editor.v2.completion;

import java.util.Objects;

public final class CompletionItem {

    private final String label;
    private final String insertText;
    private final String detail;
    private final CompletionItemKind kind;

    public CompletionItem(String label, String insertText, String detail, CompletionItemKind kind) {
        this.label = label;
        this.insertText = insertText;
        this.detail = detail;
        this.kind = kind;
    }

    public String getLabel() { return label; }

    public String getInsertText() { return insertText; }

    public String getDetail() { return detail; }

    public CompletionItemKind getKind() { return kind; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompletionItem that)) return false;
        return Objects.equals(label, that.label)
                && Objects.equals(insertText, that.insertText)
                && Objects.equals(detail, that.detail)
                && kind == that.kind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, insertText, detail, kind);
    }

    @Override
    public String toString() {
        return "CompletionItem{"
                + "label='" + label + '\''
                + ", insertText='" + insertText + '\''
                + ", detail='" + detail + '\''
                + ", kind=" + kind
                + '}';
    }
}
