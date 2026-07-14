package com.eyecode.learning.ui.components;

import com.eyecode.learning.document.LearningDocumentStyle;

import javax.swing.*;

public final class LearningSubtitle extends JLabel {

    public LearningSubtitle(String text) {
        super(text);
        setFont(LearningDocumentStyle.subtitleFont());
        setForeground(LearningDocumentStyle.subtitleColor());
        setBorder(LearningDocumentStyle.emptyBorder());
    }
}
