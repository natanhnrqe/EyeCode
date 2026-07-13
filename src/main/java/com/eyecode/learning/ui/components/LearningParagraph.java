package com.eyecode.learning.ui.components;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;

public final class LearningParagraph extends JTextPane {

    private static final int FONT_SIZE = 13;

    public LearningParagraph(String text) {
        setContentType("text/plain");
        setText(text);
        setFont(TypographyManager.monoRegular(FONT_SIZE));
        setForeground(ColorManager.TEXT_SECONDARY);
        setBackground(new Color(0, 0, 0, 0));
        setEditable(false);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        StyledDocument doc = getStyledDocument();
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setLineSpacing(attrs, 0.3f);
        doc.setParagraphAttributes(0, doc.getLength(), attrs, false);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }
}
