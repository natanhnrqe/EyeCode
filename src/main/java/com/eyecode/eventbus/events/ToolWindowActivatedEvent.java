package com.eyecode.eventbus.events;

import com.eyecode.eventbus.Event;
import com.eyecode.workbench.toolwindow.ToolWindowPosition;

public final class ToolWindowActivatedEvent implements Event {

    private final String toolWindowId;
    private final ToolWindowPosition position;

    public ToolWindowActivatedEvent(String toolWindowId, ToolWindowPosition position) {
        this.toolWindowId = toolWindowId;
        this.position = position;
    }

    public String getToolWindowId() {
        return toolWindowId;
    }

    public ToolWindowPosition getPosition() {
        return position;
    }
}
