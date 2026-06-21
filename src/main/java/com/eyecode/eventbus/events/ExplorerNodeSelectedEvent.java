package com.eyecode.eventbus.events;

import com.eyecode.eventbus.Event;
import com.eyecode.explorer.model.ExplorerNode;

public final class ExplorerNodeSelectedEvent implements Event {

    private final ExplorerNode node;

    public ExplorerNodeSelectedEvent(ExplorerNode node) {
        this.node = node;
    }

    public ExplorerNode getNode() { return node; }
}
