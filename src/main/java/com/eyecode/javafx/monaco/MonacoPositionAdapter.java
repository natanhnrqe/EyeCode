package com.eyecode.javafx.monaco;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;

public final class MonacoPositionAdapter {
    private MonacoPositionAdapter() { }

    public static int toOffset(DocumentSnapshot snapshot, int lineNumber, int column) {
        if (snapshot == null) return 0;
        int line = Math.max(1, lineNumber) - 1;
        int columnOffset = Math.max(1, column) - 1;
        return snapshot.lineMap().offsetOf(line, columnOffset);
    }
}
