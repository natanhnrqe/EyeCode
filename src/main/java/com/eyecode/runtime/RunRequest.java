package com.eyecode.runtime;

import com.eyecode.project.model.ProjectModel;

public record RunRequest(ProjectModel project) {
    public RunRequest {
        if (project == null) {
            throw new IllegalArgumentException("Project model is required");
        }
    }
}
