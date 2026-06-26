package com.eyecode.editor;

import java.io.File;

public class EditorTab {

    private final File file;
    private String content;
    private boolean dirty;
    private long lastModified;

    public EditorTab(File file) {
        this.file = file;
        this.content = null;
        this.dirty = false;
        this.lastModified = file != null ? file.lastModified() : 0L;
    }

    public String getDisplayName() {
        String baseName = file != null ? file.getName() : "Untitled";
        return baseName + (dirty ? "*" : "");
    }

    public File getFile() { return file; }

    public String getContent() { return content; }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isDirty() { return dirty; }

    public void setDirty(boolean dirty) { this.dirty = dirty; }

    public long getLastModified() { return lastModified; }

    public void setLastModified(long lastModified) { this.lastModified = lastModified; }
}
