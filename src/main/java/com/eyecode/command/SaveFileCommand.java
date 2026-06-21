package com.eyecode.command;

import com.eyecode.editor.EditorContext;
import com.eyecode.editor.EditorTab;
import com.eyecode.eventbus.EventBus;
import com.eyecode.eventbus.events.FileModifiedEvent;

public class SaveFileCommand implements Command {

    private final CommandContext context;

    public SaveFileCommand(CommandContext context) {
        this.context = context;
    }

    @Override
    public String getName() {
        return "Save File";
    }

    @Override
    public boolean isEnabled() {
        EditorTab activeTab = context.getEditorContext().getActiveTab();
        return activeTab != null && activeTab.isDirty();
    }

    @Override
    public void execute() {
        EditorContext editorContext = context.getEditorContext();
        EditorTab activeTab = editorContext.getActiveTab();
        if (activeTab == null) return;

        // TODO: persist content through FileSystemService once available
        // context.getFileSystemService().write(activeTab.getFile(), activeTab.getContent());

        editorContext.markClean(editorContext.getActiveTabIndex());

        EventBus eventBus = context.getEventBus();
        if (eventBus != null) {
            eventBus.publish(new FileModifiedEvent(activeTab.getFile(), false));
        }
    }
}
