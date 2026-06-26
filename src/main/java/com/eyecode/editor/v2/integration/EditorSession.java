package com.eyecode.editor.v2.integration;

import com.eyecode.editor.EditorTab;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.ui.RichEditorView;

import java.io.File;
import java.util.function.Consumer;

public final class EditorSession {

    private final EditorTab tab;
    private final EditorBuffer buffer;
    private final RichEditorView view;
    private final File fileKey;
    private final EditorDocument.DirtyChangeListener dirtyListener;

    public EditorSession(EditorTab tab, EditorBuffer buffer, RichEditorView view, File fileKey, Consumer<Boolean> dirtyStateListener) {
        this.tab = tab;
        this.buffer = buffer;
        this.view = view;
        this.fileKey = fileKey;
        this.dirtyListener = dirty -> {
            tab.setDirty(dirty);
            if (dirtyStateListener != null) {
                dirtyStateListener.accept(dirty);
            }
        };
        this.buffer.getDocument().addDirtyChangeListener(dirtyListener);
        this.tab.setDirty(this.buffer.getDocument().isDirty());
    }

    public EditorTab getTab() { return tab; }

    public EditorBuffer getBuffer() { return buffer; }

    public RichEditorView getView() { return view; }

    public File getFileKey() { return fileKey; }

    public void dispose() {
        buffer.getDocument().removeDirtyChangeListener(dirtyListener);
    }
}
