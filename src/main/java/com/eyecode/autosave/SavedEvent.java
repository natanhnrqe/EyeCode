package com.eyecode.autosave;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Immutable notification fired by {@link AutoSaveManager} after a save attempt.
 */
public record SavedEvent(Path path, boolean success, IOException error) {

    public boolean succeeded() {
        return success;
    }
}
