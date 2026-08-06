package com.eyecode.javafx.ui.toolwindow.content;

public final class SettingsToolWindowContent extends ToolWindowPlaceholderContent {

    public SettingsToolWindowContent() {
        super("Settings");
        addSection("Appearance", placeholder("Em breve"));
        addSection("Editor", placeholder("Em breve"));
        addSection("Keymap", placeholder("Em breve"));
        addSection("Plugins", placeholder("Em breve"));
    }
}