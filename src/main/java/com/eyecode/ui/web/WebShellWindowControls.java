package com.eyecode.ui.web;

/**
 * Native-window commands consumed only by the Web Shell window controller.
 *
 * <p>Default no-ops deliberately preserve the local Web runtime, which has no
 * native application window to control.</p>
 */
public interface WebShellWindowControls {
    default void minimizeWindow() {
    }

    default void toggleMaximizeWindow() {
    }

    default void closeWindow() {
    }

    default void beginWindowDrag(int screenX, int screenY) {
    }

    default void moveWindow(int screenX, int screenY) {
    }

    default void endWindowDrag() {
    }
}
