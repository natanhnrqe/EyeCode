package com.eyecode.workbench.toolwindow;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class WorkspaceNavigatorModel {

    private final List<WorkspaceNavigatorItem> items = new ArrayList<>();
    private WorkspaceNavigatorItem activeItem;
    private final List<Runnable> changeListeners = new ArrayList<>();
    private final List<Consumer<WorkspaceNavigatorItem>> selectionListeners = new ArrayList<>();

    public void setItems(List<WorkspaceNavigatorItem> items) {
        this.items.clear();
        if (items != null) {
            for (WorkspaceNavigatorItem item : items) {
                if (item != null && !this.items.contains(item)) {
                    this.items.add(item);
                }
            }
        }
        activeItem = this.items.isEmpty() ? null : this.items.get(0);
        notifyChanged();
        notifySelection();
    }

    public List<WorkspaceNavigatorItem> getItems() {
        return List.copyOf(items);
    }

    public Optional<WorkspaceNavigatorItem> findItem(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return items.stream().filter(item -> id.equals(item.getId())).findFirst();
    }

    public WorkspaceNavigatorItem getActiveItem() {
        return activeItem;
    }

    public void select(String id) {
        WorkspaceNavigatorItem item = findItem(id).orElse(null);
        if (item == null || item == activeItem) {
            return;
        }
        activeItem = item;
        notifyChanged();
        notifySelection();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    public void addSelectionListener(Consumer<WorkspaceNavigatorItem> listener) {
        if (listener != null && !selectionListeners.contains(listener)) {
            selectionListeners.add(listener);
        }
    }

    public void removeSelectionListener(Consumer<WorkspaceNavigatorItem> listener) {
        selectionListeners.remove(listener);
    }

    private void notifyChanged() {
        for (Runnable listener : List.copyOf(changeListeners)) {
            listener.run();
        }
    }

    private void notifySelection() {
        for (Consumer<WorkspaceNavigatorItem> listener : List.copyOf(selectionListeners)) {
            listener.accept(activeItem);
        }
    }
}
