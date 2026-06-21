package com.eyecode.eventbus.events;

import com.eyecode.eventbus.Event;
import java.io.File;

public final class FileClosedEvent implements Event {

    private final File file;

    public FileClosedEvent(File file) {
        this.file = file;
    }

    public File getFile() { return file; }
}
