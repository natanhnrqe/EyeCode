package com.eyecode.javafx.ui;

import com.eyecode.designsystem.icon.EyeCodeIcon;
import com.eyecode.javafx.designsystem.FxBreadcrumbs;
import com.eyecode.javafx.designsystem.FxSpacing;
import com.eyecode.javafx.designsystem.FxStatusBarGroup;
import com.eyecode.javafx.designsystem.FxStatusItem;
import com.eyecode.javafx.designsystem.JavaFxIconManager;
import com.eyecode.workbench.toolwindow.ToolWindow;
import com.eyecode.workbench.toolwindow.ToolWindowManager;
import com.eyecode.workbench.toolwindow.ToolWindowPosition;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FxBottomToolWindowBar extends HBox {

    private final ToolWindowManager manager;
    private final Map<String, Button> buttonsById = new HashMap<>();
    private final FxBreadcrumbs breadcrumbs;
    private final FxStatusBarGroup statusGroup;
    private FxStatusItem positionItem;

    public FxBottomToolWindowBar(ToolWindowManager manager) {
        getStyleClass().add("bottom-tool-window-bar");

        setPrefHeight(FxSpacing.BOTTOM_BAR_HEIGHT);
        setMinHeight(FxSpacing.BOTTOM_BAR_HEIGHT);
        setMaxHeight(FxSpacing.BOTTOM_BAR_HEIGHT);

        this.manager = manager;
        this.breadcrumbs = defaultBreadcrumbs();
        this.statusGroup = defaultStatusGroup();

        rebuild(manager.getToolWindows(ToolWindowPosition.BOTTOM));
        applySelection(manager.getActive(ToolWindowPosition.BOTTOM));

        manager.addChangeListener(() ->
                rebuild(manager.getToolWindows(ToolWindowPosition.BOTTOM)));

        manager.addActiveToolWindowListener(this::onActiveChanged);
    }

    private void rebuild(List<ToolWindow> windows) {
        getChildren().clear();
        buttonsById.clear();

        HBox tools = new HBox();
        tools.getStyleClass().add("bottom-tools");

        for (ToolWindow window : windows) {
            Button button = tabButton(window);
            buttonsById.put(window.getId(), button);
            tools.getChildren().add(button);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(
                tools,
                barSeparator(),
                breadcrumbs,
                spacer,
                statusGroup
        );

        applySelection(manager.getActive(ToolWindowPosition.BOTTOM));
    }

    private void onActiveChanged(ToolWindow active) {
        if (active == null || active.getPosition() != ToolWindowPosition.BOTTOM) {
            applySelection(null);
            return;
        }
        applySelection(active);
    }

    private Button tabButton(ToolWindow window) {
        Button b = new Button();
        b.getStyleClass().add("bottom-tw-btn");
        b.setTooltip(new Tooltip(window.getTitle()));
        b.setOnAction(e -> manager.activate(window.getId()));
        if (window.getIconKey() != null) {
            b.setGraphic(JavaFxIconManager.icon(iconFor(window.getIconKey()), 14));
            b.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        } else {
            b.setText(window.getTitle());
            b.setContentDisplay(javafx.scene.control.ContentDisplay.TEXT_ONLY);
        }
        return b;
    }

    private void applySelection(ToolWindow active) {
        String activeId = active != null && active.getPosition() == ToolWindowPosition.BOTTOM
                ? active.getId() : null;
        for (Map.Entry<String, Button> entry : buttonsById.entrySet()) {
            Button b = entry.getValue();
            b.getStyleClass().remove("bottom-tw-btn-selected");
            if (entry.getKey().equals(activeId)) {
                b.getStyleClass().add("bottom-tw-btn-selected");
            }
        }
    }

    private FxBreadcrumbs defaultBreadcrumbs() {
        return new FxBreadcrumbs(List.of(
                "src", "active", "java", "controller", "UserController.java"));
    }

    private FxStatusBarGroup defaultStatusGroup() {
        FxStatusBarGroup group = new FxStatusBarGroup();
        group.addItem(new FxStatusItem("Java 21"));
        group.addItem(new FxStatusItem("UTF-8"));
        group.addItem(new FxStatusItem("LF"));
        group.addItem(new FxStatusItem("Spaces: 4"));
        positionItem = new FxStatusItem("Ln 1, Col 1");
        group.addItem(positionItem);
        return group;
    }

    public void updateCaretPosition(int line, int column) {
        Runnable update = () -> positionItem.setText("Ln " + Math.max(1, line)
                + ", Col " + Math.max(1, column));
        if (javafx.application.Platform.isFxApplicationThread()) update.run();
        else {
            try {
                javafx.application.Platform.runLater(update);
            } catch (IllegalStateException ignored) {
            }
        }
    }

    private Region barSeparator() {
        Region s = new Region();
        s.getStyleClass().add("bar-sep");
        return s;
    }

    private EyeCodeIcon iconFor(String iconKey) {
        if (iconKey == null) {
            return EyeCodeIcon.TERMINAL;
        }
        try {
            return EyeCodeIcon.valueOf(iconKey);
        } catch (IllegalArgumentException ignored) {
            return EyeCodeIcon.TERMINAL;
        }
    }
}
