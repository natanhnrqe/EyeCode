package com.eyecode.javafx.monaco;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MonacoPositionAdapterTest {
    @Test
    void convertsMonacoOneBasedUtf16PositionsToDocumentOffsets() {
        DocumentSnapshot snapshot = DocumentSnapshot.oneShot("a\n\ncafé");

        assertEquals(0, MonacoPositionAdapter.toOffset(snapshot, 1, 1));
        assertEquals(1, MonacoPositionAdapter.toOffset(snapshot, 1, 2));
        assertEquals(2, MonacoPositionAdapter.toOffset(snapshot, 2, 1));
        assertEquals(3, MonacoPositionAdapter.toOffset(snapshot, 3, 1));
        assertEquals(7, MonacoPositionAdapter.toOffset(snapshot, 3, 5));
    }

    @Test
    void clampsInvalidPositionsThroughTheDocumentLineMap() {
        DocumentSnapshot snapshot = DocumentSnapshot.oneShot("one\ntwo");

        assertEquals(0, MonacoPositionAdapter.toOffset(snapshot, 0, 0));
        assertEquals(snapshot.text().length(), MonacoPositionAdapter.toOffset(snapshot, 99, 99));
    }
}
