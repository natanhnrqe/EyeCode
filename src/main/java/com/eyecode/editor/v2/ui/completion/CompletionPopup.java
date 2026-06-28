package com.eyecode.editor.v2.ui.completion;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionSnapshot;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JWindow;
import javax.swing.ListSelectionModel;
import javax.swing.border.MatteBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public final class CompletionPopup {

    private static final int MAX_VISIBLE_ROWS = 8;

    private final CompletionPopupPositioner positioner;
    private JWindow window;
    private JList<CompletionItem> list;
    private JScrollPane scrollPane;
    private JEditorPane detailPane;
    private JPanel detailContainer;
    private int selectedIndex = 0;
    private int caretPosition;
    private String selectedLabel;
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
        populateModel(snapshot);
        this.caretPosition = caretPosition;

        Point position = positioner.positionFor(editor, caretPosition);
        window.pack();
        window.setMinimumSize(new Dimension(300, 0));
        window.setLocation(position);
        window.setVisible(true);
    }

    public void update(CompletionSnapshot snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            hide();
            return;
        }
        if (window == null || !window.isVisible()) return;
        populateModel(snapshot);
    }

    public void move(JTextPane editor, int caretPosition) {
        if (window == null || !window.isVisible()) return;
        this.caretPosition = caretPosition;
        Point position = positioner.positionFor(editor, caretPosition);
        window.setLocation(position);
    }

    public void hide() {
        selectedLabel = null;
        if (window != null) {
            window.setVisible(false);
        }
    }

    public boolean isVisible() {
        return window != null && window.isVisible();
    }

    public void selectNext() {
        moveSelection(1);
    }

    public void selectPrevious() {
        moveSelection(-1);
    }

    public void selectPageDown() {
        moveSelection(MAX_VISIBLE_ROWS);
    }

    public void selectPageUp() {
        moveSelection(-MAX_VISIBLE_ROWS);
    }

    public void selectFirst() {
        moveSelectionTo(0);
    }

    public void selectLast() {
        if (list == null || list.getModel().getSize() == 0) return;
        moveSelectionTo(list.getModel().getSize() - 1);
    }

    public void acceptSelected() {
        emitSelection();
    }

    public CompletionItem getSelectedItem() {
        if (list == null || list.getModel().getSize() == 0) return null;
        if (selectedIndex < 0 || selectedIndex >= list.getModel().getSize()) return null;
        return list.getModel().getElementAt(selectedIndex);
    }

    public void setOnSelect(Consumer<CompletionSelectionEvent> onSelect) {
        this.onSelect = onSelect;
    }

    private void ensureWindow(JTextPane editor) {
        if (window != null) return;

        Window owner = javax.swing.SwingUtilities.getWindowAncestor(editor);
        window = new JWindow(owner);
        window.setFocusableWindowState(false);
        window.setBackground(ColorManager.AUTOCOMPLETE_BG);

        list = new JList<>();
        list.setFont(TypographyManager.UI_CODE());
        list.setBackground(ColorManager.AUTOCOMPLETE_BG);
        list.setForeground(ColorManager.AUTOCOMPLETE_FG);
        list.setSelectionBackground(ColorManager.AUTOCOMPLETE_SELECTION_BG);
        list.setSelectionForeground(ColorManager.TEXT_PRIMARY);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new CompletionListRenderer());
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

        scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(340, 180));
        scrollPane.setBackground(ColorManager.AUTOCOMPLETE_BG);
        scrollPane.getViewport().setBackground(ColorManager.AUTOCOMPLETE_BG);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        detailPane = new JEditorPane();
        detailPane.setContentType("text/html");
        detailPane.setEditable(false);
        detailPane.setBackground(ColorManager.PANEL_BG);
        detailPane.setForeground(ColorManager.TEXT_SECONDARY);
        detailPane.setFont(TypographyManager.UI_SMALL());
        detailPane.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        detailContainer = new JPanel(new BorderLayout());
        detailContainer.setBackground(ColorManager.PANEL_BG);
        detailContainer.setBorder(new MatteBorder(1, 0, 0, 0, ColorManager.BORDER_DIVIDER));
        detailContainer.add(detailPane, BorderLayout.CENTER);
        detailContainer.setVisible(false);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(ColorManager.AUTOCOMPLETE_BG);
        content.setBorder(BorderFactory.createLineBorder(ColorManager.BORDER));
        content.add(scrollPane, BorderLayout.CENTER);
        content.add(detailContainer, BorderLayout.SOUTH);

        window.getContentPane().setLayout(new BorderLayout());
        window.getContentPane().setBackground(ColorManager.AUTOCOMPLETE_BG);
        window.getContentPane().add(content, BorderLayout.CENTER);
    }

    private void populateModel(CompletionSnapshot snapshot) {
        DefaultListModel<CompletionItem> model = new DefaultListModel<>();
        for (CompletionItem item : snapshot.getItems()) {
            model.addElement(item);
        }

        int previousSelectedIndex = selectedIndex;
        Point previousViewPosition = scrollPane == null
                ? null
                : scrollPane.getViewport().getViewPosition();
        int newSelectedIndex = 0;
        boolean foundSelectedLabel = false;
        if (selectedLabel != null) {
            for (int i = 0; i < model.getSize(); i++) {
                if (model.getElementAt(i).getLabel().equals(selectedLabel)) {
                    newSelectedIndex = i;
                    foundSelectedLabel = true;
                    break;
                }
            }
        }

        list.setModel(model);
        list.setVisibleRowCount(Math.min(MAX_VISIBLE_ROWS, snapshot.size()));
        selectedIndex = newSelectedIndex;
        updateSelection();
        if (foundSelectedLabel && previousSelectedIndex == selectedIndex && previousViewPosition != null) {
            scrollPane.getViewport().setViewPosition(previousViewPosition);
        }
    }

    private void moveSelection(int delta) {
        if (list == null || list.getModel().getSize() == 0) return;
        moveSelectionTo(selectedIndex + delta);
    }

    private void moveSelectionTo(int index) {
        if (list == null || list.getModel().getSize() == 0) return;
        selectedIndex = Math.max(0, Math.min(index, list.getModel().getSize() - 1));
        updateSelection();
    }

    private void updateSelection() {
        if (list == null || list.getModel().getSize() == 0) return;
        selectedIndex = Math.max(0, Math.min(selectedIndex, list.getModel().getSize() - 1));
        list.setSelectedIndex(selectedIndex);
        list.ensureIndexIsVisible(selectedIndex);
        CompletionItem item = list.getModel().getElementAt(selectedIndex);
        selectedLabel = item != null ? item.getLabel() : null;
        updateDetailPanel(item);
    }

    private void updateDetailPanel(CompletionItem item) {
        if (detailContainer == null) return;

        if (item == null) {
            detailContainer.setVisible(false);
            if (window != null) window.pack();
            return;
        }

        String doc = item.getDocumentation();
        String example = item.getExample();
        String signature = item.getSignature();

        boolean hasDoc = doc != null && !doc.isEmpty();
        boolean hasExample = example != null && !example.isEmpty();
        boolean hasSig = signature != null && !signature.isEmpty();

        if (!hasDoc && !hasExample && !hasSig) {
            detailContainer.setVisible(false);
            if (window != null) window.pack();
            return;
        }

        StringBuilder html = new StringBuilder("<html><body style='font-family:monospaced;font-size:11px;color:#BBB;'>");

        if (hasSig) {
            html.append("<b style='color:#DDD;'>").append(escapeHtml(signature)).append("</b><br>");
        }
        if (hasDoc) {
            html.append(escapeHtml(doc));
            if (hasExample) html.append("<br><br>");
        }
        if (hasExample) {
            html.append("<i style='color:#999;'>Example:</i><br>")
                    .append("<code style='color:#6A8759;'>")
                    .append(escapeHtml(example))
                    .append("</code>");
        }
        html.append("</body></html>");

        detailPane.setText(html.toString());
        detailPane.setCaretPosition(0);
        detailContainer.setVisible(true);
        detailContainer.setPreferredSize(new Dimension(340, 60));
        if (window != null) window.pack();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("'", "&#39;")
                .replace("\"", "&quot;");
    }

    private void emitSelection() {
        if (list == null || list.getModel().getSize() == 0 || onSelect == null) return;
        CompletionItem item = list.getModel().getElementAt(selectedIndex);
        onSelect.accept(new CompletionSelectionEvent(item, selectedIndex, caretPosition));
    }
}
