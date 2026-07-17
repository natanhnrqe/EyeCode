package com.eyecode.ui.core;

import java.awt.Component;

public interface UIContainer extends UIView {

    void add(UIComponent component);

    void add(Component component);

    void add(Component component, Object constraints);

    void remove(UIComponent component);

    void removeAll();

    void revalidate();

    void repaint();

    Component getComponent();
}
