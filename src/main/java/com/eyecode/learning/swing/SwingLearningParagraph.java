package com.eyecode.learning.swing;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.Font;

public final class SwingLearningParagraph extends JPanel {

    private final JTextArea area;

    public SwingLearningParagraph(String text) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        area = new JTextArea(text != null ? text : "");
        area.setFont(TypographyManager.monoRegular(12));
        area.setForeground(ColorManager.TEXT_SECONDARY);
        area.setBackground(ColorManager.CARD_BG);
        area.setBorder(new EmptyBorder(4, 0, 12, 0));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setOpaque(false);
        area.setAlignmentX(LEFT_ALIGNMENT);

        add(area);
    }

    public void setText(String text) {
        area.setText(text != null ? text : "");
    }

    public String getText() {
        return area.getText();
    }
}
