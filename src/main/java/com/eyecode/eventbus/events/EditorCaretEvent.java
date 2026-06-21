package com.eyecode.eventbus.events;

import com.eyecode.eventbus.Event;

public final class EditorCaretEvent implements Event {

    private final int line;
    private final int column;

    public EditorCaretEvent(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public int getLine() { return line; }

    public int getColumn() { return column; }
}
