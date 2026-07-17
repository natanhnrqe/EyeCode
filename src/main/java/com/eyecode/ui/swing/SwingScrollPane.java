package com.eyecode.ui.swing;

import com.eyecode.ui.core.UIScrollPane;

import java.awt.Component;
import javax.swing.JScrollPane;

public final class SwingScrollPane implements UIScrollPane {

    private final JScrollPane scrollPane;

    public SwingScrollPane() {
        this.scrollPane = new JScrollPane();
    }

    public SwingScrollPane(Component view) {
        this.scrollPane = new JScrollPane(view);
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }
}
