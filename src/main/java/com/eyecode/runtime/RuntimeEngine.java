package com.eyecode.runtime;

import com.eyecode.project.model.ProjectModel;

public interface RuntimeEngine {

    /**
     * Starts project execution asynchronously and returns an immediate start result.
     * The completed execution result is available from the implementation after completion.
     */
    ProcessResult run(ProjectModel model);

    void stop();

    boolean isRunning();
}
