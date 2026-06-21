package com.eyecode.eventbus.events;

import com.eyecode.eventbus.Event;

import java.nio.file.Path;

public final class ExplorerFileOpenRequestedEvent implements Event {

    private final Path file;

    public ExplorerFileOpenRequestedEvent(Path file) {
        this.file = file;
    }

    public Path getFile() { return file; }
}
