package com.eyecode.command;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.ui.RichEditorView;
import org.junit.jupiter.api.Test;

import javax.swing.JTextPane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoToDefinitionCommandTest {

    @Test
    void name_isGoToDefinition() {
        GoToDefinitionCommand cmd = new GoToDefinitionCommand(null);
        assertEquals("Go to Definition", cmd.getName());
    }

    @Test
    void isEnabled_isFalse_whenViewIsNull() {
        GoToDefinitionCommand cmd = new GoToDefinitionCommand(null);
        assertFalse(cmd.isEnabled());
    }

    @Test
    void isEnabled_isTrue_whenViewIsNotNull() {
        RichEditorView view = new RichEditorView(new EditorBuffer(new EditorDocument(null, "class C {}")));
        try {
            GoToDefinitionCommand cmd = new GoToDefinitionCommand(view);
            assertTrue(cmd.isEnabled());
        } finally {
            view.dispose();
        }
    }

    @Test
    void execute_invokesGoToDefinition() {
        String source = "class C {\n" +
                "    int value = 1;\n" +
                "    void m() { int x = value; }\n" +
                "}\n";
        RichEditorView view = new RichEditorView(new EditorBuffer(new EditorDocument(null, source)));
        try {
            JTextPane pane = view.getTextPane();
            int refOffset = source.indexOf("= value");
            pane.setCaretPosition(refOffset + 2);
            view.getBuffer().moveCaret(view.getBuffer().getDocument().positionOf(refOffset + 2));

            GoToDefinitionCommand cmd = new GoToDefinitionCommand(view);
            cmd.execute();

            int declOffset = source.indexOf("int value =");
            assertEquals(declOffset, pane.getCaretPosition());
        } finally {
            view.dispose();
        }
    }

    @Test
    void execute_isSafe_whenViewIsNull() {
        GoToDefinitionCommand cmd = new GoToDefinitionCommand(null);
        cmd.execute();
    }
}
