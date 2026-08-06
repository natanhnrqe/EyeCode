package com.eyecode.javafx.ui.toolwindow.content;

import com.eyecode.javafx.designsystem.FxSpacing;
import javafx.scene.control.TextField;

public final class SearchToolWindowContent extends ToolWindowPlaceholderContent {

    public SearchToolWindowContent() {
        super("Search");
        addSection("Search Box", searchBox());
        addSection("Recent Searches", placeholder("Nenhuma pesquisa recente"));
        addSection("Results", placeholder("Nenhum resultado ainda"));
    }

    private TextField searchBox() {
        TextField field = new TextField();
        field.setPromptText("Search...");
        field.getStyleClass().add("toolwindow-search-field");
        field.setPrefHeight(FxSpacing.HUGE);
        return field;
    }
}