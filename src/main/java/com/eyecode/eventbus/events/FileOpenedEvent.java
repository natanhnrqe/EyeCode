package com.eyecode.eventbus.events;

import com.eyecode.eventbus.Event;
import java.io.File;

public final class FileOpenedEvent implements Event {

    private final File file;

    public FileOpenedEvent(File file) {
        this.file = file;
    }

    public File getFile() { return file; }
}
