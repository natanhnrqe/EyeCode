package com.eyecode.runtime;

import com.eyecode.project.model.ProjectModel;

public interface RuntimeEngine {

    ProcessResult run(ProjectModel model);

    void stop();

    boolean isRunning();
}
