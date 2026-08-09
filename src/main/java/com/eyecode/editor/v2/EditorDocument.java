package com.eyecode.editor.v2;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.DocumentSnapshotProvider;
import com.eyecode.editor.intelligence.document.LineMap;
import com.eyecode.editor.intelligence.document.TextChange;
import com.eyecode.editor.intelligence.events.DocumentChangeListener;
import com.eyecode.editor.intelligence.events.DocumentTextChangeEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Versioned, snapshot-backed document model.
 * <p>
 * Every accepted mutation advances a monotonic version and rebuilds an
 * immutable {@link LineMap}. Readers must never reach into the mutable text:
 * they consume {@link #snapshot()} instead. Mutations fired inside a
 * {@code DocumentTransaction} batch are applied atomically and produce a single
 * merged {@link DocumentTextChangeEvent}.
 */
public final class EditorDocument implements DocumentSnapshotProvider {

    private final StringBuilder content;
    private final List<EditorLine> lines;
    private final List<DirtyChangeListener> dirtyListeners;
    private final List<DocumentChangeListener> documentListeners;
    private final String sessionId;
    private Path sourceFile;
    private boolean dirty;
    private long version;
    private LineMap lineMap;
    private boolean inBatch;
    private String batchStartText;
    private long batchStartVersion;
    private LineMap batchStartLineMap;

    public EditorDocument() {
        this(null, "");
    }

    public EditorDocument(Path sourceFile, String text) {
        this.content = new StringBuilder();
        this.lines = new ArrayList<>();
        this.dirtyListeners = new ArrayList<>();
        this.documentListeners = new ArrayList<>();
        this.sessionId = UUID.randomUUID().toString();
        this.sourceFile = sourceFile;
        this.lineMap = LineMap.empty();
        setText(text);
        this.dirty = false;
    }

    /**
     * Identity of this document/session instance. Two documents on the same
     * file have distinct identities; untitled documents have their own.
     */
    public String sessionId() {
        return sessionId;
    }

    public String getText() {
        return content.toString();
    }

    public void setText(String text) {
        String newText = text == null ? "" : text;
        if (newText.contentEquals(content)) return;
        mutate(() -> {
            content.setLength(0);
            content.append(newText);
        });
    }

    public void insert(int offset, String text) {
        validateOffset(offset);
        if (text == null || text.isEmpty()) return;
        mutate(() -> content.insert(offset, text));
    }

    public void delete(int start, int end) {
        validateRange(start, end);
        if (start == end) return;
        mutate(() -> content.delete(start, end));
    }

    @Override
    public DocumentSnapshot snapshot() {
        return new DocumentSnapshot(version, content.toString(), lineMap, sourceFile, sessionId);
    }

    @Override
    public long currentVersion() {
        return version;
    }

    public int length() {
        return content.length();
    }

    public int offsetOf(EditorPosition position) {
        return lineMap.offsetOf(position.line(), position.column());
    }

    public EditorPosition positionOf(int offset) {
        int safe = Math.max(0, Math.min(offset, content.length()));
        return new EditorPosition(lineMap.lineOfOffset(safe), lineMap.columnOfOffset(safe));
    }

    /**
     * Starts a mutation batch. While a batch is active, mutations are applied
     * but no change events are fired. Must be paired with {@link #endBatch()}
     * or {@link #abortBatch()}.
     */
    public void beginBatch() {
        if (inBatch) {
            throw new IllegalStateException("A batch is already active on this document");
        }
        inBatch = true;
        batchStartText = content.toString();
        batchStartVersion = version;
        batchStartLineMap = lineMap;
    }

    /**
     * Ends a mutation batch, firing exactly one merged change event that
     * describes the whole batch as a single transaction.
     */
    public DocumentTextChangeEvent endBatch() {
        if (!inBatch) {
            throw new IllegalStateException("No active batch on this document");
        }
        inBatch = false;
        DocumentSnapshot before = new DocumentSnapshot(batchStartVersion, batchStartText, batchStartLineMap, sourceFile, sessionId);
        DocumentSnapshot after = snapshot();
        TextChange change = TextChange.between(before, after);
        batchStartText = null;
        batchStartLineMap = null;
        DocumentTextChangeEvent event = new DocumentTextChangeEvent(before, after, change, true);
        notifyDocumentChanged(event);
        return event;
    }

    /**
     * Aborts a mutation batch without firing any event.
     */
    public void abortBatch() {
        if (!inBatch) {
            throw new IllegalStateException("No active batch on this document");
        }
        inBatch = false;
        batchStartText = null;
        batchStartLineMap = null;
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

    public void addDocumentChangeListener(DocumentChangeListener listener) {
        if (listener != null && !documentListeners.contains(listener)) {
            documentListeners.add(listener);
        }
    }

    public void removeDocumentChangeListener(DocumentChangeListener listener) {
        documentListeners.remove(listener);
    }

    private void mutate(Runnable mutation) {
        String oldText = content.toString();
        long oldVersion = version;
        LineMap oldLineMap = lineMap;
        mutation.run();
        rebuild();
        version++;
        if (!inBatch) {
            fireTextChanged(oldText, oldVersion, oldLineMap);
        }
        setDirty(true);
    }

    private void fireTextChanged(String oldText, long oldVersion, LineMap oldLineMap) {
        DocumentSnapshot before = new DocumentSnapshot(oldVersion, oldText, oldLineMap, sourceFile, sessionId);
        DocumentSnapshot after = snapshot();
        TextChange change = TextChange.between(before, after);
        notifyDocumentChanged(new DocumentTextChangeEvent(before, after, change, false));
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

    private void notifyDocumentChanged(DocumentTextChangeEvent event) {
        for (DocumentChangeListener listener : List.copyOf(documentListeners)) {
            listener.onTextChanged(event);
        }
    }

    private void rebuild() {
        this.lineMap = LineMap.of(content);
        lines.clear();
        int count = lineMap.lineCount();
        for (int i = 0; i < count; i++) {
            String lineText = content.substring(lineMap.lineStartOffset(i), lineMap.lineEndOffset(i));
            lines.add(new EditorLine(i, lineText));
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
}
