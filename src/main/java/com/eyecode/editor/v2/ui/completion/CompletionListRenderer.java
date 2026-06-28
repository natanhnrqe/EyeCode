package com.eyecode.editor.v2.ui.completion;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public final class CompletionListRenderer implements ListCellRenderer<CompletionItem> {

    private static final int ICON_COLUMN_WIDTH = 24;
    private static final int ROW_ARC = 8;
    private static final int ROW_PAD_H = 10;
    private static final int ROW_PAD_V = 5;

    private static final Font MAIN_FONT = TypographyManager.UI_CODE();
    private static final Font SMALL_FONT = MAIN_FONT.deriveFont(Font.PLAIN, Math.max(10f, MAIN_FONT.getSize2D() - 2f));
    private static final Font SIG_FONT = MAIN_FONT.deriveFont(Font.ITALIC, Math.max(10f, MAIN_FONT.getSize2D() - 2f));
    private static final Font BOLD_FONT = MAIN_FONT.deriveFont(Font.BOLD);

    private int hoverIndex = -1;
    private String matchPrefix = "";

    public void setHoverIndex(int index) {
        this.hoverIndex = index;
    }

    public int getHoverIndexValue() {
        return hoverIndex;
    }

    public void setMatchPrefix(String prefix) {
        this.matchPrefix = prefix == null ? "" : prefix;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends CompletionItem> list, CompletionItem value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        boolean isHovered = index == hoverIndex && !isSelected;
        Color bg = isSelected ? ColorManager.AUTOCOMPLETE_SELECTION_BG
                : isHovered ? ColorManager.ACCENT_HOVER_BG
                : ColorManager.AUTOCOMPLETE_BG;
        Color fg = isSelected ? ColorManager.TEXT_PRIMARY : ColorManager.AUTOCOMPLETE_FG;
        Color mutedFg = isSelected ? ColorManager.TEXT_TERTIARY : ColorManager.TEXT_MUTED;
        Color typeFg = isSelected ? ColorManager.TEXT_SECONDARY : ColorManager.SYNTAX_TYPE;
        Color sigFg = isSelected ? ColorManager.TEXT_SECONDARY : ColorManager.TEXT_TERTIARY;
        Color highlightFg = isSelected ? ColorManager.TEXT_PRIMARY : ColorManager.ACCENT_BLUE_LIGHT;

        JPanel panel = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, ROW_ARC, ROW_ARC);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(ROW_PAD_V, ROW_PAD_H, ROW_PAD_V, ROW_PAD_H));

        ImageIcon icon = CompletionIconManager.getIcon(value.getKind());
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setPreferredSize(new Dimension(ICON_COLUMN_WIDTH, icon.getIconHeight()));
        panel.add(iconLabel, BorderLayout.WEST);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JPanel topRow = new JPanel();
        topRow.setLayout(new BoxLayout(topRow, BoxLayout.X_AXIS));
        topRow.setOpaque(false);

        JLabel nameLabel = createHighlightedLabel(value.getLabel(), highlightFg, fg);
        nameLabel.setFont(MAIN_FONT);
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

    private JLabel createHighlightedLabel(String text, Color highlightColor, Color normalColor) {
        if (matchPrefix.isEmpty() || !text.toLowerCase().startsWith(matchPrefix.toLowerCase())) {
            JLabel label = new JLabel(text);
            label.setForeground(normalColor);
            return label;
        }

        int prefixLen = matchPrefix.length();
        String prefixPart = text.substring(0, prefixLen);
        String restPart = text.substring(prefixLen);

        JLabel label = new JLabel("<html><b style='color:" + toHex(highlightColor) + "'>"
                + escapeHtml(prefixPart) + "</b><span style='color:" + toHex(normalColor) + "'>"
                + escapeHtml(restPart) + "</span></html>");
        label.setForeground(normalColor);
        return label;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("'", "&#39;")
                .replace("\"", "&quot;");
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
