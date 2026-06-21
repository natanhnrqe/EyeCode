package com.eyecode.eventbus.events;

import com.eyecode.eventbus.Event;

public final class BuildOutputEvent implements Event {

    private final String outputLine;

    public BuildOutputEvent(String outputLine) {
        this.outputLine = outputLine;
    }

    public String getOutputLine() { return outputLine; }
}
