package com.eyecode.eventbus.events;

import com.eyecode.eventbus.Event;
import java.io.File;

public final class FileModifiedEvent implements Event {

    private final File file;
    private final boolean dirty;

    public FileModifiedEvent(File file, boolean dirty) {
        this.file = file;
        this.dirty = dirty;
    }

    public File getFile() { return file; }

    public boolean isDirty() { return dirty; }
}
