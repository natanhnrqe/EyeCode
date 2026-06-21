package com.eyecode.command;

import com.eyecode.editor.EditorContext;
import com.eyecode.editor.EditorTab;
import com.eyecode.eventbus.EventBus;
import com.eyecode.eventbus.events.FileClosedEvent;

public class CloseTabCommand implements Command {

    private final CommandContext context;
    private final int tabIndex;

    public CloseTabCommand(CommandContext context, int tabIndex) {
        this.context = context;
        this.tabIndex = tabIndex;
    }

    @Override
    public String getName() {
        return "Close Tab";
    }

    @Override
    public boolean isEnabled() {
        return tabIndex >= 0
                && tabIndex < context.getEditorContext().getTabs().size();
    }

    @Override
    public void execute() {
        EditorContext editorContext = context.getEditorContext();
        if (!isEnabled()) return;

        EditorTab tab = editorContext.getTabs().get(tabIndex);
        editorContext.closeTab(tabIndex);

        EventBus eventBus = context.getEventBus();
        if (eventBus != null) {
            eventBus.publish(new FileClosedEvent(tab.getFile()));
        }
    }
}
