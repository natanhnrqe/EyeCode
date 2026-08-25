package com.eyecode.javafx.monaco;

public record MonacoEvent(Type type, String modelId, String content, long version,
                          int line, int column, int selectionEndLine, int selectionEndColumn,
                          double x, double y, Command command) {
    public enum Type { READY, CONTENT_CHANGED, CARET_CHANGED, HOVER, HOVER_EXIT, COMMAND, FOCUS_CHANGED }
    public enum Command { GO_TO_DEFINITION, DOCUMENTATION }

    public static MonacoEvent ready() {
        return new MonacoEvent(Type.READY, null, null, 0, 0, 0, 0, 0, 0, 0, null);
    }

    public static MonacoEvent contentChanged(String id, String content, long version) {
        return new MonacoEvent(Type.CONTENT_CHANGED, id, content, version, 0, 0, 0, 0, 0, 0, null);
    }

    public static MonacoEvent caretChanged(String id, int line, int column,
                                           int endLine, int endColumn) {
        return caretChanged(id, line, column, endLine, endColumn, 0);
    }

    public static MonacoEvent caretChanged(String id, int line, int column,
                                           int endLine, int endColumn, long version) {
        return new MonacoEvent(Type.CARET_CHANGED, id, null, version, line, column,
                endLine, endColumn, 0, 0, null);
    }

    public static MonacoEvent hover(String id, long version, int line, int column,
                                    double x, double y) {
        return new MonacoEvent(Type.HOVER, id, null, version, line, column, line, column, x, y, null);
    }

    public static MonacoEvent hoverExit(String id, long version) {
        return new MonacoEvent(Type.HOVER_EXIT, id, null, version, 0, 0, 0, 0, 0, 0, null);
    }

    public static MonacoEvent command(String id, long version, Command command) {
        return command(id, version, command, 0, 0);
    }

    public static MonacoEvent command(String id, long version, Command command, int line, int column) {
        return new MonacoEvent(Type.COMMAND, id, null, version, line, column, line, column, 0, 0, command);
    }
}
