package com.eyecode.editor.v2.command;

import com.eyecode.editor.v2.EditorDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandManagerTest {

    @Test
    void consecutiveAdjacentInsertsCoalesceIntoSingleUndoStep() {
        EditorDocument document = new EditorDocument();
        CommandManager manager = new CommandManager();

        document.insert(0, "h");
        manager.recordTextChange("", "h");
        document.insert(1, "i");
        manager.recordTextChange("h", "hi");

        assertEquals("hi", document.getText());
        manager.undo(document);
        assertEquals("", document.getText());
        manager.redo(document);
        assertEquals("hi", document.getText());
    }

    @Test
    void nonAppendOnlyReplacementIsNotCoalesced() {
        EditorDocument document = new EditorDocument();
        CommandManager manager = new CommandManager();
        document.insert(0, "a");
        manager.recordTextChange("", "a");
        document.insert(1, "b");
        manager.recordTextChange("a", "ab");
        document.insert(0, "X");
        manager.recordTextChange("ab", "Xab");

        assertEquals("Xab", document.getText());
        manager.undo(document);
        assertEquals("ab", document.getText());
        manager.undo(document);
        assertEquals("", document.getText());
        manager.redo(document);
        assertEquals("ab", document.getText());
        manager.redo(document);
        assertEquals("Xab", document.getText());
    }

    @Test
    void appendOnlyReplacementsCoalesceIntoSingleUndoStep() {
        EditorDocument document = new EditorDocument();
        CommandManager manager = new CommandManager();
        document.insert(0, "a");
        manager.recordTextChange("", "a");
        document.insert(1, "b");
        manager.recordTextChange("a", "ab");
        document.insert(2, "c");
        manager.recordTextChange("ab", "abc");

        assertEquals("abc", document.getText());
        manager.undo(document);
        assertEquals("", document.getText());
        assertFalse(manager.canUndo());
    }

    @Test
    void recordGroupPushesWithoutReExecuting() {
        EditorDocument document = new EditorDocument();
        CommandManager manager = new CommandManager();

        InsertTextCommand first = new InsertTextCommand(0, "ab");
        InsertTextCommand second = new InsertTextCommand(2, "cd");
        first.execute(document);
        second.execute(document);
        assertEquals("abcd", document.getText());

        manager.recordGroup(new CompositeEditCommand(java.util.List.of(first, second)));
        assertEquals("abcd", document.getText());

        manager.undo(document);
        assertEquals("", document.getText());
        manager.redo(document);
        assertEquals("abcd", document.getText());
    }

    @Test
    void executeRunsAndRecordsCommand() {
        EditorDocument document = new EditorDocument();
        CommandManager manager = new CommandManager();
        manager.execute(new InsertTextCommand(0, "x"), document);

        assertEquals("x", document.getText());
        manager.undo(document);
        assertEquals("", document.getText());
    }

    @Test
    void programmaticUpdateSuppressesRecording() {
        EditorDocument document = new EditorDocument();
        CommandManager manager = new CommandManager();
        manager.runProgrammaticUpdate(() -> document.insert(0, "bulk"));

        assertEquals("bulk", document.getText());
        assertFalse(manager.canUndo());
    }

    @Test
    void recordTextChangeIgnoresIdenticalText() {
        EditorDocument document = new EditorDocument();
        CommandManager manager = new CommandManager();
        manager.recordTextChange("same", "same");
        assertFalse(manager.canUndo());
    }
}
