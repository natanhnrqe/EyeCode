package com.eyecode.javafx.designsystem;

import javafx.scene.Node;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class FxCard extends VBox {

    public FxCard() {
        getStyleClass().add("fx-card");
    }

    public FxCard(Node content) {
        this();
        setContent(content);
    }

    public void setContent(Node content) {
        if (content == null) {
            throw new IllegalStateException("FxCard recebeu conteúdo nulo.");
        }
        Node body = wrapBody(content);
        getChildren().setAll(body);
    }

    private Node wrapBody(Node content) {
        VBox body = new VBox();
        body.getStyleClass().add("card-body");
        body.getChildren().add(content);
        VBox.setVgrow(body, Priority.ALWAYS);
        VBox.setVgrow(content, Priority.ALWAYS);
        return body;
    }

    public void addHeader(Node header) {
        if (header == null) {
            throw new IllegalStateException("FxCard recebeu cabeçalho nulo.");
        }
        header.getStyleClass().add("card-header");
        getChildren().add(0, header);
    }

    public void removeHeader() {
        if (!getChildren().isEmpty()
                && getChildren().get(0).getStyleClass().contains("card-header")) {
            getChildren().remove(0);
        }
    }
}
