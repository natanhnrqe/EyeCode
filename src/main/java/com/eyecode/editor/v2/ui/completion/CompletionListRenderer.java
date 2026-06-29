package com.eyecode.editor.v2.ui.completion;

import com.eyecode.editor.v2.completion.CompletionItem;
import javax.swing.*;
import java.awt.Component;


public final class CompletionListRenderer implements ListCellRenderer<CompletionItem> {

    private final CompletionRowPanel renderer = new CompletionRowPanel();

    private int hoverIndex = -1;
    private String matchPrefix = "";

    public void setHoverIndex(int index) {
        hoverIndex = index;

    }

    public int getHoverIndexValue() {
        return hoverIndex;
    }

    public void setMatchPrefix(String prefix) {
        matchPrefix = prefix == null ? "" : prefix;
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends CompletionItem> list,
            CompletionItem value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

        renderer.setItem(
                value,
                matchPrefix,
                isSelected,
                index == hoverIndex
        );

        return renderer;
    }
}