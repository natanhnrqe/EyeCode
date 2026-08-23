package com.eyecode.javafx.explorer;

import com.eyecode.project.model.ProjectModel;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.function.Consumer;

public final class JavaFxExplorer extends VBox {

    private final ExplorerTreeView treeView;
    private ExplorerState state = ExplorerState.PROJECT;

    public JavaFxExplorer(ProjectModel model) {
        this(model, path -> { });
    }

    public JavaFxExplorer(ProjectModel model, Consumer<Path> fileOpenHandler) {
        this(model, fileOpenHandler, request -> { });
    }

    public JavaFxExplorer(ProjectModel model, Consumer<Path> fileOpenHandler,
                          Consumer<ExplorerNewRequest> newActionHandler) {
        this(model, fileOpenHandler, newActionHandler, item -> { }, item -> { });
    }

    public JavaFxExplorer(ProjectModel model, Consumer<Path> fileOpenHandler,
                          Consumer<ExplorerNewRequest> newActionHandler,
                          Consumer<ProjectNode> renameAction,
                          Consumer<ProjectNode> deleteAction) {
        getStyleClass().add("java-fx-explorer");

        this.treeView = new ExplorerTreeView(model, fileOpenHandler, newActionHandler,
                renameAction, deleteAction);
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

    public void refresh(ProjectModel model) {
        treeView.reloadProject(model);
    }

    public void reloadProject(ProjectModel model) {
        refresh(model);
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
