package com.eyecode.javafx.monaco;

public record MonacoEvent(Type type, String modelId, String content, long version,
                          int line, int column, int selectionEndLine, int selectionEndColumn) {
    public enum Type { READY, CONTENT_CHANGED, CARET_CHANGED, FOCUS_CHANGED }

    public static MonacoEvent ready() {
        return new MonacoEvent(Type.READY, null, null, 0, 0, 0, 0, 0);
    }

    public static MonacoEvent contentChanged(String id, String content, long version) {
        return new MonacoEvent(Type.CONTENT_CHANGED, id, content, version, 0, 0, 0, 0);
    }

    public static MonacoEvent caretChanged(String id, int line, int column,
                                           int endLine, int endColumn) {
        return new MonacoEvent(Type.CARET_CHANGED, id, null, 0, line, column, endLine, endColumn);
    }
}
