package com.eyecode.learning.ui.components;

import com.eyecode.learning.document.LearningDocumentStyle;

import javax.swing.*;
import java.awt.*;

public final class LearningButton extends JButton {

    public LearningButton(String text) {
        super(text);
        setFont(LearningDocumentStyle.buttonFont());
        setForeground(LearningDocumentStyle.buttonColor());
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(LearningDocumentStyle.buttonBorder());
    }
}
