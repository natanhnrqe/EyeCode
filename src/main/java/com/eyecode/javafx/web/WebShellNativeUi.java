package com.eyecode.javafx.web;

import java.nio.file.Path;

public interface WebShellNativeUi {
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
            @Override public Path chooseDirectory(String title) { return null; }
            @Override public Path chooseJavaSaveTarget(String suggestedName) { return null; }
            @Override public void minimizeWindow() { }
            @Override public void toggleMaximizeWindow() { }
            @Override public void closeWindow() { }
        };
    }
}
