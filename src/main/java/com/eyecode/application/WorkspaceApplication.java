package com.eyecode.application;

import com.eyecode.project.ProjectLifecycleService;
import com.eyecode.runtime.RunService;
import com.eyecode.terminal.TerminalService;
import com.eyecode.workbench.editor.EditorManager;

import java.util.Objects;

public final class WorkspaceApplication implements AutoCloseable {
    private final EditorManager editorManager;
    private final ProjectLifecycleService projectLifecycleService;
    private final RunService runService;
    private final TerminalService terminalService;
    private boolean closed;

    public WorkspaceApplication(EditorManager editorManager,
                                ProjectLifecycleService projectLifecycleService,
                                RunService runService,
                                TerminalService terminalService) {
        this.editorManager = Objects.requireNonNull(editorManager, "editorManager");
        this.projectLifecycleService = Objects.requireNonNull(projectLifecycleService, "projectLifecycleService");
        this.runService = Objects.requireNonNull(runService, "runService");
        this.terminalService = Objects.requireNonNull(terminalService, "terminalService");
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        editorManager.dispose();
        runService.dispose();
        terminalService.dispose();
        projectLifecycleService.close();
    }
}
