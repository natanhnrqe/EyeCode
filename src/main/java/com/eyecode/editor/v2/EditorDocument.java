package com.eyecode.editor.v2;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class EditorDocument {

    private final StringBuilder content;
    private final List<EditorLine> lines;
    private final List<DirtyChangeListener> dirtyListeners;
    private final List<TextChangeListener> textListeners;
    private Path sourceFile;
    private boolean dirty;

    public EditorDocument() {
        this(null, "");
    }

    public EditorDocument(Path sourceFile, String text) {
        this.content = new StringBuilder();
        this.lines = new ArrayList<>();
        this.dirtyListeners = new ArrayList<>();
        this.textListeners = new ArrayList<>();
        this.sourceFile = sourceFile;
        setText(text);
        this.dirty = false;
    }

    public String getText() {
        return content.toString();
    }

    public void setText(String text) {
        String newText = text == null ? "" : text;
        if (newText.contentEquals(content)) return;
        String oldText = content.toString();
        content.setLength(0);
        content.append(newText);
        rebuildLines();
        notifyTextChanged(oldText, newText);
        setDirty(true);
    }

    public void insert(int offset, String text) {
        validateOffset(offset);
        if (text == null || text.isEmpty()) return;
        String oldText = content.toString();
        content.insert(offset, text);
        rebuildLines();
        notifyTextChanged(oldText, content.toString());
        setDirty(true);
    }

    public void delete(int start, int end) {
        validateRange(start, end);
        if (start == end) return;
        String oldText = content.toString();
        content.delete(start, end);
        rebuildLines();
        notifyTextChanged(oldText, content.toString());
        setDirty(true);
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

    public void markClean() { setDirty(false); }

    public void addDirtyChangeListener(DirtyChangeListener listener) {
        if (listener != null && !dirtyListeners.contains(listener)) {
            dirtyListeners.add(listener);
        }
    }

    public void removeDirtyChangeListener(DirtyChangeListener listener) {
        dirtyListeners.remove(listener);
    }

    public void addTextChangeListener(TextChangeListener listener) {
        if (listener != null && !textListeners.contains(listener)) {
            textListeners.add(listener);
        }
    }

    public void removeTextChangeListener(TextChangeListener listener) {
        textListeners.remove(listener);
    }

    private void setDirty(boolean dirty) {
        if (this.dirty == dirty) {
            return;
        }

        this.dirty = dirty;
        for (DirtyChangeListener listener : List.copyOf(dirtyListeners)) {
            listener.onDirtyChanged(dirty);
        }
    }

    private void notifyTextChanged(String oldText, String newText) {
        for (TextChangeListener listener : List.copyOf(textListeners)) {
            listener.onTextChanged(oldText, newText);
        }
    }

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

    public interface DirtyChangeListener {
        void onDirtyChanged(boolean dirty);
    }

    public interface TextChangeListener {
        void onTextChanged(String oldText, String newText);
    }
}
