package com.eyecode.editor.intelligence.document;

import com.eyecode.editor.v2.EditorDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentSnapshotTest {

    @Test
    void freshEmptyDocumentHasZeroVersion() {
        EditorDocument document = new EditorDocument();
        assertEquals(0, document.currentVersion());
        assertEquals("", document.snapshot().getText());
    }

    @Test
    void versionIsMonotonicAcrossMutations() {
        EditorDocument document = new EditorDocument();
        document.insert(0, "a");
        document.insert(1, "b");
        document.insert(2, "c");
        assertEquals(3, document.currentVersion());
    }

    @Test
    void snapshotsAreImmutable() {
        EditorDocument document = new EditorDocument();
        DocumentSnapshot first = document.snapshot();
        document.insert(0, "data");
        DocumentSnapshot second = document.snapshot();

        assertEquals("", first.getText());
        assertEquals("data", second.getText());
        assertEquals(0, first.version());
        assertEquals(1, second.version());
    }

    @Test
    void snapshotExposesLineMapOfText() {
        EditorDocument document = new EditorDocument();
        document.insert(0, "a\nb");
        DocumentSnapshot snapshot = document.snapshot();
        assertEquals(2, snapshot.lineMap().lineCount());
    }

    @Test
    void snapshotDocumentVersionWrapsVersion() {
        EditorDocument document = new EditorDocument();
        document.insert(0, "x");
        DocumentSnapshot snapshot = document.snapshot();
        assertEquals(new DocumentVersion(1), snapshot.documentVersion());
    }

    @Test
    void snapshotSlicesTextByRange() {
        EditorDocument document = new EditorDocument();
        document.insert(0, "hello world");
        DocumentSnapshot snapshot = document.snapshot();
        assertEquals("lo wo", snapshot.text(new TextRange(3, 8)));
    }

    @Test
    void snapshotTextRangeClampsOutOfBounds() {
        EditorDocument document = new EditorDocument();
        document.insert(0, "abc");
        DocumentSnapshot snapshot = document.snapshot();
        assertEquals("bc", snapshot.text(new TextRange(1, 99)));
        assertEquals("", snapshot.text(new TextRange(3, 99)));
    }

    @Test
    void documentVersionComparison() {
        DocumentVersion a = new DocumentVersion(3);
        DocumentVersion b = new DocumentVersion(5);
        assertTrue(b.isAfter(a));
        assertTrue(a.isBefore(b));
        assertFalse(a.isAfter(b));
        assertEquals(new DocumentVersion(6), b.next());
    }
}
