package com.eyecode.eventbus.events;

import com.eyecode.eventbus.Event;
import com.eyecode.workbench.editor.EditorSession;

public final class EditorActivatedEvent implements Event {

    private final EditorSession session;

    public EditorActivatedEvent(EditorSession session) {
        this.session = session;
    }

    public EditorSession getSession() {
        return session;
    }
}
