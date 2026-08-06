package com.eyecode.javafx.explorer;

import com.eyecode.project.model.ProjectModel;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class JavaFxExplorer extends VBox {

    private final ExplorerTreeView treeView;
    private ExplorerState state = ExplorerState.PROJECT;

    public JavaFxExplorer(ProjectModel model) {
        getStyleClass().add("java-fx-explorer");

        this.treeView = new ExplorerTreeView(model);
        this.treeView.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(this.treeView, Priority.ALWAYS);

        getChildren().setAll(contentFor(state));
    }

    public ExplorerState getState() {
        return state;
    }

    public void setState(ExplorerState newState) {
        if (newState == state) {
            return;
        }
        state = newState;
        getChildren().setAll(contentFor(newState));
    }

    public ExplorerTreeView getTreeView() {
        return treeView;
    }

    private Node contentFor(ExplorerState s) {
        return switch (s) {
            case PROJECT -> treeView;
            case SEARCH -> placeholder("Busca de arquivos e símbolos");
            case LEARN -> placeholder("Conteúdo de aprendizado");
            case ROADMAP -> placeholder("Roadmap de aprendizado");
            case DOCUMENTATION -> placeholder("Documentação do projeto");
            case PREVIEW -> placeholder("Pré-visualização");
        };
    }

    private Node placeholder(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("explorer-placeholder");
        return label;
    }
}
