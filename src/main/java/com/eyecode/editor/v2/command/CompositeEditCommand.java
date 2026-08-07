package com.eyecode.editor.v2.command;

import com.eyecode.editor.v2.EditorDocument;

import java.util.List;
import java.util.Objects;

/**
 * Executes a list of sub-commands as a single undoable unit.
 * <p>
 * {@link #execute} runs every sub-command in order; {@link #undo} reverts them
 * in reverse order.
 */
public final class CompositeEditCommand implements EditCommand {

    private final List<EditCommand> commands;

    public CompositeEditCommand(List<EditCommand> commands) {
        this.commands = List.copyOf(commands);
    }

    @Override
    public void execute(EditorDocument document) {
        for (EditCommand command : commands) {
            command.execute(document);
        }
    }

    @Override
    public void undo(EditorDocument document) {
        for (int i = commands.size() - 1; i >= 0; i--) {
            commands.get(i).undo(document);
        }
    }

    public List<EditCommand> getCommands() {
        return commands;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompositeEditCommand that)) return false;
        return commands.equals(that.commands);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commands);
    }
}
