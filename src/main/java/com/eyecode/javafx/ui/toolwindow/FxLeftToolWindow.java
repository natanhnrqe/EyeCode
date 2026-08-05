package com.eyecode.javafx.ui.toolwindow;

import com.eyecode.javafx.designsystem.FxCard;
import com.eyecode.workbench.toolwindow.ToolWindow;
import com.eyecode.workbench.toolwindow.ToolWindowManager;
import com.eyecode.workbench.toolwindow.ToolWindowPosition;
import javafx.scene.Node;
import javafx.scene.control.Label;

import java.util.HashMap;
import java.util.Map;

public final class FxLeftToolWindow extends FxCard {

    private final ToolWindowManager manager;
    private final ToolWindowContentFactory contentFactory;
    private final Map<String, Node> cache = new HashMap<>();
    private Node current;

    public FxLeftToolWindow(ToolWindowManager manager, ToolWindowContentFactory contentFactory) {
        getStyleClass().add("left-tool-window");
        getStyleClass().remove("fx-card");
        this.manager = manager;
        this.contentFactory = contentFactory;

        ToolWindow active = manager.getActive(ToolWindowPosition.LEFT);
        show(active);

        manager.addActiveToolWindowListener(this::onActiveChanged);
    }

    private void onActiveChanged(ToolWindow active) {
        if (active == null || active.getPosition() != ToolWindowPosition.LEFT) {
            return;
        }
        show(active);
    }

    private void show(ToolWindow active) {
        Node node;
        if (active == null) {
            node = emptyPlaceholder();
        } else {
            node = cache.computeIfAbsent(active.getId(), contentFactory::createContent);
        }
        current = node;
        setContent(node);
    }

    private Node emptyPlaceholder() {
        Label label = new Label("No tool window");
        label.getStyleClass().add("toolwindow-placeholder");
        return label;
    }

    public Node getCurrentContent() {
        return current;
    }
}
