package com.eyecode.explorer.integration;

import com.eyecode.command.CommandContext;
import com.eyecode.editor.EditorContext;
import com.eyecode.eventbus.EventBus;
import com.eyecode.eventbus.events.ExplorerFileOpenRequestedEvent;
import com.eyecode.eventbus.events.ExplorerNodeSelectedEvent;
import com.eyecode.explorer.model.ExplorerNode;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ExplorerIntegrationController {

    private final EventBus eventBus;
    private final EditorContext editorContext;
    private final CommandContext commandContext;
    private ExplorerNode lastSelectedNode;

    public ExplorerIntegrationController(EventBus eventBus,
                                         EditorContext editorContext,
                                         CommandContext commandContext) {
        this.eventBus = eventBus;
        this.editorContext = editorContext;
        this.commandContext = commandContext;
        subscribe();
    }

    public ExplorerNode getLastSelectedNode() {
        return lastSelectedNode;
    }

    private void subscribe() {
        eventBus.subscribe(ExplorerFileOpenRequestedEvent.class, this::handleFileOpenRequested);
        eventBus.subscribe(ExplorerNodeSelectedEvent.class, this::handleNodeSelected);
    }

    private void handleFileOpenRequested(ExplorerFileOpenRequestedEvent event) {
        Path file = event.getFile();
        if (file == null || !Files.isRegularFile(file)) return;

        editorContext.openTab(file.toFile());
    }

    private void handleNodeSelected(ExplorerNodeSelectedEvent event) {
        lastSelectedNode = event.getNode();
        commandContext.getClass(); // keep dependency explicit for future command-scoped state.
    }
}
