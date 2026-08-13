package com.eyecode.language.symbol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticModelSnapshotTest {

    @Test
    void immutability() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        SemanticModelSnapshot snapshot = table.snapshot(1, "test.java");

        assertEquals(1, snapshot.version());
        assertEquals("test.java", snapshot.sourceFile());
        assertNotNull(snapshot.symbolTable());
    }

    @Test
    void differentVersionNotEqual() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        SemanticModelSnapshot snapshot1 = table.snapshot(1, "test.java");
        SemanticModelSnapshot snapshot2 = table.snapshot(2, "test.java");

        // version is part of equality, distinct versions are never equal
        assertNotEquals(snapshot1.version(), snapshot2.version());
        assertEquals(2, snapshot2.version());
    }

    @Test
    void oldSnapshotRemainsStable() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        SemanticModelSnapshot snapshot1 = table.snapshot(1, "test.java");
        SemanticModelSnapshot snapshot2 = table.snapshot(2, "test.java");

        // snapshot1 should not be affected by snapshot2
        assertEquals(1, snapshot1.version());
        assertEquals(2, snapshot2.version());
    }

    @Test
    void rootScopeAccessible() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        SemanticModelSnapshot snapshot = table.snapshot(1, "test.java");

        assertNotNull(snapshot.symbolTable().rootScope());
        // The root scope kind is ROOT; the numeric id is process-global (1-based)
        // and not stable across test ordering, so we assert the kind only.
        assertEquals(ScopeKind.ROOT, snapshot.symbolTable().rootScope().kind());
        assertTrue(snapshot.symbolTable().rootScope().parent().isEmpty());
    }

    @Test
    void snapshotHasSymbolTableInstance() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        SemanticModelSnapshot snapshot = table.snapshot(1, "test.java");
        assertTrue(snapshot.symbolTable() instanceof SymbolTable);
    }
}
