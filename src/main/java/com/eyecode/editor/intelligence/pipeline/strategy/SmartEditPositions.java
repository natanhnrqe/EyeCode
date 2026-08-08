package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.LineMap;
import com.eyecode.editor.v2.EditorPosition;

/**
 * Offset to {@link EditorPosition} conversion based only on the immutable
 * snapshot exposed by {@code EditorCommandContext}. Keeps strategies free of
 * any direct document access.
 */
final class SmartEditPositions {

    private SmartEditPositions() {
    }

    static EditorPosition positionOf(DocumentSnapshot snapshot, int offset) {
        int safe = Math.max(0, Math.min(offset, snapshot.length()));
        LineMap map = snapshot.lineMap();
        return new EditorPosition(map.lineOfOffset(safe), map.columnOfOffset(safe));
    }
}
