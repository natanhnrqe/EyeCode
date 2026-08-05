package com.eyecode.javafx.ui.editor;

public record TabModel(String sessionId, String displayName,
                       boolean dirty, boolean pinned, boolean preview) {

    public String getTitle() {
        return displayName;
    }
}
