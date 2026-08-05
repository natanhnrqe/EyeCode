package com.eyecode.workbench.editor;

import com.eyecode.editor.v2.EditorPosition;

public record OpenOptions(boolean preview, boolean pinned,
                          EditorPosition restoreCaret, EditorScroll restoreScroll) {

    public static OpenOptions standard() {
        return new OpenOptions(false, false, null, null);
    }

    public static OpenOptions previewOptions() {
        return new OpenOptions(true, false, null, null);
    }

    public static OpenOptions pinnedOptions() {
        return new OpenOptions(false, true, null, null);
    }
}
