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

    public static int toOffset(DocumentSnapshot snapshot, int monacoUtf16Offset) {
        if (snapshot == null) return 0;
        int safe = Math.max(0, Math.min(monacoUtf16Offset, snapshot.text().length()));
        return snapshot.lineMap().offsetOf(snapshot.lineMap().lineOfOffset(safe),
                snapshot.lineMap().columnOfOffset(safe));
    }
}
