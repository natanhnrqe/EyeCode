package com.eyecode.javafx.editor;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;
import com.eyecode.editor.v2.completion.CompletionSnapshot;
import com.eyecode.javafx.designsystem.JavaFxIconManager;
import com.eyecode.ui.designsystem.ColorManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Window;
import org.fxmisc.richtext.CodeArea;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class JavaFxCompletionPopup {

    private static final double POPUP_WIDTH = 600;
    private static final int PAGE_SELECTION_STEP = 10;
    private static final double LIST_VIEWPORT_HEIGHT = 240;
    private static final double LIST_CELL_HEIGHT = 44;
    private static final double DETAIL_MAX_HEIGHT = 220;
    private static final double SCREEN_MARGIN = 8;
    private static final double CARET_X_OFFSET = 8;
    private static final double POPUP_GAP = 0;

    private final PopupControl popup;
    private final ObservableList<CompletionItem> items;
    private final ListView<CompletionItem> listView;
    private final StackPane root;
    private final VBox content;
    private final StackPane listShell;
    private final Separator separator;
    private final ScrollPane detailScroll;
    private final VBox detailContent;
    private final Label signatureLabel;
    private final Label returnTypeLabel;
    private final Label documentationLabel;
    private final Label exampleTitleLabel;
    private final Label exampleLabel;
    private final Label ownerTitleLabel;
    private final Label ownerLabel;
    private Consumer<CompletionItem> onAccept;
    private String selectedLabel;
    private int caretOffset;
    private boolean testShowing;
    private PopupLayout lastLayout;

    public JavaFxCompletionPopup() {
        popup = new PopupControl();
        popup.setAutoHide(false);
        popup.setAutoFix(false);
        popup.setHideOnEscape(false);
        installStylesheet(popup.getScene());

        items = FXCollections.observableArrayList();
        listView = new ListView<>(items);
        listView.getStyleClass().add("completion-list");
        listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        listView.setFocusTraversable(false);
        listView.setFixedCellSize(LIST_CELL_HEIGHT);
        listView.setCellFactory(ignored -> new CompletionCell());
        listView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                acceptSelected();
            }
        });
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            selectedLabel = newItem != null ? newItem.getLabel() : null;
            updateDocumentation(newItem);
        });

        listShell = new StackPane(listView);
        listShell.getStyleClass().add("completion-list-shell");
        listShell.setMinHeight(LIST_VIEWPORT_HEIGHT);
        listShell.setPrefHeight(LIST_VIEWPORT_HEIGHT);
        listShell.setMaxHeight(LIST_VIEWPORT_HEIGHT);

        VBox.setVgrow(listShell, Priority.NEVER);

        separator = new Separator();
        separator.getStyleClass().add("completion-separator");

        signatureLabel = label("completion-title", Font.font("Segoe UI", FontWeight.BOLD, 13));
        returnTypeLabel = label("completion-return", Font.font("Segoe UI", 12));
        documentationLabel = label("completion-doc-text", Font.font("Segoe UI", 12));
        exampleTitleLabel = label("completion-example-title", Font.font("Segoe UI", FontWeight.BOLD, 12));
        exampleTitleLabel.setText("Example");
        exampleLabel = label("completion-example-code", Font.font("Consolas", 12));
        exampleLabel.setWrapText(true);
        ownerTitleLabel = label("completion-owner-title", Font.font("Segoe UI", FontWeight.BOLD, 11));
        ownerTitleLabel.setText("Owner");
        ownerLabel = label("completion-owner", Font.font("Segoe UI", 11));

        detailContent = new VBox(8,
                signatureLabel,
                returnTypeLabel,
                documentationLabel,
                exampleTitleLabel,
                exampleLabel,
                ownerTitleLabel,
                ownerLabel
        );
        detailContent.getStyleClass().add("completion-doc-pane");
        detailContent.setMinWidth(0);
        detailContent.setMaxWidth(Double.MAX_VALUE);

        detailScroll = new ScrollPane(detailContent);
        detailScroll.getStyleClass().add("completion-doc-scroll");
        detailScroll.setFitToWidth(true);
        detailScroll.setFocusTraversable(false);
        detailScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);


        content = new VBox(listShell, separator, detailContent);
        content.getStyleClass().add("completion-popup-content");

        root = new StackPane(content);
        root.getStyleClass().add("completion-popup");
        root.setMinWidth(POPUP_WIDTH);
        root.setPrefWidth(POPUP_WIDTH);
        root.setMaxWidth(POPUP_WIDTH);

        popup.getScene().setFill(Color.TRANSPARENT);
        popup.getScene().setRoot(root);

        lastLayout = new PopupLayout(0, 0, POPUP_WIDTH, 0, false);
        updateItems(CompletionSnapshot.empty());
        updateDocumentation(null);
    }

    public void show(CodeArea codeArea, CompletionSnapshot snapshot, int caretOffset) {
        updateItems(snapshot);
        if (items.isEmpty()) {
            hide();
            return;
        }
        this.caretOffset = caretOffset;
        applySelection();
        position(codeArea);
    }

    public void update(CodeArea codeArea, CompletionSnapshot snapshot, int caretOffset) {
        updateItems(snapshot);
        if (items.isEmpty()) {
            hide();
            return;
        }
        this.caretOffset = caretOffset;
        applySelection();
        position(codeArea);
    }

    private int actualVisibleRowCount() {
    return Math.max(
            1,
            (int) Math.floor(
                    (LIST_VIEWPORT_HEIGHT - 16) / LIST_CELL_HEIGHT
            )
    );
}

    public void move(CodeArea codeArea, int caretOffset) {
        if (!popup.isShowing()) {
            return;
        }
        this.caretOffset = caretOffset;
        position(codeArea);
    }

    public void hide() {
        selectedLabel = null;
        testShowing = false;
        popup.hide();
    }

    public boolean isShowing() {
        return popup.isShowing() || testShowing;
    }

    public void selectNext() {
        moveSelection(1);
    }

    public void selectPrevious() {
        moveSelection(-1);
    }

    public void selectPageDown() {
        moveSelection(PAGE_SELECTION_STEP);
    }

    public void selectPageUp() {
        moveSelection(-PAGE_SELECTION_STEP);
    }

    public void selectFirst() {
        moveTo(0);
    }

    public void selectLast() {
        moveTo(items.size() - 1);
    }

    public void acceptSelected() {
        CompletionItem item = getSelectedItem();
        if (item != null && onAccept != null) {
            onAccept.accept(item);
        }
    }

    public CompletionItem getSelectedItem() {
        return listView.getSelectionModel().getSelectedItem();
    }

    public void setOnAccept(Consumer<CompletionItem> onAccept) {
        this.onAccept = onAccept;
    }

    PopupLayout lastLayout() {
        return lastLayout;
    }

    int visibleRowCount() {
        return Math.min(PAGE_SELECTION_STEP, items.size());
    }

    double boundedPopupWidth() {
        return root.getPrefWidth();
    }

    double boundedPopupHeight() {
        root.applyCss();
        root.layout();
        return popupHeight();

    }

    String detailText() {
        return documentationLabel.isVisible() ? documentationLabel.getText() : "";
    }

    String exampleText() {
        return exampleLabel.isVisible() ? exampleLabel.getText() : "";
    }

    String signatureText() {
        return signatureLabel.isVisible() ? signatureLabel.getText() : "";
    }

    boolean hasDetailPane() {
        return detailScroll.isManaged();
    }

    boolean hasSuggestionList() {
        return listView != null;
    }

    int selectedIndex() {
        return listView.getSelectionModel().getSelectedIndex();
    }

    CompletionRowGraphic rowGraphicFor(CompletionItem item, boolean selected, boolean hovered) {
        CompletionRowGraphic graphic = new CompletionRowGraphic();
        graphic.update(item, selected, hovered);
        return graphic;
    }

    void applySnapshotForTest(CompletionSnapshot snapshot) {
        updateItems(snapshot);
        applySelection();
    }

    void showForTest(CompletionSnapshot snapshot) {
        updateItems(snapshot);
        applySelection();
        testShowing = !items.isEmpty();
    }

    static PopupLayout layoutFor(Bounds caretBounds, Rectangle2D screenBounds, double popupWidth, double popupHeight) {
        double minX = screenBounds.getMinX() + SCREEN_MARGIN;
        double maxX = screenBounds.getMaxX() - popupWidth - SCREEN_MARGIN;
        if (maxX < minX) {
            maxX = minX;
        }
        double x = clamp(caretBounds.getMinX() - CARET_X_OFFSET, minX, maxX);

        double belowY = caretBounds.getMaxY() + POPUP_GAP;
        double aboveY = caretBounds.getMinY() - popupHeight - POPUP_GAP;
        boolean placeAbove = belowY + popupHeight > screenBounds.getMaxY() - SCREEN_MARGIN
                && aboveY >= screenBounds.getMinY() + SCREEN_MARGIN;

        double minY = screenBounds.getMinY() + SCREEN_MARGIN;
        double maxY = screenBounds.getMaxY() - popupHeight - SCREEN_MARGIN;
        if (maxY < minY) {
            maxY = minY;
        }
        double y = clamp(placeAbove ? aboveY : belowY, minY, maxY);
        return new PopupLayout(x, y, popupWidth, popupHeight, placeAbove);
    }

    private void moveSelection(int delta) {
        if (items.isEmpty()) {
            return;
        }
        moveTo(listView.getSelectionModel().getSelectedIndex() + delta);
    }

    private void moveTo(int index) {
        if (items.isEmpty()) {
            return;
        }
        int clamped = Math.max(0, Math.min(index, items.size() - 1));
        listView.getSelectionModel().select(clamped);
        centerSelection(clamped);
    }

    private void centerSelection(int index) {
        int visibleRows = Math.max(
                1,
                Math.min(actualVisibleRowCount(), items.size())
        );

        int target = Math.max(
                0,
                Math.min(
                        index - (visibleRows / 2),
                        Math.max(0, items.size() - visibleRows)
                )
        );

        listView.scrollTo(target);
    }

    private void updateItems(CompletionSnapshot snapshot) {
        List<CompletionItem> nextItems = snapshot == null ? List.of() : snapshot.getItems();
        items.setAll(nextItems);

    }



    private void applySelection() {
        if (items.isEmpty()) {
            listView.getSelectionModel().clearSelection();
            updateDocumentation(null);
            return;
        }
        int index = 0;
        if (selectedLabel != null) {
            for (int i = 0; i < items.size(); i++) {
                if (Objects.equals(selectedLabel, items.get(i).getLabel())) {
                    index = i;
                    break;
                }
            }
        }
        listView.getSelectionModel().select(index);
        centerSelection(index);
        updateDocumentation(listView.getSelectionModel().getSelectedItem());
    }

    private void updateDocumentation(CompletionItem item) {
        updateLabel(signatureLabel, firstNonBlank(item == null ? null : item.getSignature(), item == null ? null : item.getDetail()));
        updateLabel(returnTypeLabel, item != null && notBlank(item.getReturnType()) ? "Returns: " + item.getReturnType() : "");
        updateLabel(documentationLabel, item == null ? "" : item.getDocumentation());

        boolean hasExample = item != null && notBlank(item.getExample());
        exampleTitleLabel.setVisible(hasExample);
        exampleTitleLabel.setManaged(hasExample);
        updateLabel(exampleLabel, item == null ? "" : item.getExample());

        boolean hasOwner = item != null && notBlank(item.getOwner());
        ownerTitleLabel.setVisible(hasOwner);
        ownerTitleLabel.setManaged(hasOwner);
        updateLabel(ownerLabel, item == null ? "" : item.getOwner());

        boolean hasDetail = signatureLabel.isVisible()
                || returnTypeLabel.isVisible()
                || documentationLabel.isVisible()
                || exampleLabel.isVisible()
                || ownerLabel.isVisible();
        separator.setVisible(hasDetail);
        separator.setManaged(hasDetail);
        detailScroll.setVisible(hasDetail);
        detailScroll.setManaged(hasDetail);
        detailScroll.setVvalue(0);
        root.applyCss();
        root.layout();
        double preferredHeight = hasDetail
                ? Math.min(DETAIL_MAX_HEIGHT, detailContent.prefHeight(POPUP_WIDTH - 28))
                : 0;
        detailContent.setPrefHeight(preferredHeight);
        detailContent.setMaxHeight(DETAIL_MAX_HEIGHT);
        detailContent.setVisible(hasDetail);
        detailContent.setManaged(hasDetail);
    }

    private void updateLabel(Label label, String text) {
        boolean visible = notBlank(text);
        label.setText(visible ? text : "");
        label.setVisible(visible);
        label.setManaged(visible);
    }

    private void position(CodeArea codeArea) {
        Window window = ownerWindow(codeArea);
        if (window == null) {
            return;
        }
        Bounds caretBounds = caretScreenBounds(codeArea);
        if (caretBounds == null) {
            return;
        }
        PopupLayout layout = layoutFor(caretBounds, screenBoundsFor(caretBounds), boundedPopupWidth(), popupHeight());
        lastLayout = layout;
        if (popup.isShowing()) {
            popup.setAnchorX(layout.x());
            popup.setAnchorY(layout.y());
        } else {
            popup.show(window, layout.x(), layout.y());
        }
    }

    private double popupHeight() {
        root.applyCss();
        root.layout();
        return Math.max(root.minHeight(POPUP_WIDTH), root.prefHeight(POPUP_WIDTH));
    }

    private Bounds caretScreenBounds(CodeArea codeArea) {
    Bounds caretScreen = codeArea.getCaretBounds().orElse(null);
    if (caretScreen != null) {
        return caretScreen;
    }

    Bounds editorBounds = codeArea.localToScreen(codeArea.getBoundsInLocal());
    if (editorBounds == null) {
        return null;
    }

    return new javafx.geometry.BoundingBox(
            editorBounds.getMinX(),
            editorBounds.getMinY(),
            1,
            18
    );
}

    private Rectangle2D screenBoundsFor(Bounds anchor) {
        List<Screen> screens = Screen.getScreensForRectangle(
                anchor.getMinX(),
                anchor.getMinY(),
                Math.max(1, anchor.getWidth()),
                Math.max(1, anchor.getHeight())
        );
        Screen screen = screens.isEmpty() ? Screen.getPrimary() : screens.getFirst();
        return screen.getVisualBounds();
    }

    private Window ownerWindow(CodeArea codeArea) {
        Scene scene = codeArea.getScene();
        return scene != null ? scene.getWindow() : null;
    }

    private static Label label(String styleClass, Font font) {
        Label label = new Label();
        label.getStyleClass().add(styleClass);
        label.setFont(font);
        label.setTextFill(Color.WHITE);
        label.setWrapText(true);
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMinWidth(0);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setVisible(false);
        label.setManaged(false);
        return label;
    }

    private static void installStylesheet(Scene scene) {
        String stylesheet = JavaFxCompletionPopup.class
                .getResource("/javafx/style/eyecode.css")
                .toExternalForm();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (notBlank(primary)) {
            return primary;
        }
        return notBlank(fallback) ? fallback : "";
    }

    private static boolean notBlank(String text) {
        return text != null && !text.isBlank();
    }

    private static Color fx(java.awt.Color color) {
        return Color.rgb(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 255.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    static final class PopupLayout {
        private final double x;
        private final double y;
        private final double width;
        private final double height;
        private final boolean aboveCaret;

        PopupLayout(double x, double y, double width, double height, boolean aboveCaret) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.aboveCaret = aboveCaret;
        }

        double x() {
            return x;
        }

        double y() {
            return y;
        }

        double width() {
            return width;
        }

        double height() {
            return height;
        }

        boolean aboveCaret() {
            return aboveCaret;
        }
    }

    static final class CompletionRowGraphic extends StackPane {

        private final Label iconLabel;
        private final Label nameLabel;
        private final Label signatureLabel;
        private final Label returnTypeLabel;
        private final Label ownerLabel;

        CompletionRowGraphic() {
            getStyleClass().add("completion-row-shell");

            iconLabel = new Label();
            iconLabel.getStyleClass().add("completion-icon");
            iconLabel.setMinWidth(20);
            iconLabel.setPrefWidth(20);
            iconLabel.setMaxWidth(20);
            iconLabel.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

            nameLabel = new Label();
            nameLabel.setMinWidth(0);
            nameLabel.setMaxWidth(Double.MAX_VALUE);
            nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
            nameLabel.getStyleClass().add("completion-primary");
            nameLabel.setFont(Font.font("Consolas", 12));

            signatureLabel = new Label();
            signatureLabel.setMinWidth(0);
            signatureLabel.setMaxWidth(Double.MAX_VALUE);
            signatureLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
            HBox.setHgrow(signatureLabel, Priority.ALWAYS);
            signatureLabel.getStyleClass().add("completion-secondary");
            signatureLabel.setFont(Font.font("Consolas", 11));

            HBox titleRow = new HBox(6, nameLabel, signatureLabel);
            titleRow.setMinWidth(0);
            titleRow.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(titleRow, Priority.ALWAYS);
            titleRow.setFillHeight(true);

            ownerLabel = new Label();
            ownerLabel.setMinWidth(0);
            ownerLabel.setMaxWidth(Double.MAX_VALUE);
            ownerLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
            ownerLabel.getStyleClass().add("completion-origin");
            ownerLabel.setFont(Font.font("Segoe UI", 11));

            VBox textPanel = new VBox(2, titleRow, ownerLabel);
            textPanel.setMinWidth(0);
            textPanel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(textPanel, Priority.ALWAYS);


            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            returnTypeLabel = new Label();
            returnTypeLabel.getStyleClass().add("completion-return-type");
            returnTypeLabel.setFont(Font.font("Segoe UI", 11));
            returnTypeLabel.setMinWidth(55);
            returnTypeLabel.setPrefWidth(70);
            returnTypeLabel.setMaxWidth(85);
            returnTypeLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

            HBox row = new HBox(4, iconLabel, textPanel, spacer, returnTypeLabel);
            row.getStyleClass().add("completion-row");
            getChildren().add(row);
        }

        void update(CompletionItem item, boolean selected, boolean hovered) {
            iconLabel.setGraphic(JavaFxIconManager.completionIcon(item.getKind(), 16));
            nameLabel.setText(item.getLabel());
            updateOptionalLabel(signatureLabel, firstNonBlank(item.getSignature(), item.getDetail()));
            updateOptionalLabel(ownerLabel, item.getOwner());
            updateOptionalLabel(returnTypeLabel, item.getReturnType());

            nameLabel.setTextFill(selected ? fx(ColorManager.TEXT_PRIMARY) : fx(ColorManager.AUTOCOMPLETE_FG));
            signatureLabel.setTextFill(selected ? fx(ColorManager.TEXT_SECONDARY) : fx(ColorManager.TEXT_TERTIARY));
            ownerLabel.setTextFill(selected ? fx(ColorManager.TEXT_TERTIARY) : fx(ColorManager.TEXT_MUTED));
            returnTypeLabel.setTextFill(fx(ColorManager.SYNTAX_TYPE));

            setBackground(new Background(new BackgroundFill(
                    backgroundFor(selected, hovered),
                    new CornerRadii(8),
                    Insets.EMPTY
            )));
        }

        boolean hasIcon() {
            return iconLabel.getGraphic() != null;
        }

        String primaryText() {
            return nameLabel.getText();
        }

        String secondaryText() {
            return signatureLabel.isVisible() ? signatureLabel.getText() : "";
        }

        String ownerText() {
            return ownerLabel.isVisible() ? ownerLabel.getText() : "";
        }

        String returnTypeText() {
            return returnTypeLabel.isVisible() ? returnTypeLabel.getText() : "";
        }

        private void updateOptionalLabel(Label label, String text) {
            boolean visible = notBlank(text);
            label.setText(visible ? text : "");
            label.setVisible(visible);
            label.setManaged(visible);
        }

        private Color backgroundFor(boolean selected, boolean hovered) {
            if (selected) {
                return fx(ColorManager.AUTOCOMPLETE_SELECTION_BG);
            }
            if (hovered) {
                return fx(ColorManager.ACCENT_HOVER_BG);
            }
            return fx(ColorManager.AUTOCOMPLETE_BG);
        }
    }

    private static final class CompletionCell extends ListCell<CompletionItem> {

        private final CompletionRowGraphic graphic = new CompletionRowGraphic();

        private CompletionCell() {
            setMaxWidth(Double.MAX_VALUE);

            graphic.setMinWidth(0);
            graphic.setMaxWidth(Double.MAX_VALUE);

            graphic.prefWidthProperty().bind(
                    widthProperty().subtract(4)
            );

            selectedProperty().addListener(
                    (obs, oldValue, newValue) -> refresh()
            );

            hoverProperty().addListener(
                    (obs, oldValue, newValue) -> refresh()
            );
        }

        @Override
        protected void updateItem(CompletionItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            graphic.update(item, isSelected(), isHover());
            setText(null);
            setGraphic(graphic);
            setBackground(Background.EMPTY);
        }

        private void refresh() {
            CompletionItem item = getItem();
            if (item != null && !isEmpty()) {
                graphic.update(item, isSelected(), isHover());
            }
        }
    }
}
