package com.eyecode.javafx.ui.editor;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class FxEditorTabs extends TabPane {

    private final Map<String, Tab> tabsBySessionId = new HashMap<>();
    private final Map<String, TabModel> modelsBySessionId = new HashMap<>();
    private boolean syncing;

    private Consumer<String> onTabSelected;
    private Consumer<String> onTabCloseRequested;

    public FxEditorTabs() {
        getStyleClass().add("editor-tabs");
        setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
        setTabDragPolicy(TabDragPolicy.FIXED);

        getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (syncing || newTab == null) {
                return;
            }
            String sessionId = (String) newTab.getUserData();
            if (sessionId != null && onTabSelected != null) {
                onTabSelected.accept(sessionId);
            }
        });
    }

    public void setOnTabSelected(Consumer<String> onTabSelected) {
        this.onTabSelected = onTabSelected;
    }

    public void setOnTabCloseRequested(Consumer<String> onTabCloseRequested) {
        this.onTabCloseRequested = onTabCloseRequested;
    }

    public void update(List<TabModel> models, String activeSessionId) {
        syncing = true;
        try {
            reconcileTabs(models);
            selectActive(activeSessionId);
        } finally {
            syncing = false;
        }
    }

    private void reconcileTabs(List<TabModel> models) {
        Map<String, TabModel> incoming = new HashMap<>();
        for (TabModel model : models) {
            incoming.put(model.sessionId(), model);
        }

        for (String staleId : List.copyOf(tabsBySessionId.keySet())) {
            if (!incoming.containsKey(staleId)) {
                Tab stale = tabsBySessionId.remove(staleId);
                modelsBySessionId.remove(staleId);
                getTabs().remove(stale);
            }
        }

        for (TabModel model : models) {
            Tab tab = tabsBySessionId.get(model.sessionId());
            if (tab == null) {
                tab = new Tab();
                tab.setUserData(model.sessionId());
                tab.setOnCloseRequest(event -> {
                    event.consume();
                    if (onTabCloseRequested != null) {
                        onTabCloseRequested.accept(model.sessionId());
                    }
                });
                tabsBySessionId.put(model.sessionId(), tab);
                getTabs().add(tab);
            }
            applyModel(tab, model);
            modelsBySessionId.put(model.sessionId(), model);
        }
    }

    private void applyModel(Tab tab, TabModel model) {
        String title = model.displayName() + (model.dirty() ? " *" : "");
        if (!Objects.equals(tab.getText(), title)) {
            tab.setText(title);
        }
        boolean closable = !model.pinned();
        if (tab.isClosable() != closable) {
            tab.setClosable(closable);
        }
        if (model.preview() && !tab.getStyleClass().contains("tab-preview")) {
            tab.getStyleClass().add("tab-preview");
        } else if (!model.preview()) {
            tab.getStyleClass().remove("tab-preview");
        }
    }

    private void selectActive(String activeSessionId) {
        if (activeSessionId == null) {
            return;
        }
        Tab activeTab = tabsBySessionId.get(activeSessionId);
        if (activeTab != null && getSelectionModel().getSelectedItem() != activeTab) {
            getSelectionModel().select(activeTab);
        }
    }
}
