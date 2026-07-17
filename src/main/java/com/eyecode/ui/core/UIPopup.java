package com.eyecode.ui.core;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;

public interface UIPopup extends UIComponent {

    void show();

    void hide();

    boolean isVisible();

    void setLocation(int x, int y);

    void setSize(int width, int height);

    void setContent(Component content);

    void setBackground(Color color);

    Rectangle getBounds();

    void revalidate();

    void repaint();
}
