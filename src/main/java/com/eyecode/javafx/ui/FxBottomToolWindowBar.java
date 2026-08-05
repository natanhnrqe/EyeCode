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

    public FxBottomToolWindowBar(ToolWindowManager manager) {
        getStyleClass().add("bottom-tool-window-bar");
        setPrefHeight(FxSpacing.BOTTOM_BAR_HEIGHT);
        setMinHeight(FxSpacing.BOTTOM_BAR_HEIGHT);
        setPadding(new Insets(
                FxSpacing.BOTTOM_BAR_BTN_PAD_V,
                FxSpacing.BOTTOM_BAR_SIDE,
                FxSpacing.BOTTOM_BAR_BTN_PAD_V,
                FxSpacing.BOTTOM_BAR_SIDE));
        setSpacing(FxSpacing.XXS);

        this.manager = manager;
        this.breadcrumbs = defaultBreadcrumbs();
        this.statusGroup = defaultStatusGroup();

        rebuild(manager.getToolWindows(ToolWindowPosition.BOTTOM));
        applySelection(manager.getActive(ToolWindowPosition.BOTTOM));

        manager.addChangeListener(() ->
                rebuild(manager.getToolWindows(ToolWindowPosition.BOTTOM)));
        manager.addActiveToolWindowListener(this::onActiveChanged);
    }

    private void onActiveChanged(ToolWindow active) {
        if (active == null || active.getPosition() != ToolWindowPosition.BOTTOM) {
            applySelection(null);
            return;
        }
        applySelection(active);
    }

    private void rebuild(List<ToolWindow> windows) {
        getChildren().clear();
        buttonsById.clear();
        for (ToolWindow window : windows) {
            Button b = tabButton(window);
            buttonsById.put(window.getId(), b);
            getChildren().add(b);
        }
        getChildren().add(barSeparator());
        getChildren().add(breadcrumbs);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        getChildren().add(spacer);
        getChildren().add(statusGroup);
        applySelection(manager.getActive(ToolWindowPosition.BOTTOM));
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
        group.addItem(new FxStatusItem("Ln 15, Col 8"));
        return group;
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