package com.eyecode.ui.core;

public interface UIContainer extends UIView {

    void add(UIComponent component);

    void remove(UIComponent component);

    void removeAll();

    void revalidate();

    void repaint();
}
