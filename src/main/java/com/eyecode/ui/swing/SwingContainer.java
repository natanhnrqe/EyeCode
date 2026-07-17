package com.eyecode.ui.swing;

import com.eyecode.ui.core.UIComponent;
import com.eyecode.ui.core.UIContainer;

import javax.swing.JPanel;

public final class SwingContainer implements UIContainer {

    private final JPanel panel;

    public SwingContainer() {
        this.panel = new JPanel();
    }

    @Override
    public void add(UIComponent component) {
        if (component instanceof SwingContainer c) {
            panel.add(c.getPanel());
        }
    }

    @Override
    public void remove(UIComponent component) {
        if (component instanceof SwingContainer c) {
            panel.remove(c.getPanel());
        }
    }

    @Override
    public void removeAll() {
        panel.removeAll();
    }

    @Override
    public void revalidate() {
        panel.revalidate();
    }

    @Override
    public void repaint() {
        panel.repaint();
    }

    public JPanel getPanel() {
        return panel;
    }
}
