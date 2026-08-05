package com.eyecode.workbench.toolwindow;

public final class ToolWindow {

    private final String id;
    private final String title;
    private final String iconKey;
    private final ToolWindowPosition position;
    private boolean visible;
    private boolean active;

    public ToolWindow(String id, String title, String iconKey, ToolWindowPosition position) {
        this(id, title, iconKey, position, false, false);
    }

    ToolWindow(String id, String title, String iconKey, ToolWindowPosition position,
               boolean visible, boolean active) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ToolWindow exige um id.");
        }
        if (position == null) {
            throw new IllegalArgumentException("ToolWindow exige uma posição.");
        }
        this.id = id;
        this.title = title == null ? id : title;
        this.iconKey = iconKey;
        this.position = position;
        this.visible = visible;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getIconKey() {
        return iconKey;
    }

    public ToolWindowPosition getPosition() {
        return position;
    }

    public boolean isVisible() {
        return visible;
    }

    void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isActive() {
        return active;
    }

    void setActive(boolean active) {
        this.active = active;
    }
}
