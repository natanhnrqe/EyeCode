package com.eyecode.eventbus.events;

import com.eyecode.eventbus.Event;

public final class RuntimeStatusEvent implements Event {

    private final boolean running;
    private final String statusText;

    public RuntimeStatusEvent(boolean running, String statusText) {
        this.running = running;
        this.statusText = statusText;
    }

    public boolean isRunning() { return running; }

    public String getStatusText() { return statusText; }
}
