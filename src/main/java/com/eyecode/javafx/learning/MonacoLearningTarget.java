package com.eyecode.javafx.learning;

import com.eyecode.editor.v2.syntax.TokenType;

public record MonacoLearningTarget(
        String modelId,
        long version,
        int startOffset,
        int endOffset,
        int line,
        int column,
        String text,
        String documentText,
        TokenType tokenType
) {
    public MonacoLearningTarget {
        modelId = modelId == null ? "" : modelId;
        startOffset = Math.max(0, startOffset);
        endOffset = Math.max(startOffset, endOffset);
        line = Math.max(1, line);
        column = Math.max(1, column);
        text = text == null ? "" : text;
        documentText = documentText == null ? "" : documentText;
    }

    public boolean sameIdentity(MonacoLearningTarget other) {
        return other != null && version == other.version
                && startOffset == other.startOffset && endOffset == other.endOffset
                && modelId.equals(other.modelId);
    }

    public MonacoLearningTarget(String modelId, long version, int startOffset, int endOffset,
                                int line, int column, String text) {
        this(modelId, version, startOffset, endOffset, line, column, text, "", null);
    }
}
