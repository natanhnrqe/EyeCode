package com.eyecode.ui.swing;

import com.eyecode.ui.core.UIPopup;

import javax.swing.JWindow;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;

public final class SwingPopup implements UIPopup {

    private final JWindow window;

    public SwingPopup() {
        this.window = new JWindow();
    }

    @Override
    public void show() {
        window.setVisible(true);
    }

    @Override
    public void hide() {
        window.setVisible(false);
    }

    @Override
    public boolean isVisible() {
        return window.isVisible();
    }

    public void setLocation(int x, int y) {
        window.setLocation(x, y);
    }

    public void setContent(Component content) {
        if (content == null) {
            window.getContentPane().removeAll();
        } else {
            window.setContentPane((Container) content);
        }
    }

    public Rectangle getBounds() {
        return window.getBounds();
    }

    public JWindow getWindow() {
        return window;
    }
}
