package com.eyecode.javafx.editor;

import java.util.List;

public record IndentGuideLine(List<Integer> columns, int leadingIndentColumn, boolean blank) {

    public IndentGuideLine {
        columns = columns == null ? List.of() : List.copyOf(columns);
        if (leadingIndentColumn < 0) {
            throw new IllegalArgumentException("leadingIndentColumn must be non-negative");
        }
    }

    public boolean hasGuides() {
        return !columns.isEmpty();
    }

    public int deepestColumn() {
        return columns.isEmpty() ? 0 : columns.get(columns.size() - 1);
    }

    public boolean containsGuideColumn(int column) {
        return blank ? column <= leadingIndentColumn : column < leadingIndentColumn;
    }
}
