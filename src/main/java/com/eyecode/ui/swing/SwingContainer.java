package com.eyecode.ui.swing;

import com.eyecode.ui.core.UIComponent;
import com.eyecode.ui.core.UIContainer;

import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Graphics;
import java.util.function.Consumer;

public final class SwingContainer implements UIContainer {

    private final JPanel panel;
    private Consumer<Graphics> paintDelegate;

    public SwingContainer() {
        this.panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                if (paintDelegate != null) {
                    paintDelegate.accept(g);
                }
                super.paintComponent(g);
            }
        };
    }

    public void setPaintDelegate(Consumer<Graphics> delegate) {
        this.paintDelegate = delegate;
    }

    @Override
    public void add(UIComponent component) {
        if (component instanceof SwingContainer c) {
            panel.add(c.getPanel());
        }
    }

    @Override
    public void add(Component component) {
        panel.add(component);
    }

    @Override
    public void add(Component component, Object constraints) {
        panel.add(component, constraints);
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

    @Override
    public Component getComponent() {
        return panel;
    }

    public JPanel getPanel() {
        return panel;
    }
}
