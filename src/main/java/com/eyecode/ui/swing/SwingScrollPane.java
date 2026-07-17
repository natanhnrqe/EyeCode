package com.eyecode.ui.swing;

import com.eyecode.ui.core.UIScrollPane;

import javax.swing.JScrollPane;

public final class SwingScrollPane implements UIScrollPane {

    private final JScrollPane scrollPane;

    public SwingScrollPane() {
        this.scrollPane = new JScrollPane();
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }
}
