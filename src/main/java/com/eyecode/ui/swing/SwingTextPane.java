package com.eyecode.ui.swing;

import com.eyecode.ui.core.UITextPane;

import javax.swing.JTextPane;

public final class SwingTextPane implements UITextPane {

    private final JTextPane textPane;

    public SwingTextPane() {
        this.textPane = new JTextPane();
    }

    public JTextPane getTextPane() {
        return textPane;
    }
}
