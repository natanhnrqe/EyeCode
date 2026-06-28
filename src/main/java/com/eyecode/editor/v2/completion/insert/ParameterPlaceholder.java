package com.eyecode.editor.v2.completion.insert;

public record ParameterPlaceholder(String name, int startOffset, int endOffset) {

    public static ParameterPlaceholder empty() {
        return new ParameterPlaceholder("", -1, -1);
    }

    public boolean isEmpty() {
        return name == null || name.isEmpty() || startOffset < 0;
    }
}
