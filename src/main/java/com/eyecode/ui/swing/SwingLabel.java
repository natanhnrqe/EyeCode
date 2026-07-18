package com.eyecode.ui.swing;

import com.eyecode.ui.core.UILabel;

import javax.swing.JLabel;

public final class SwingLabel implements UILabel {

    private final JLabel label;

    public SwingLabel() {
        this.label = new JLabel();
    }

    public JLabel getLabel() {
        return label;
    }
}
