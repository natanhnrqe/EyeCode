package com.eyecode.editor.v2.ui.completion;

import com.eyecode.editor.v2.completion.CompletionItem;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

public final class CompletionListRenderer implements ListCellRenderer<CompletionItem> {

    @Override
    public Component getListCellRendererComponent(JList<? extends CompletionItem> list, CompletionItem value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(true);
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8));
        panel.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());

        JLabel label = new JLabel(value.getLabel());
        label.setFont(list.getFont());
        label.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());

        JLabel detail = new JLabel(value.getDetail());
        detail.setFont(list.getFont().deriveFont(Font.PLAIN, Math.max(10f, list.getFont().getSize2D() - 2f)));
        detail.setForeground(isSelected ? list.getSelectionForeground() : Color.GRAY);

        panel.add(label, BorderLayout.WEST);
        panel.add(detail, BorderLayout.EAST);
        return panel;
    }
}
