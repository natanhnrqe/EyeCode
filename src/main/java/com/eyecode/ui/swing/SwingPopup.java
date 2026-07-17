package com.eyecode.ui.swing;

import com.eyecode.ui.core.UIPopup;

import javax.swing.JWindow;
import java.awt.Color;
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

    @Override
    public void setLocation(int x, int y) {
        window.setLocation(x, y);
    }

    @Override
    public void setSize(int width, int height) {
        window.setSize(width, height);
    }

    @Override
    public void setContent(Component content) {
        if (content == null) {
            window.getContentPane().removeAll();
        } else {
            window.setContentPane((Container) content);
        }
    }

    @Override
    public void setBackground(Color color) {
        window.setBackground(color);
    }

    @Override
    public Rectangle getBounds() {
        return window.getBounds();
    }

    @Override
    public void revalidate() {
        window.revalidate();
    }

    @Override
    public void repaint() {
        window.repaint();
    }

    public void setFocusableWindowState(boolean focusable) {
        window.setFocusableWindowState(focusable);
    }

    public JWindow getWindow() {
        return window;
    }
}
