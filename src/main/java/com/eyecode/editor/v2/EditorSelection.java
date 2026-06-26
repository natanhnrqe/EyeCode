package com.eyecode.editor.v2;

public final class EditorSelection {

    private final EditorPosition start;
    private final EditorPosition end;

    public EditorSelection(EditorPosition start, EditorPosition end) {
        this.start = start;
        this.end = end;
    }

    public boolean isEmpty() {
        return start.equals(end);
    }

    public boolean isSingleLine() {
        return start.line() == end.line();
    }

    public EditorPosition getStart() { return start; }

    public EditorPosition getEnd() { return end; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EditorSelection that)) return false;
        return start.equals(that.start) && end.equals(that.end);
    }

    @Override
    public int hashCode() {
        int result = start.hashCode();
        result = 31 * result + end.hashCode();
        return result;
    }
}
