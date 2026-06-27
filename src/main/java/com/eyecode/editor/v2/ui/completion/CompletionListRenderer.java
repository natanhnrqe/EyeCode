package com.eyecode.editor.v2.ui.completion;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;

public final class CompletionListRenderer implements ListCellRenderer<CompletionItem> {

    @Override
    public Component getListCellRendererComponent(JList<? extends CompletionItem> list, CompletionItem value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(true);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        panel.setBackground(isSelected ? ColorManager.AUTOCOMPLETE_SELECTION_BG : ColorManager.AUTOCOMPLETE_BG);

        JLabel label = new JLabel(value.getLabel());
        label.setFont(TypographyManager.UI_CODE());
        label.setForeground(isSelected ? ColorManager.TEXT_PRIMARY : ColorManager.AUTOCOMPLETE_FG);

        JLabel detail = new JLabel(value.getDetail());
        detail.setFont(TypographyManager.UI_CODE().deriveFont(Font.PLAIN, Math.max(10f, TypographyManager.UI_CODE().getSize2D() - 2f)));
        detail.setForeground(isSelected ? ColorManager.TEXT_SECONDARY : ColorManager.TEXT_TERTIARY);

        panel.add(label, BorderLayout.WEST);
        panel.add(detail, BorderLayout.EAST);
        return panel;
    }
}
