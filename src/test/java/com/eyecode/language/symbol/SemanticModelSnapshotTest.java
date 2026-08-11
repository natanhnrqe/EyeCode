package com.eyecode.language.symbol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    void structuralEquality() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        SemanticModelSnapshot snapshot1 = table.snapshot(1, "test.java");
        SemanticModelSnapshot snapshot2 = table.snapshot(1, "test.java");

        assertEquals(snapshot1, snapshot2);
        assertEquals(snapshot1.hashCode(), snapshot2.hashCode());
    }

    @Test
    void differentVersionNotEqual() {
        ProjectSymbolTable table = new ProjectSymbolTable();
        SemanticModelSnapshot snapshot1 = table.snapshot(1, "test.java");
        SemanticModelSnapshot snapshot2 = table.snapshot(2, "test.java");

        assertNotEquals(snapshot1, snapshot2);
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
        assertEquals(0, snapshot.symbolTable().rootScope().id());
    }
}