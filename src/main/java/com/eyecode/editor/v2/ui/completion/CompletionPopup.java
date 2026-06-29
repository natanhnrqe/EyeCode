package com.eyecode.editor.v2.ui.completion;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionSnapshot;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.function.Consumer;

public final class CompletionPopup {

    private static final int MAX_VISIBLE_ROWS = 10;
    private static final int OUTER_PADDING = 8;

    private final CompletionPopupPositioner positioner;
    private final CompletionListRenderer renderer;
    private JWindow window;
    private JList<CompletionItem> list;
    private JScrollPane scrollPane;
    private JPanel detailContainer;

    private JPanel documentationPanel;
    private JPanel headerPanel;
    private JLabel signatureLabel;
    private JLabel returnTypeLabel;
    private JTextArea descriptionArea;
    private JTextArea exampleArea;
    private int selectedIndex = 0;
    private int caretPosition;
    private String selectedLabel;
    private Consumer<CompletionSelectionEvent> onSelect;

    public CompletionPopup() {
        this.positioner = new CompletionPopupPositioner();
        this.renderer = new CompletionListRenderer();
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
        renderer.setMatchPrefix(prefix);
        populateModel(snapshot);
        this.caretPosition = caretPosition;

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
        renderer.setMatchPrefix(prefix);
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
        window.setBackground(new Color(0, 0, 0, 0));

        list = new JList<>();
        list.setFont(TypographyManager.UI_CODE());
        list.setBackground(new Color(0, 0, 0, 0));
        list.setForeground(ColorManager.AUTOCOMPLETE_FG);
        list.setSelectionBackground(new Color(0, 0, 0, 0));
        list.setSelectionForeground(ColorManager.TEXT_PRIMARY);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(renderer);
        list.setFixedCellHeight(-1);
        list.setOpaque(false);
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
        list.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index != renderer.getHoverIndexValue()) {
                    renderer.setHoverIndex(index);
                    list.repaint();
                }
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                renderer.setHoverIndex(-1);
                list.repaint();
            }
        });

        scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(460, 240));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBackground(new Color(0, 0, 0, 0));
        scrollPane.getViewport().setBackground(new Color(0, 0, 0, 0));
        scrollPane.setBorder(BorderFactory.createEmptyBorder(OUTER_PADDING, OUTER_PADDING, OUTER_PADDING, OUTER_PADDING));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setOpaque(false);

        UIManager.put("ScrollBar.thumb", ColorManager.SURFACE_BG);
        UIManager.put("ScrollBar.track", new Color(0, 0, 0, 0));
        UIManager.put("ScrollBar.width", 6);


        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        signatureLabel = new JLabel();
        signatureLabel.setFont(TypographyManager.UI_CODE());
        signatureLabel.setForeground(ColorManager.TEXT_PRIMARY);

        returnTypeLabel = new JLabel();
        returnTypeLabel.setFont(TypographyManager.UI_CODE());
        returnTypeLabel.setForeground(ColorManager.TEXT_MUTED);

        headerPanel.add(signatureLabel, BorderLayout.WEST);
        headerPanel.add(returnTypeLabel, BorderLayout.EAST);

        descriptionArea = new JTextArea();

        descriptionArea.setEditable(false);
        descriptionArea.setFocusable(false);

        descriptionArea.setOpaque(false);

        descriptionArea.setBorder(null);

        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        descriptionArea.setFont(TypographyManager.UI_SMALL());
        descriptionArea.setForeground(ColorManager.TEXT_SECONDARY);

        exampleArea = new JTextArea();

        exampleArea.setEditable(false);
        exampleArea.setFocusable(false);

        exampleArea.setOpaque(false);

        exampleArea.setBorder(null);

        exampleArea.setLineWrap(true);
        exampleArea.setWrapStyleWord(true);

        exampleArea.setFont(TypographyManager.UI_CODE());
        exampleArea.setForeground(ColorManager.SYNTAX_STRING);

        documentationPanel = new JPanel();
        documentationPanel.setOpaque(false);

        documentationPanel.setLayout(new BoxLayout(documentationPanel, BoxLayout.Y_AXIS));

        documentationPanel.setBorder(
                BorderFactory.createEmptyBorder(12,16,12,16)
        );

        documentationPanel.add(headerPanel);

        documentationPanel.add(Box.createVerticalStrut(10));

        documentationPanel.add(descriptionArea);

        documentationPanel.add(Box.createVerticalStrut(10));

        documentationPanel.add(exampleArea);

        detailContainer = new JPanel(new BorderLayout());

        detailContainer.setOpaque(false);

        detailContainer.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(
                                1,
                                0,
                                0,
                                0,
                                ColorManager.BORDER_DIVIDER
                        ),
                        BorderFactory.createEmptyBorder(4,0,0,0)
                )
        );

        detailContainer.add(documentationPanel);

        detailContainer.setVisible(false);

        detailContainer = new JPanel(new BorderLayout());

        detailContainer.setOpaque(false);

        detailContainer.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(
                                1,
                                0,
                                0,
                                0,
                                ColorManager.BORDER_DIVIDER
                        ),
                        BorderFactory.createEmptyBorder(4,0,0,0)
                )
        );

        detailContainer.add(documentationPanel);

        detailContainer.setVisible(false);

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
        centerSelectionInViewport();
        CompletionItem item = list.getModel().getElementAt(selectedIndex);
        selectedLabel = item != null ? item.getLabel() : null;
        updateDetailPanel(item);
    }

    private void centerSelectionInViewport() {
        if (list == null || scrollPane == null) return;
        int total = list.getModel().getSize();
        if (total == 0) return;

        int visibleRows = Math.min(MAX_VISIBLE_ROWS, total);
        int halfVisible = visibleRows / 2;
        int target = Math.max(0, Math.min(selectedIndex - halfVisible, total - visibleRows));
        if (target < 0) target = 0;

        int cellHeight = list.getFixedCellHeight();
        if (cellHeight <= 0) {
            try {
                Rectangle cellBounds = list.getCellBounds(selectedIndex, selectedIndex);
                if (cellBounds != null) {
                    cellHeight = cellBounds.height;
                }
            } catch (Exception ignored) {
            }
        }
        if (cellHeight > 0) {
            int viewportY = target * cellHeight;
            scrollPane.getViewport().setViewPosition(new Point(0, viewportY));
        }
        list.ensureIndexIsVisible(selectedIndex);
    }

    private void updateDetailPanel(CompletionItem item) {

    if (detailContainer == null) {
        return;
    }

    if (item == null) {
        detailContainer.setVisible(false);

        if (window != null) {
            window.pack();
        }
        return;
    }

    boolean hasSignature =
            item.getSignature() != null && !item.getSignature().isBlank();

    boolean hasReturnType =
            item.getReturnType() != null && !item.getReturnType().isBlank();

    boolean hasDescription =
            item.getDocumentation() != null && !item.getDocumentation().isBlank();

    boolean hasExample =
            item.getExample() != null && !item.getExample().isBlank();

    boolean hasContent =
            hasSignature || hasReturnType || hasDescription || hasExample;

    if (!hasContent) {
        detailContainer.setVisible(false);

        if (window != null) {
            window.pack();
        }
        return;
    }

    // Header

    signatureLabel.setVisible(hasSignature);
    signatureLabel.setText(hasSignature ? item.getSignature() : "");

    returnTypeLabel.setVisible(hasReturnType);
    returnTypeLabel.setText(hasReturnType ? item.getReturnType() : "");

    // Description

    descriptionArea.setVisible(hasDescription);
    descriptionArea.setText(hasDescription
            ? item.getDocumentation()
            : "");

    descriptionArea.setCaretPosition(0);

    // Example

    exampleArea.setVisible(hasExample);
    exampleArea.setText(hasExample
            ? item.getExample()
            : "");

    exampleArea.setCaretPosition(0);

    detailContainer.setVisible(true);

    documentationPanel.revalidate();
    documentationPanel.doLayout();

    Dimension pref = documentationPanel.getPreferredSize();

    detailContainer.setPreferredSize(
            new Dimension(460, pref.height + 8)
    );

    if (window != null) {
        window.pack();
    }
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
