package com.eyecode.editor.v2.ui.completion;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionSnapshot;
import com.eyecode.ui.designsystem.ColorManager;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.JWindow;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Window;
import java.util.function.Consumer;

public final class CompletionPopup {

    private final CompletionPopupPositioner positioner;
    private final CompletionList completionList;
    private final DocumentationPanel documentationPanel;
    private JWindow window;
    private int caretPosition;
    private Consumer<CompletionSelectionEvent> onSelect;

    public CompletionPopup() {
        this.positioner = new CompletionPopupPositioner();
        this.documentationPanel = new DocumentationPanel();
        this.completionList = new CompletionList(
                this::onSelectionChanged,
                this::emitSelection
        );
    }

    public void show(JTextPane editor, CompletionSnapshot snapshot, int caretPosition) {
        show(editor, snapshot, caretPosition, "");
    }

    public void show(JTextPane editor, CompletionSnapshot snapshot, int caretPosition, String prefix) {
        if (snapshot == null || snapshot.isEmpty()) {
            hide();
            return;
        }

        ensureWindow(editor);
        completionList.setMatchPrefix(prefix);
        completionList.update(snapshot);
        this.caretPosition = caretPosition;

        updateDocumentation();

        Point position = positioner.positionFor(editor, caretPosition);
        window.pack();
        window.setMinimumSize(new Dimension(600, 0));
        window.setLocation(position);
        window.setVisible(true);
    }

    public void update(CompletionSnapshot snapshot) {
        update(snapshot, "");
    }

    public void update(CompletionSnapshot snapshot, String prefix) {
        if (snapshot == null || snapshot.isEmpty()) {
            hide();
            return;
        }
        if (window == null || !window.isVisible()) return;
        completionList.setMatchPrefix(prefix);
        completionList.update(snapshot);
        updateDocumentation();
        window.pack();
    }

    public void move(JTextPane editor, int caretPosition) {
        if (window == null || !window.isVisible()) return;
        this.caretPosition = caretPosition;
        Point position = positioner.positionFor(editor, caretPosition);
        window.setLocation(position);
    }

    public void hide() {
        completionList.setSelectedLabel(null);
        if (window != null) {
            window.setVisible(false);
        }
    }

    public boolean isVisible() {
        return window != null && window.isVisible();
    }

    public void selectNext() {
        completionList.moveSelection(1);
        updateDocumentation();
        window.pack();
    }

    public void selectPrevious() {
        completionList.moveSelection(-1);
        updateDocumentation();
        window.pack();
    }

    public void selectPageDown() {
        completionList.moveSelection(10);
        updateDocumentation();
        window.pack();
    }

    public void selectPageUp() {
        completionList.moveSelection(-10);
        updateDocumentation();
        window.pack();
    }

    public void selectFirst() {
        completionList.moveToFirst();
        updateDocumentation();
        window.pack();
    }

    public void selectLast() {
        completionList.moveToLast();
        updateDocumentation();
        window.pack();
    }

    public void acceptSelected() {
        emitSelection();
    }

    public CompletionItem getSelectedItem() {
        return completionList.getSelectedItem();
    }

    public void setOnSelect(Consumer<CompletionSelectionEvent> onSelect) {
        this.onSelect = onSelect;
    }

    private void ensureWindow(JTextPane editor) {
        if (window != null) return;

        Window owner = javax.swing.SwingUtilities.getWindowAncestor(editor);
        window = new JWindow(owner);
        window.setFocusableWindowState(false);
        window.setBackground(new Color(0, 0, 0, 0));

        JPanel content = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ColorManager.AUTOCOMPLETE_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(ColorManager.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder());
        content.add(completionList.getScrollPane(), BorderLayout.CENTER);
        content.add(documentationPanel, BorderLayout.SOUTH);

        window.getContentPane().setLayout(new BorderLayout());
        window.getContentPane().setBackground(ColorManager.AUTOCOMPLETE_BG);
        window.getContentPane().add(content, BorderLayout.CENTER);
    }

    private void onSelectionChanged(int index) {
        updateDocumentation();
        if (window != null) window.pack();
    }

    private void updateDocumentation() {
        CompletionItem item = completionList.getSelectedItem();
        documentationPanel.update(item);
    }

    private void emitSelection() {
        CompletionItem item = completionList.getSelectedItem();
        if (item == null || onSelect == null) return;
        onSelect.accept(new CompletionSelectionEvent(item, completionList.getSelectedIndex(), caretPosition));
    }
}
