package com.eyecode.ui.web;

import java.nio.file.Path;

/**
 * Narrow native-window capability used by Web Shell handlers. Each toolkit
 * adapts its own dialogs and window controls here; unavailable environments use
 * the provided no-op implementation instead of leaking toolkit checks into
 * protocol controllers.
 */
public interface WebShellNativeUi {
    default boolean isAvailable() {
        return false;
    }

    Path chooseDirectory(String title);

    Path chooseJavaSaveTarget(String suggestedName);

    void minimizeWindow();

    void toggleMaximizeWindow();

    void closeWindow();

    default void beginWindowDrag(int screenX, int screenY) { }

    default void moveWindow(int screenX, int screenY) { }

    default void endWindowDrag() { }

    static WebShellNativeUi unavailable() {
        return new WebShellNativeUi() {
            @Override public boolean isAvailable() { return false; }
            @Override public Path chooseDirectory(String title) { return null; }
            @Override public Path chooseJavaSaveTarget(String suggestedName) { return null; }
            @Override public void minimizeWindow() { }
            @Override public void toggleMaximizeWindow() { }
            @Override public void closeWindow() { }
        };
    }
}
