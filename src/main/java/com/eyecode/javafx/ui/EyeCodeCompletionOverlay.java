package com.eyecode.javafx.ui;

import com.eyecode.javafx.designsystem.JavaFxIconManager;
import com.eyecode.javafx.monaco.JavaFxMonacoEditorSurface;
import com.eyecode.javafx.monaco.MonacoCommand;
import com.eyecode.javafx.monaco.MonacoCompletionItem;
import com.eyecode.javafx.monaco.MonacoCompletionRequest;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public final class EyeCodeCompletionOverlay extends Pane {
    private static final double WIDTH = 600;
    private static final double ROW_HEIGHT = 32;
    private static final double MAX_LIST_HEIGHT = ROW_HEIGHT * 8 + 8;

    private final VBox popup = new VBox();
    private final ListView<MonacoCompletionItem> list = new ListView<>();
    private final VBox details = new VBox();
    private JavaFxMonacoEditorSurface editorSurface;
    private MonacoCompletionRequest request;
    private List<MonacoCompletionItem> items = List.of();

    public EyeCodeCompletionOverlay() {
        getStyleClass().add("eyecode-completion-global-overlay");
        popup.getStyleClass().add("eyecode-completion-popup-fx");
        list.getStyleClass().add("eyecode-completion-list-fx");
        details.getStyleClass().add("eyecode-completion-details-fx");
        list.setCellFactory(view -> new CompletionCell());
        list.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, item) -> renderDetails(item));
        list.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) acceptSelected();
        });
        list.setPrefHeight(MAX_LIST_HEIGHT);
        list.setMaxHeight(MAX_LIST_HEIGHT);
        list.setPrefWidth(WIDTH);
        VBox.setVgrow(list, Priority.NEVER);
        popup.getChildren().addAll(list, details);
        popup.setManaged(false);
        getChildren().add(popup);
        setMinSize(0, 0);
        setMouseTransparent(true);
        setPickOnBounds(false);
        setVisible(false);
        setFocusTraversable(false);
        sceneProperty().addListener((obs, oldScene, scene) -> {
            if (oldScene != null) oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyEvent);
            if (scene != null) scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyEvent);
        });
    }

    public void showCompletion(JavaFxMonacoEditorSurface surface, double caretX, double caretY,
                               MonacoCompletionRequest completionRequest,
                               List<MonacoCompletionItem> completionItems) {
        if (surface == null || completionRequest == null || completionItems == null || completionItems.isEmpty()
                || surface.getScene() == null) {
            hideCompletion();
            return;
        }
        editorSurface = surface;
        request = completionRequest;
        items = List.copyOf(completionItems);
        list.getItems().setAll(items);
        list.getSelectionModel().select(0);
        renderDetails(items.get(0));
        Point2D scenePoint = surface.localToScene(caretX, caretY);
        Point2D overlayPoint = sceneToLocal(scenePoint);
        popup.relocate(Math.max(8, overlayPoint.getX()), Math.max(8, overlayPoint.getY()));
        popup.applyCss();
        popup.autosize();
        setMouseTransparent(false);
        setVisible(true);
        requestLayout();
    }

    public void hideCompletion() {
        setVisible(false);
        setMouseTransparent(true);
        request = null;
        editorSurface = null;
        items = List.of();
        list.getItems().clear();
        details.getChildren().clear();
    }

    public void hideMarker() {
        hideCompletion();
    }

    private void handleKeyEvent(KeyEvent event) {
        if (!isVisible() || request == null || items.isEmpty()) return;
        if (event.getCode() == KeyCode.DOWN) {
            list.getSelectionModel().selectNext();
            list.scrollTo(list.getSelectionModel().getSelectedIndex());
            event.consume();
        } else if (event.getCode() == KeyCode.UP) {
            list.getSelectionModel().selectPrevious();
            list.scrollTo(list.getSelectionModel().getSelectedIndex());
            event.consume();
        } else if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
            acceptSelected();
            event.consume();
        } else if (event.getCode() == KeyCode.ESCAPE) {
            hideCompletion();
            event.consume();
        }
    }

    private void acceptSelected() {
        if (editorSurface == null || request == null) return;
        int index = list.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= items.size()) return;
        MonacoCompletionItem item = items.get(index);
        JavaFxMonacoEditorSurface surface = editorSurface;
        MonacoCompletionRequest completion = request;
        hideCompletion();
        if (item.snippet()) {
            surface.send(new MonacoCommand.InsertSnippet(completion.modelId(), item.replaceStart(),
                    item.replaceEnd(), item.insertText()));
        } else {
            surface.send(new MonacoCommand.ApplyEdit(completion.modelId(), item.replaceStart(),
                    item.replaceEnd(), item.insertText()));
        }
    }

    private void renderDetails(MonacoCompletionItem item) {
        details.getChildren().clear();
        if (item == null) return;
        details.getChildren().add(detailHeader(item));
        if (!item.documentation().isBlank()) details.getChildren().add(label(item.documentation(), "documentation"));
        if (!item.example().isBlank()) {
            details.getChildren().add(label("Example", "example-title"));
            details.getChildren().add(label(item.example(), "example"));
        }
    }

    private Node detailHeader(MonacoCompletionItem item) {
        HBox row = new HBox(10);
        row.getStyleClass().add("eyecode-completion-detail-header-fx");
        String signature = item.signature().isBlank() ? item.label() : item.signature();
        if (!item.returnType().isBlank()) row.getChildren().add(label(item.returnType(), "detail-return"));
        row.getChildren().add(label(signature, "detail-name"));
        if (!item.owner().isBlank()) {
            Label owner = label(item.owner(), "detail-owner");
            HBox.setHgrow(owner, Priority.ALWAYS);
            row.getChildren().add(owner);
        }
        return row;
    }

    private Label label(String value, String style) {
        Label label = new Label(value);
        label.getStyleClass().add("eyecode-completion-" + style + "-fx");
        label.setWrapText(true);
        return label;
    }

    private final class CompletionCell extends ListCell<MonacoCompletionItem> {
        private final HBox row = new HBox(8);
        private final Label main = new Label();
        private final Label returnType = new Label();
        private final Label owner = new Label();

        private CompletionCell() {
            row.getStyleClass().add("eyecode-completion-row-fx");
            main.getStyleClass().add("eyecode-completion-main-fx");
            returnType.getStyleClass().add("eyecode-completion-return-fx");
            owner.getStyleClass().add("eyecode-completion-owner-fx");
            HBox.setHgrow(main, Priority.ALWAYS);
            row.getChildren().addAll(main, returnType, owner);
        }

        @Override
        protected void updateItem(MonacoCompletionItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            String signature = item.signature().isBlank() ? item.label() : item.signature();
            main.setText(signature);
            returnType.setText(item.returnType());
            owner.setText(item.owner());
            row.getChildren().set(0, new HBox(8, JavaFxIconManager.completionIcon(item.kind(), 16), main));
            setGraphic(row);
            setText(null);
            setPrefHeight(ROW_HEIGHT);
            setMinHeight(ROW_HEIGHT);
            setMaxHeight(ROW_HEIGHT);
        }
    }
}
