package com.eyecode.editor.v2.ui.completion;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public final class CompletionListRenderer implements ListCellRenderer<CompletionItem> {

    private static final Font MAIN_FONT = TypographyManager.UI_CODE();
    private static final Font SMALL_FONT = MAIN_FONT.deriveFont(Font.PLAIN, Math.max(10f, MAIN_FONT.getSize2D() - 2f));
    private static final Font META_FONT = MAIN_FONT.deriveFont(Font.ITALIC, Math.max(10f, MAIN_FONT.getSize2D() - 2f));
    private static final Font ICON_FONT = MAIN_FONT.deriveFont(Font.BOLD, Math.max(9f, MAIN_FONT.getSize2D() - 3f));

    @Override
    public Component getListCellRendererComponent(JList<? extends CompletionItem> list, CompletionItem value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        Color bg = isSelected ? ColorManager.AUTOCOMPLETE_SELECTION_BG : ColorManager.AUTOCOMPLETE_BG;
        Color fg = isSelected ? ColorManager.TEXT_PRIMARY : ColorManager.AUTOCOMPLETE_FG;
        Color metaFg = isSelected ? ColorManager.TEXT_SECONDARY : ColorManager.TEXT_TERTIARY;
        Color mutedFg = isSelected ? ColorManager.TEXT_TERTIARY : ColorManager.TEXT_MUTED;

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(true);
        panel.setBackground(bg);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));

        GridBagConstraints c = new GridBagConstraints();

        JLabel iconLabel = new JLabel(kindIcon(value.getKind()), JLabel.CENTER);
        iconLabel.setFont(ICON_FONT);
        iconLabel.setForeground(isSelected ? ColorManager.TEXT_PRIMARY : kindColor(value.getKind()));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 2;
        c.insets = new Insets(0, 0, 0, 8);
        c.anchor = GridBagConstraints.WEST;
        panel.add(iconLabel, c);

        int gridy = 0;
        String returnType = value.getReturnType();
        boolean hasReturnType = returnType != null && !returnType.isEmpty();

        JLabel nameLabel = new JLabel(value.getLabel());
        nameLabel.setFont(MAIN_FONT);
        nameLabel.setForeground(fg);
        c.gridx = 1;
        c.gridy = gridy;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.insets = new Insets(0, 0, 0, 4);
        c.anchor = GridBagConstraints.WEST;
        panel.add(nameLabel, c);

        if (hasReturnType) {
            JLabel typeLabel = new JLabel(returnType);
            typeLabel.setFont(META_FONT);
            typeLabel.setForeground(metaFg);
            c.gridx = 2;
            c.gridy = gridy;
            c.weightx = 0.0;
            c.insets = new Insets(0, 4, 0, 0);
            c.anchor = GridBagConstraints.EAST;
            panel.add(typeLabel, c);
        }

        gridy++;

        String signature = value.getSignature();
        if (signature != null && !signature.isEmpty()) {
            JLabel sigLabel = new JLabel(signature);
            sigLabel.setFont(META_FONT);
            sigLabel.setForeground(metaFg);
            c.gridx = 1;
            c.gridy = gridy;
            c.gridwidth = hasReturnType ? 1 : 2;
            c.weightx = 1.0;
            c.insets = new Insets(1, 0, 0, 0);
            c.anchor = GridBagConstraints.WEST;
            panel.add(sigLabel, c);
            gridy++;
        }

        String owner = value.getOwner();
        if (owner != null && !owner.isEmpty()) {
            JLabel ownerLabel = new JLabel(owner);
            ownerLabel.setFont(SMALL_FONT);
            ownerLabel.setForeground(mutedFg);
            c.gridx = 1;
            c.gridy = gridy;
            c.gridwidth = hasReturnType ? 1 : 2;
            c.weightx = 1.0;
            c.insets = new Insets(1, 0, 0, 0);
            c.anchor = GridBagConstraints.WEST;
            panel.add(ownerLabel, c);
        }

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
