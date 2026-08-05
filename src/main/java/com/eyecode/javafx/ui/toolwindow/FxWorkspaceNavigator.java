package com.eyecode.javafx.ui.toolwindow;

import com.eyecode.designsystem.icon.EyeCodeIcon;
import com.eyecode.javafx.designsystem.JavaFxIconManager;
import com.eyecode.workbench.toolwindow.ToolWindowManager;
import com.eyecode.workbench.toolwindow.WorkspaceNavigatorItem;
import com.eyecode.workbench.toolwindow.WorkspaceNavigatorModel;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FxWorkspaceNavigator extends VBox {

    private final WorkspaceNavigatorModel model;
    private final ToolWindowManager manager;
    private final Map<String, Button> buttonsById = new HashMap<>();

    public FxWorkspaceNavigator(WorkspaceNavigatorModel model, ToolWindowManager manager) {
        getStyleClass().add("workspace-navigator");
        setSpacing(4);
        setPadding(new javafx.geometry.Insets(12, 0, 0, 0));

        this.model = model;
        this.manager = manager;

        rebuild(model.getItems());
        applySelection(model.getActiveItem());

        model.addChangeListener(() -> rebuild(model.getItems()));
        model.addSelectionListener(this::applySelection);
    }

    private void rebuild(List<WorkspaceNavigatorItem> items) {
        getChildren().clear();
        buttonsById.clear();
        for (WorkspaceNavigatorItem item : items) {
            Button b = navigatorButton(item);
            buttonsById.put(item.getId(), b);
            getChildren().add(b);
        }
        applySelection(model.getActiveItem());
    }

    private Button navigatorButton(WorkspaceNavigatorItem item) {
        Button b = new Button();
        b.setId(item.getId());
        b.getStyleClass().add("activity-btn");
        b.setGraphic(JavaFxIconManager.icon(iconFor(item.getIconKey()), 18));
        b.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        b.setTooltip(new Tooltip(item.getTooltip()));
        b.setOnAction(e -> select(item));
        return b;
    }

    private void select(WorkspaceNavigatorItem item) {
        model.select(item.getId());
        manager.activate(item.getTargetToolWindowId());
    }

    private void applySelection(WorkspaceNavigatorItem active) {
        String activeId = active != null ? active.getId() : null;
        for (Map.Entry<String, Button> entry : buttonsById.entrySet()) {
            Button b = entry.getValue();
            b.getStyleClass().remove("activity-btn-selected");
            if (entry.getKey().equals(activeId)) {
                b.getStyleClass().add("activity-btn-selected");
            }
        }
    }

    private EyeCodeIcon iconFor(String iconKey) {
        if (iconKey == null) {
            return EyeCodeIcon.PROJECT;
        }
        try {
            return EyeCodeIcon.valueOf(iconKey);
        } catch (IllegalArgumentException ignored) {
            return EyeCodeIcon.PROJECT;
        }
    }
}
