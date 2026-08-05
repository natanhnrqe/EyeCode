package com.eyecode.workbench.toolwindow;

import com.eyecode.eventbus.EventBus;
import com.eyecode.eventbus.events.ToolWindowActivatedEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public final class ToolWindowManager {

    private final EventBus eventBus;
    private final Map<String, ToolWindow> windowsById = new LinkedHashMap<>();
    private final List<Runnable> changeListeners = new ArrayList<>();
    private final List<Consumer<ToolWindow>> activeListeners = new ArrayList<>();
    private final List<Consumer<ToolWindowPosition>> activePositionListeners = new ArrayList<>();

    public ToolWindowManager() {
        this(null);
    }

    public ToolWindowManager(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public ToolWindow register(ToolWindow toolWindow) {
        if (toolWindow == null) {
            throw new IllegalArgumentException("register exige uma ToolWindow.");
        }
        if (windowsById.containsKey(toolWindow.getId())) {
            throw new IllegalStateException("ToolWindow já registrada: " + toolWindow.getId());
        }
        windowsById.put(toolWindow.getId(), toolWindow);
        notifyChanged();
        return toolWindow;
    }

    public ToolWindow register(String id, String title, String iconKey, ToolWindowPosition position) {
        return register(new ToolWindow(id, title, iconKey, position));
    }

    public boolean unregister(String id) {
        ToolWindow removed = windowsById.remove(id);
        if (removed == null) {
            return false;
        }
        if (removed.isActive()) {
            activateFirstAvailable(removed.getPosition());
        }
        notifyChanged();
        return true;
    }

    public void open(String id) {
        ToolWindow window = windowsById.get(id);
        if (window == null) {
            return;
        }
        window.setVisible(true);
        activate(id);
    }

    public void close(String id) {
        ToolWindow window = windowsById.get(id);
        if (window == null) {
            return;
        }
        window.setVisible(false);
        if (window.isActive()) {
            window.setActive(false);
            activateFirstAvailable(window.getPosition());
        } else {
            notifyChanged();
        }
    }

    public void activate(String id) {
        ToolWindow window = windowsById.get(id);
        if (window == null) {
            return;
        }
        ToolWindowPosition position = window.getPosition();
        for (ToolWindow other : windowsById.values()) {
            if (other.getPosition() == position && other.isActive()) {
                other.setActive(false);
            }
        }
        window.setVisible(true);
        window.setActive(true);
        notifyChanged();
        notifyActive(window);
        notifyActivePosition(position);
        if (eventBus != null) {
            eventBus.publish(new ToolWindowActivatedEvent(id, position));
        }
    }

    public Optional<ToolWindow> getToolWindow(String id) {
        return Optional.ofNullable(windowsById.get(id));
    }

    public ToolWindow getActive(ToolWindowPosition position) {
        for (ToolWindow window : windowsById.values()) {
            if (window.getPosition() == position && window.isActive()) {
                return window;
            }
        }
        return null;
    }

    public List<ToolWindow> getToolWindows(ToolWindowPosition position) {
        List<ToolWindow> result = new ArrayList<>();
        for (ToolWindow window : windowsById.values()) {
            if (window.getPosition() == position) {
                result.add(window);
            }
        }
        return List.copyOf(result);
    }

    public List<ToolWindow> getAll() {
        return List.copyOf(windowsById.values());
    }

    public void swapContent(String outgoingId, String incomingId) {
        ToolWindow outgoing = windowsById.get(outgoingId);
        ToolWindow incoming = windowsById.get(incomingId);
        if (outgoing == null || incoming == null) {
            return;
        }
        if (outgoing.getPosition() != incoming.getPosition()) {
            throw new IllegalStateException(
                    "swapContent exige ToolWindows da mesma posição.");
        }
        outgoing.setVisible(false);
        outgoing.setActive(false);
        activate(incomingId);
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    public void addActiveToolWindowListener(Consumer<ToolWindow> listener) {
        if (listener != null && !activeListeners.contains(listener)) {
            activeListeners.add(listener);
        }
    }

    public void removeActiveToolWindowListener(Consumer<ToolWindow> listener) {
        activeListeners.remove(listener);
    }

    public void addActivePositionListener(Consumer<ToolWindowPosition> listener) {
        if (listener != null && !activePositionListeners.contains(listener)) {
            activePositionListeners.add(listener);
        }
    }

    public void removeActivePositionListener(Consumer<ToolWindowPosition> listener) {
        activePositionListeners.remove(listener);
    }

    private void activateFirstAvailable(ToolWindowPosition position) {
        for (ToolWindow window : windowsById.values()) {
            if (window.getPosition() == position && window.isVisible()) {
                activate(window.getId());
                return;
            }
        }
        notifyActive(null);
        notifyActivePosition(position);
    }

    private void notifyChanged() {
        for (Runnable listener : List.copyOf(changeListeners)) {
            listener.run();
        }
    }

    private void notifyActive(ToolWindow window) {
        for (Consumer<ToolWindow> listener : List.copyOf(activeListeners)) {
            listener.accept(window);
        }
    }

    private void notifyActivePosition(ToolWindowPosition position) {
        for (Consumer<ToolWindowPosition> listener : List.copyOf(activePositionListeners)) {
            listener.accept(position);
        }
    }
}
