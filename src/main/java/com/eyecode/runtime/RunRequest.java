package com.eyecode.runtime;

import com.eyecode.project.model.ProjectModel;

public record RunRequest(ProjectModel project, RunConfiguration configuration) {
    public RunRequest(ProjectModel project) {
        this(project, null);
    }

    public RunRequest {
        if (project == null) {
            throw new IllegalArgumentException("Project model is required");
        }
    }

    public String configurationLabel() {
        return configuration == null ? project.getName() : configuration.displayName();
    }
}
