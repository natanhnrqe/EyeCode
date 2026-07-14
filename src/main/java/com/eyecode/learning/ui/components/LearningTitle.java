package com.eyecode.learning.ui.components;

import com.eyecode.learning.document.LearningDocumentStyle;

import javax.swing.*;

public final class LearningTitle extends JLabel {

    public LearningTitle(String text, Icon icon) {
        super(text, icon, LEADING);
        setFont(LearningDocumentStyle.titleFont());
        setForeground(LearningDocumentStyle.titleColor());
        setIconTextGap(LearningDocumentStyle.titleIconTextGap());
        setBorder(LearningDocumentStyle.emptyBorder());
    }

    public LearningTitle(String text) {
        this(text, null);
    }
}
