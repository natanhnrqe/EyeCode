package com.eyecode.command;

import com.eyecode.editor.v2.ui.RichEditorView;

public class GoToDefinitionCommand implements Command {

    private final RichEditorView view;

    public GoToDefinitionCommand(RichEditorView view) {
        this.view = view;
    }

    @Override
    public String getName() {
        return "Go to Definition";
    }

    @Override
    public boolean isEnabled() {
        return view != null;
    }

    @Override
    public void execute() {
        if (view != null) {
            view.goToDefinition();
        }
    }
}
