package com.eyecode.javafx.ui.editor;

public record TabModel(String sessionId, String displayName,
                       boolean dirty, boolean pinned, boolean preview,
                       boolean saveFailed) {

    public TabModel(String sessionId, String displayName,
                    boolean dirty, boolean pinned, boolean preview) {
        this(sessionId, displayName, dirty, pinned, preview, false);
    }

    public String getTitle() {
        return displayName;
    }
}
