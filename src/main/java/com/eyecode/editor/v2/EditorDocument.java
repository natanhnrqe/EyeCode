package com.eyecode.editor.v2;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class EditorDocument {

    private final StringBuilder content;
    private final List<EditorLine> lines;
    private Path sourceFile;
    private boolean dirty;

    public EditorDocument() {
        this(null, "");
    }

    public EditorDocument(Path sourceFile, String text) {
        this.content = new StringBuilder();
        this.lines = new ArrayList<>();
        this.sourceFile = sourceFile;
        setText(text);
        this.dirty = false;
    }

    public String getText() {
        return content.toString();
    }

    public void setText(String text) {
        content.setLength(0);
        content.append(text == null ? "" : text);
        rebuildLines();
        dirty = true;
    }

    public void insert(int offset, String text) {
        validateOffset(offset);
        if (text == null || text.isEmpty()) return;
        content.insert(offset, text);
        rebuildLines();
        dirty = true;
    }

    public void delete(int start, int end) {
        validateRange(start, end);
        if (start == end) return;
        content.delete(start, end);
        rebuildLines();
        dirty = true;
    }

    public int getLineCount() {
        return lines.size();
    }

    public String getLine(int index) {
        if (index < 0 || index >= lines.size()) {
            throw new IndexOutOfBoundsException("Line index out of range: " + index);
        }
        return lines.get(index).text();
    }

    public Path getSourceFile() { return sourceFile; }

    public void setSourceFile(Path sourceFile) { this.sourceFile = sourceFile; }

    public boolean isDirty() { return dirty; }

    public void markClean() { this.dirty = false; }

    private void rebuildLines() {
        lines.clear();
        String[] splitLines = content.toString().split("\\R", -1);
        for (int i = 0; i < splitLines.length; i++) {
            lines.add(new EditorLine(i, splitLines[i]));
        }
    }

    private void validateOffset(int offset) {
        if (offset < 0 || offset > content.length()) {
            throw new IndexOutOfBoundsException("Offset out of range: " + offset);
        }
    }

    private void validateRange(int start, int end) {
        if (start < 0 || end < start || end > content.length()) {
            throw new IndexOutOfBoundsException("Invalid range: " + start + ".." + end);
        }
    }
}
