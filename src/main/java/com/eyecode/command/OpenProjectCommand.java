package com.eyecode.command;

import com.eyecode.eventbus.EventBus;
import com.eyecode.eventbus.events.ProjectOpenedEvent;
import com.eyecode.project.model.ProjectModel;

import java.io.File;

public class OpenProjectCommand implements Command {

    private final CommandContext context;
    private final File projectDirectory;

    public OpenProjectCommand(CommandContext context, File projectDirectory) {
        this.context = context;
        this.projectDirectory = projectDirectory;
    }

    @Override
    public String getName() {
        return "Open Project";
    }

    @Override
    public boolean isEnabled() {
        return projectDirectory != null && projectDirectory.isDirectory();
    }

    @Override
    public void execute() {
        if (!isEnabled()) return;

        ProjectModel model = ProjectModel.fromDirectory(projectDirectory);
        context.setProjectModel(model);

        EventBus eventBus = context.getEventBus();
        if (eventBus != null) {
            eventBus.publish(new ProjectOpenedEvent(model));
        }
    }
}
