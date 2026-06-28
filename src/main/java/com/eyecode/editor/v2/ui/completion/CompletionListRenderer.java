package com.eyecode.editor.v2.ui.completion;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

public final class CompletionListRenderer implements ListCellRenderer<CompletionItem> {

    private static final int ICON_COLUMN_WIDTH = 28;

    private static final Font MAIN_FONT = TypographyManager.UI_CODE();
    private static final Font SMALL_FONT = MAIN_FONT.deriveFont(Font.PLAIN, Math.max(10f, MAIN_FONT.getSize2D() - 2f));
    private static final Font SIG_FONT = MAIN_FONT.deriveFont(Font.ITALIC, Math.max(10f, MAIN_FONT.getSize2D() - 2f));
    private static final Font ICON_FONT = MAIN_FONT.deriveFont(Font.BOLD, Math.max(10f, MAIN_FONT.getSize2D() - 2f));

    @Override
    public Component getListCellRendererComponent(JList<? extends CompletionItem> list, CompletionItem value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        Color bg = isSelected ? ColorManager.AUTOCOMPLETE_SELECTION_BG : ColorManager.AUTOCOMPLETE_BG;
        Color fg = isSelected ? ColorManager.TEXT_PRIMARY : ColorManager.AUTOCOMPLETE_FG;
        Color mutedFg = isSelected ? ColorManager.TEXT_TERTIARY : ColorManager.TEXT_MUTED;
        Color typeFg = isSelected ? ColorManager.TEXT_SECONDARY : ColorManager.SYNTAX_TYPE;
        Color sigFg = isSelected ? ColorManager.TEXT_SECONDARY : ColorManager.TEXT_TERTIARY;

        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setOpaque(true);
        panel.setBackground(bg);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JLabel iconLabel = new JLabel(kindIcon(value.getKind()));
        iconLabel.setFont(ICON_FONT);
        iconLabel.setForeground(isSelected ? ColorManager.TEXT_PRIMARY : kindColor(value.getKind()));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setPreferredSize(new Dimension(ICON_COLUMN_WIDTH, MAIN_FONT.getSize() + 4));
        panel.add(iconLabel, BorderLayout.WEST);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JPanel topRow = new JPanel();
        topRow.setLayout(new BoxLayout(topRow, BoxLayout.X_AXIS));
        topRow.setOpaque(false);

        JLabel nameLabel = new JLabel(value.getLabel());
        nameLabel.setFont(MAIN_FONT);
        nameLabel.setForeground(fg);
        nameLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        topRow.add(nameLabel);

        String signature = value.getSignature();
        if (signature != null && !signature.isEmpty()) {
            topRow.add(javax.swing.Box.createHorizontalStrut(8));
            JLabel sigLabel = new JLabel(signature);
            sigLabel.setFont(SIG_FONT);
            sigLabel.setForeground(sigFg);
            sigLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
            topRow.add(sigLabel);
        }

        topRow.add(javax.swing.Box.createHorizontalGlue());

        String returnType = value.getReturnType();
        if (returnType != null && !returnType.isEmpty()) {
            JLabel typeLabel = new JLabel(returnType);
            typeLabel.setFont(SMALL_FONT);
            typeLabel.setForeground(typeFg);
            typeLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
            topRow.add(typeLabel);
        }

        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(topRow);

        String owner = value.getOwner();
        if (owner != null && !owner.isEmpty()) {
            JLabel ownerLabel = new JLabel(owner);
            ownerLabel.setFont(SMALL_FONT);
            ownerLabel.setForeground(mutedFg);
            ownerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(javax.swing.Box.createVerticalStrut(1));
            content.add(ownerLabel);
        }

        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private String kindIcon(CompletionItemKind kind) {
        if (kind == null) return "?";
        return switch (kind) {
            case KEYWORD -> "K";
            case CLASS -> "C";
            case INTERFACE -> "I";
            case METHOD -> "M";
            case FIELD -> "F";
            case VARIABLE -> "V";
            case PACKAGE -> "P";
            case SNIPPET -> "S";
        };
    }

    private Color kindColor(CompletionItemKind kind) {
        if (kind == null) return ColorManager.AUTOCOMPLETE_FG;
        return switch (kind) {
            case KEYWORD -> ColorManager.SYNTAX_KEYWORD;
            case CLASS -> ColorManager.SYNTAX_CLASS;
            case INTERFACE -> ColorManager.SYNTAX_TYPE;
            case METHOD -> ColorManager.SYNTAX_METHOD;
            case FIELD -> ColorManager.SYNTAX_CONSTANT;
            case VARIABLE -> ColorManager.AUTOCOMPLETE_FG;
            case PACKAGE -> ColorManager.SYNTAX_TYPE;
            case SNIPPET -> ColorManager.SYNTAX_ANNOTATION;
        };
    }
}
