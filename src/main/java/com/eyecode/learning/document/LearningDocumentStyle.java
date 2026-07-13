package com.eyecode.learning.document;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;

public final class LearningDocumentStyle {

    private LearningDocumentStyle() {}

    private static final SimpleAttributeSet TITLE;
    private static final SimpleAttributeSet SUBTITLE;
    private static final SimpleAttributeSet SECTION_TITLE;
    private static final SimpleAttributeSet BODY;
    private static final SimpleAttributeSet CODE;
    private static final SimpleAttributeSet BULLET;
    private static final SimpleAttributeSet DIVIDER_LEFT;
    private static final SimpleAttributeSet DIVIDER_RIGHT;

    static {
        TITLE = create(TypographyManager.monoBold(16), ColorManager.TEXT_PRIMARY, null, 0, 0);
        SUBTITLE = create(TypographyManager.monoRegular(11), ColorManager.TEXT_SECONDARY, null, 0, 0);
        SECTION_TITLE = create(TypographyManager.monoBold(12), ColorManager.SYNTAX_CLASS, null, 8, 2);
        BODY = create(TypographyManager.monoRegular(13), ColorManager.TEXT_SECONDARY, null, 2, 2);
        CODE = create(TypographyManager.monoRegular(13), ColorManager.EDITOR_FOREGROUND, ColorManager.EDITOR_BG, 4, 4);
        BULLET = create(TypographyManager.monoRegular(12), ColorManager.TEXT_SECONDARY, null, 1, 1);

        DIVIDER_LEFT = new SimpleAttributeSet();
        StyleConstants.setComponent(DIVIDER_LEFT, createDividerComponent(false));

        DIVIDER_RIGHT = new SimpleAttributeSet();
        StyleConstants.setComponent(DIVIDER_RIGHT, createDividerComponent(true));
    }

    public static SimpleAttributeSet title() { return copy(TITLE); }
    public static SimpleAttributeSet subtitle() { return copy(SUBTITLE); }
    public static SimpleAttributeSet sectionTitle() { return copy(SECTION_TITLE); }
    public static SimpleAttributeSet body() { return copy(BODY); }
    public static SimpleAttributeSet code() { return copy(CODE); }
    public static SimpleAttributeSet bullet() { return copy(BULLET); }
    public static SimpleAttributeSet dividerLeft() { return copy(DIVIDER_LEFT); }
    public static SimpleAttributeSet dividerRight() { return copy(DIVIDER_RIGHT); }

    private static SimpleAttributeSet create(Font font, Color fg, Color bg, int spaceAbove, int spaceBelow) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attrs, font.getFamily());
        StyleConstants.setFontSize(attrs, font.getSize());
        StyleConstants.setBold(attrs, font.isBold());
        StyleConstants.setItalic(attrs, font.isItalic());
        StyleConstants.setForeground(attrs, fg);
        if (bg != null) {
            StyleConstants.setBackground(attrs, bg);
        }
        StyleConstants.setSpaceAbove(attrs, (float) spaceAbove);
        StyleConstants.setSpaceBelow(attrs, (float) spaceBelow);
        StyleConstants.setLeftIndent(attrs, 0.0f);
        StyleConstants.setFirstLineIndent(attrs, 0.0f);
        return attrs;
    }

    private static javax.swing.JComponent createDividerComponent(boolean rightSide) {
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout());
        panel.setOpaque(false);
        javax.swing.JSeparator sep = new javax.swing.JSeparator();
        sep.setForeground(ColorManager.BORDER_DIVIDER);
        if (rightSide) {
            panel.add(sep, java.awt.BorderLayout.SOUTH);
        } else {
            panel.add(sep, java.awt.BorderLayout.CENTER);
        }
        panel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 1));
        panel.setPreferredSize(new java.awt.Dimension(10, 1));
        return panel;
    }

    private static SimpleAttributeSet copy(SimpleAttributeSet source) {
        return new SimpleAttributeSet(source);
    }
}
