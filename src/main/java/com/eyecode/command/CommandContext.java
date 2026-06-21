package com.eyecode.command;

import com.eyecode.editor.EditorContext;
import com.eyecode.eventbus.EventBus;
import com.eyecode.filesystem.FileSystemService;
import com.eyecode.project.model.ProjectModel;
import com.eyecode.runtime.RuntimeEngine;

public class CommandContext {

    private final EventBus eventBus;
    private final EditorContext editorContext;
    private ProjectModel projectModel;
    private final FileSystemService fileSystemService;
    private final RuntimeEngine runtimeEngine;

    public CommandContext(EventBus eventBus, EditorContext editorContext, ProjectModel projectModel) {
        this(eventBus, editorContext, projectModel, null, null);
    }

    public CommandContext(EventBus eventBus, EditorContext editorContext, ProjectModel projectModel,
                          FileSystemService fileSystemService, RuntimeEngine runtimeEngine) {
        this.eventBus = eventBus;
        this.editorContext = editorContext;
        this.projectModel = projectModel;
        this.fileSystemService = fileSystemService;
        this.runtimeEngine = runtimeEngine;
    }

    public EventBus getEventBus() { return eventBus; }

    public EditorContext getEditorContext() { return editorContext; }

    public ProjectModel getProjectModel() { return projectModel; }

    public void setProjectModel(ProjectModel projectModel) { this.projectModel = projectModel; }

    public FileSystemService getFileSystemService() { return fileSystemService; }

    public RuntimeEngine getRuntimeEngine() { return runtimeEngine; }
}
