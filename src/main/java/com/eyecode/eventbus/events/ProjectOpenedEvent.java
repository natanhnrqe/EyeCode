package com.eyecode.eventbus.events;

import com.eyecode.eventbus.Event;
import com.eyecode.project.model.ProjectModel;

public final class ProjectOpenedEvent implements Event {

    private final ProjectModel model;

    public ProjectOpenedEvent(ProjectModel model) {
        this.model = model;
    }

    public ProjectModel getModel() { return model; }
}
