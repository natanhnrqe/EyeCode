package com.eyecode.javafx.editor;

import com.eyecode.editor.intelligence.indent.IndentPolicy;
import com.eyecode.editor.intelligence.indent.JavaIndentPolicy;

import java.util.ArrayList;
import java.util.List;

public final class IndentGuideModel {

    private final IndentPolicy indentPolicy;

    public IndentGuideModel() {
        this(JavaIndentPolicy.INSTANCE);
    }

    public IndentGuideModel(IndentPolicy indentPolicy) {
        this.indentPolicy = indentPolicy == null ? JavaIndentPolicy.INSTANCE : indentPolicy;
    }

    public IndentGuideLine lineFor(String lineText) {
        String line = lineText == null ? "" : lineText;
        int visualColumn = leadingIndentColumn(line);
        if (visualColumn <= 0) {
            return new IndentGuideLine(List.of(), 0, line.isBlank());
        }
        int indentSize = Math.max(1, indentPolicy.indentSize());
        List<Integer> columns = new ArrayList<>();
        for (int column = indentSize; column <= visualColumn; column += indentSize) {
            columns.add(column);
        }
        return new IndentGuideLine(columns, visualColumn, line.isBlank());
    }

    private int leadingIndentColumn(String line) {
        int indentSize = Math.max(1, indentPolicy.indentSize());
        int visualColumn = 0;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == ' ') {
                visualColumn++;
                continue;
            }
            if (current == '\t') {
                int remainder = visualColumn % indentSize;
                visualColumn += remainder == 0 ? indentSize : indentSize - remainder;
                continue;
            }
            break;
        }
        return visualColumn;
    }
}
