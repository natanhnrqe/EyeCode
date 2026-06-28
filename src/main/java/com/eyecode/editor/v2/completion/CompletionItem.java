package com.eyecode.editor.v2.completion;

import java.util.Objects;

public final class CompletionItem {

    private final String label;
    private final String insertText;
    private final String detail;
    private final CompletionItemKind kind;
    private final String signature;
    private final String returnType;
    private final String owner;
    private final String documentation;
    private final String example;
    private final String category;
    private final int priority;

    public CompletionItem(String label, String insertText, String detail, CompletionItemKind kind) {
        this(label, insertText, detail, kind, null, null, null, null, null, null, 0);
    }

    public CompletionItem(String label, String insertText, String detail, CompletionItemKind kind,
                          String signature, String returnType, String owner,
                          String documentation, String example, String category, int priority) {
        this.label = label;
        this.insertText = insertText;
        this.detail = detail == null ? "" : detail;
        this.kind = kind;
        this.signature = signature;
        this.returnType = returnType;
        this.owner = owner;
        this.documentation = documentation;
        this.example = example;
        this.category = category;
        this.priority = priority;
    }

    public String getLabel() { return label; }

    public String getInsertText() { return insertText; }

    public String getDetail() { return detail; }

    public CompletionItemKind getKind() { return kind; }

    public String getSignature() { return signature; }

    public String getReturnType() { return returnType; }

    public String getOwner() { return owner; }

    public String getDocumentation() { return documentation; }

    public String getExample() { return example; }

    public String getCategory() { return category; }

    public int getPriority() { return priority; }

    public boolean hasMetadata() {
        return signature != null || returnType != null || owner != null
                || documentation != null || example != null;
    }

    public static Builder builder(String label, String insertText, CompletionItemKind kind) {
        return new Builder(label, insertText, kind);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompletionItem that)) return false;
        return priority == that.priority
                && Objects.equals(label, that.label)
                && Objects.equals(insertText, that.insertText)
                && Objects.equals(detail, that.detail)
                && kind == that.kind
                && Objects.equals(signature, that.signature)
                && Objects.equals(returnType, that.returnType)
                && Objects.equals(owner, that.owner)
                && Objects.equals(documentation, that.documentation)
                && Objects.equals(example, that.example)
                && Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, insertText, detail, kind, signature, returnType,
                owner, documentation, example, category, priority);
    }

    @Override
    public String toString() {
        return "CompletionItem{"
                + "label='" + label + '\''
                + ", kind=" + kind
                + ", returnType='" + returnType + '\''
                + ", owner='" + owner + '\''
                + '}';
    }

    public static final class Builder {
        private final String label;
        private final String insertText;
        private final CompletionItemKind kind;
        private String detail = "";
        private String signature;
        private String returnType;
        private String owner;
        private String documentation;
        private String example;
        private String category;
        private int priority;

        private Builder(String label, String insertText, CompletionItemKind kind) {
            this.label = label;
            this.insertText = insertText;
            this.kind = kind;
        }

        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public Builder signature(String signature) {
            this.signature = signature;
            return this;
        }

        public Builder returnType(String returnType) {
            this.returnType = returnType;
            return this;
        }

        public Builder owner(String owner) {
            this.owner = owner;
            return this;
        }

        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public Builder example(String example) {
            this.example = example;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public CompletionItem build() {
            return new CompletionItem(label, insertText, detail, kind,
                    signature, returnType, owner, documentation, example, category, priority);
        }
    }
}
