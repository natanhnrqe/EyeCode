package com.eyecode.workbench.editor;

import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.EditorSelection;

import java.nio.file.Path;

public final class EditorSession {

    private final String sessionId;
    private final String documentId;
    private Path file;
    private boolean preview;
    private boolean pinned;
    private EditorPosition caretState;
    private EditorSelection selectionState;
    private EditorScroll scrollState;
    private SessionState state;

    EditorSession(String sessionId, String documentId, Path file, OpenOptions options) {
        this.sessionId = sessionId;
        this.documentId = documentId;
        this.file = file;
        this.preview = options.preview();
        this.pinned = options.pinned();
        this.caretState = options.restoreCaret() != null
                ? options.restoreCaret()
                : new EditorPosition(0, 0);
        this.selectionState = new EditorSelection(this.caretState, this.caretState);
        this.scrollState = options.restoreScroll() != null
                ? options.restoreScroll()
                : EditorScroll.zero();
        this.state = SessionState.CLOSED;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public Path getFile() {
        return file;
    }

    public void setFile(Path file) {
        this.file = file;
    }

    public boolean isPreview() {
        return preview;
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public EditorPosition getCaretState() {
        return caretState;
    }

    public void setCaretState(EditorPosition caretState) {
        this.caretState = caretState == null ? new EditorPosition(0, 0) : caretState;
    }

    public EditorSelection getSelectionState() {
        return selectionState;
    }

    public void setSelectionState(EditorSelection selectionState) {
        this.selectionState = selectionState == null
                ? new EditorSelection(this.caretState, this.caretState)
                : selectionState;
    }

    public EditorScroll getScrollState() {
        return scrollState;
    }

    public void setScrollState(EditorScroll scrollState) {
        this.scrollState = scrollState == null ? EditorScroll.zero() : scrollState;
    }

    public SessionState getState() {
        return state;
    }

    public void setState(SessionState state) {
        this.state = state;
    }

    public String getDisplayName() {
        if (file != null) {
            String name = file.getFileName() != null ? file.getFileName().toString() : file.toString();
            return name;
        }
        return documentId;
    }
}
