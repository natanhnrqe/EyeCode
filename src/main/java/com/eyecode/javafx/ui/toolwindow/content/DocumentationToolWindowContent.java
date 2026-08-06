package com.eyecode.javafx.ui.toolwindow.content;

public final class DocumentationToolWindowContent extends ToolWindowPlaceholderContent {

    public DocumentationToolWindowContent() {
        super("Documentation");
        addSection("Search Documentation", placeholder("Busque por documentação"));
        addSection("Favorites", placeholder("Nenhum favorito ainda"));
        addSection("Recent", placeholder("Nenhuma documentação recente"));
        addSection("Documentation Viewer", placeholder("Selecione uma documentação para visualizar"));
    }
}