package com.eyecode.workbench.toolwindow;

public final class WorkspaceNavigatorItem {

    private final String id;
    private final String iconKey;
    private final String title;
    private final String tooltip;
    private final String targetToolWindowId;

    public WorkspaceNavigatorItem(String id, String iconKey, String title,
                                  String tooltip, String targetToolWindowId) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("WorkspaceNavigatorItem exige um id.");
        }
        if (targetToolWindowId == null || targetToolWindowId.isBlank()) {
            throw new IllegalArgumentException(
                    "WorkspaceNavigatorItem exige um ToolWindow alvo.");
        }
        this.id = id;
        this.iconKey = iconKey;
        this.title = title == null ? id : title;
        this.tooltip = tooltip == null ? title : tooltip;
        this.targetToolWindowId = targetToolWindowId;
    }

    public String getId() {
        return id;
    }

    public String getIconKey() {
        return iconKey;
    }

    public String getTitle() {
        return title;
    }

    public String getTooltip() {
        return tooltip;
    }

    public String getTargetToolWindowId() {
        return targetToolWindowId;
    }
}
