package com.eyecode.editor.v2.ui.completion;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionSnapshot;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JWindow;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;

public final class CompletionPopup {

    private static final int MAX_VISIBLE_ROWS = 8;

    private final CompletionPopupPositioner positioner;
    private JWindow window;
    private JList<CompletionItem> list;

    public CompletionPopup() {
        this.positioner = new CompletionPopupPositioner();
    }

    public void show(JTextPane editor, CompletionSnapshot snapshot, int caretPosition) {
        if (snapshot == null || snapshot.isEmpty()) {
            hide();
            return;
        }

        ensureWindow(editor);
        DefaultListModel<CompletionItem> model = new DefaultListModel<>();
        for (CompletionItem item : snapshot.getItems()) {
            model.addElement(item);
        }
        list.setModel(model);
        list.setVisibleRowCount(Math.min(MAX_VISIBLE_ROWS, snapshot.size()));
        list.setSelectedIndex(0);

        Point position = positioner.positionFor(editor, caretPosition);
        window.pack();
        window.setMinimumSize(new Dimension(260, 0));
        window.setLocation(position);
        window.setVisible(true);
    }

    public void hide() {
        if (window != null) {
            window.setVisible(false);
        }
    }

    public boolean isVisible() {
        return window != null && window.isVisible();
    }

    private void ensureWindow(JTextPane editor) {
        if (window != null) return;

        Window owner = javax.swing.SwingUtilities.getWindowAncestor(editor);
        window = new JWindow(owner);
        window.setFocusableWindowState(false);

        list = new JList<>();
        list.setCellRenderer(new CompletionListRenderer());

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(320, 180));
        window.getContentPane().setLayout(new BorderLayout());
        window.getContentPane().add(scrollPane, BorderLayout.CENTER);
    }
}
