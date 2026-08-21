package com.eyecode.javafx.ui.editor;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Region;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class FxEditorTabs extends TabPane {

    private final Map<String, Tab> tabsBySessionId = new HashMap<>();
    private final Map<String, TabModel> modelsBySessionId = new HashMap<>();
    private boolean syncing;
    private boolean documentationSelected;
    private boolean sourceSelected;
    private Tab documentationTab;
    private final Map<String, Tab> sourceTabs = new HashMap<>();

    private Consumer<String> onTabSelected;
    private Consumer<String> onTabCloseRequested;

    public FxEditorTabs() {
        getStyleClass().add("editor-tabs");
        setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
        setTabDragPolicy(TabDragPolicy.FIXED);
        setMinHeight(Region.USE_PREF_SIZE);
        setMaxHeight(Region.USE_PREF_SIZE);

        getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (syncing || newTab == null) {
                return;
            }
            String sessionId = (String) newTab.getUserData();
            if (JavaFxDocumentationWorkspace.TAB_ID.equals(sessionId)) {
                documentationSelected = true;
                sourceSelected = false;
                if (onTabSelected != null) {
                    onTabSelected.accept(sessionId);
                }
                return;
            }
            documentationSelected = false;
            sourceSelected = sessionId != null && sessionId.startsWith("jdk-source:");
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

    public void addDocumentationTab(JavaFxDocumentationTab content, Runnable closeAction) {
        if (documentationTab != null) {
            return;
        }
        documentationTab = createTab(JavaFxDocumentationWorkspace.TAB_ID,
                "Documentation", closeAction);
        documentationTab.setClosable(true);
        getTabs().add(documentationTab);
    }

    public void showDocumentation() {
        if (documentationTab != null) {
            documentationSelected = true;
            getSelectionModel().select(documentationTab);
        }
    }

    public void removeDocumentation() {
        if (documentationTab != null) {
            getTabs().remove(documentationTab);
            documentationTab = null;
            documentationSelected = false;
        }
    }

    public void addSourceTab(String id, String title, Runnable closeAction) {
        if (sourceTabs.containsKey(id)) {
            return;
        }
        Tab tab = createTab(id, title, closeAction);
        sourceTabs.put(id, tab);
        getTabs().add(tab);
    }

    public void showSource(String id) {
        Tab tab = sourceTabs.get(id);
        if (tab != null) {
            sourceSelected = true;
            getSelectionModel().select(tab);
        }
    }

    public void removeSource(String id) {
        Tab tab = sourceTabs.remove(id);
        if (tab != null) {
            getTabs().remove(tab);
            sourceSelected = false;
        }
    }

    public void update(List<TabModel> models, String activeSessionId) {
        syncing = true;
        try {
            reconcileTabs(models);
            if (!documentationSelected && !sourceSelected) {
                selectActive(activeSessionId);
            }
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
                tab = createTab(model.sessionId(), model.displayName(), () -> {
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

    private Tab createTab(String id, String title, Runnable closeAction) {
        Tab tab = new Tab(title);
        tab.setUserData(id);
        tab.setOnCloseRequest(event -> {
            event.consume();
            if (closeAction != null) {
                closeAction.run();
            }
        });
        return tab;
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
