package com.eyecode.ui.swing;

import com.eyecode.ui.core.UIPopup;

import com.eyecode.learning.ui.HoverDiagnosticLogger;

import javax.swing.JDialog;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;

public final class SwingPopup implements UIPopup {

    private final JDialog window;

    public SwingPopup() {
        this.window = new JDialog();
        this.window.setUndecorated(true);
        this.window.setModalityType(JDialog.ModalityType.MODELESS);
        this.window.setFocusableWindowState(false);
    }

    @Override
    public void show() {
        HoverDiagnosticLogger.logPopupShow();
        window.setVisible(true);
    }

    @Override
    public void hide() {
        HoverDiagnosticLogger.logPopupHide();
        window.setVisible(false);
    }

    @Override
    public boolean isVisible() {
        boolean visible = window.isVisible();
        HoverDiagnosticLogger.logPopupVisible(visible);
        return visible;
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

    @Override
    public void setFocusableWindowState(boolean focusable) {
        window.setFocusableWindowState(focusable);
    }

    public JDialog getWindow() {
        return window;
    }
}
