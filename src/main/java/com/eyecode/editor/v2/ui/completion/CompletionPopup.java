package com.eyecode.editor.v2.ui.completion;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionSnapshot;

import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public final class CompletionPopup {

    private static final int MAX_VISIBLE_ROWS = 8;

    private final CompletionPopupPositioner positioner;
    private JWindow window;
    private JList<CompletionItem> list;
    private int selectedIndex = 0;
    private int caretPosition;
    private Consumer<CompletionSelectionEvent> onSelect;

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
        selectedIndex = 0;
        updateSelection();
        this.caretPosition = caretPosition;

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

    public void setOnSelect(Consumer<CompletionSelectionEvent> onSelect) {
        this.onSelect = onSelect;
    }

    private void ensureWindow(JTextPane editor) {
        if (window != null) return;

        Window owner = javax.swing.SwingUtilities.getWindowAncestor(editor);
        window = new JWindow(owner);
        window.setFocusableWindowState(false);

        list = new JList<>();
        list.setCellRenderer(new CompletionListRenderer());
        installNavigationActions();
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0) {
                    selectedIndex = index;
                    updateSelection();
                    if (e.getClickCount() == 2) {
                        emitSelection();
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(320, 180));
        window.getContentPane().setLayout(new BorderLayout());
        window.getContentPane().add(scrollPane, BorderLayout.CENTER);
    }

    private void installNavigationActions() {
        InputMap inputMap = list.getInputMap(JList.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = list.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("UP"), "completionUp");
        actionMap.put("completionUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveSelection(-1);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "completionDown");
        actionMap.put("completionDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveSelection(1);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "completionHide");
        actionMap.put("completionHide", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hide();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("ENTER"), "completionSelect");
        actionMap.put("completionSelect", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                emitSelection();
            }
        });
    }

    private void moveSelection(int delta) {
        if (list == null || list.getModel().getSize() == 0) return;
        selectedIndex = Math.max(0, Math.min(selectedIndex + delta, list.getModel().getSize() - 1));
        updateSelection();
    }

    private void updateSelection() {
        if (list == null || list.getModel().getSize() == 0) return;
        selectedIndex = Math.max(0, Math.min(selectedIndex, list.getModel().getSize() - 1));
        list.setSelectedIndex(selectedIndex);
        list.ensureIndexIsVisible(selectedIndex);
    }

    private void emitSelection() {
        if (list == null || list.getModel().getSize() == 0 || onSelect == null) return;
        CompletionItem item = list.getModel().getElementAt(selectedIndex);
        onSelect.accept(new CompletionSelectionEvent(item, selectedIndex, caretPosition));
    }
}
