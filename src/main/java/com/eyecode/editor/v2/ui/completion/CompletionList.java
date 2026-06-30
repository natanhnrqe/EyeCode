package com.eyecode.editor.v2.ui.completion;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionSnapshot;
import com.eyecode.ui.designsystem.ColorManager;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.function.IntConsumer;

public final class CompletionList {

    private static final int MAX_VISIBLE_ROWS = 10;
    private static final int OUTER_PADDING = 8;

    private final CompletionListRenderer renderer;
    private final JList<CompletionItem> list;
    private final JScrollPane scrollPane;
    private int selectedIndex = 0;
    private String selectedLabel;

    CompletionList(IntConsumer selectionCallback, Runnable doubleClickCallback) {
        this.renderer = new CompletionListRenderer();

        list = new JList<>();
        list.setOpaque(false);
        list.setBackground(new Color(0, 0, 0, 0));
        list.setForeground(ColorManager.AUTOCOMPLETE_FG);
        list.setSelectionBackground(new Color(0, 0, 0, 0));
        list.setSelectionForeground(ColorManager.TEXT_PRIMARY);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(renderer);
        list.setFixedCellHeight(-1);

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0) {
                    selectedIndex = index;
                    selectionCallback.accept(index);
                    if (e.getClickCount() == 2) {
                        doubleClickCallback.run();
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
        scrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(
                OUTER_PADDING, OUTER_PADDING, OUTER_PADDING, OUTER_PADDING));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setOpaque(false);

        UIManager.put("ScrollBar.thumb", ColorManager.SURFACE_BG);
        UIManager.put("ScrollBar.track", new Color(0, 0, 0, 0));
        UIManager.put("ScrollBar.width", 6);
    }

    JScrollPane getScrollPane() {
        return scrollPane;
    }

    void setMatchPrefix(String prefix) {
        renderer.setMatchPrefix(prefix);
    }

    int size() {
        return list.getModel().getSize();
    }

    CompletionItem get(int index) {
        if (index < 0 || index >= list.getModel().getSize()) return null;
        return list.getModel().getElementAt(index);
    }

    int getSelectedIndex() {
        return selectedIndex;
    }

    CompletionItem getSelectedItem() {
        return get(selectedIndex);
    }

    String getSelectedLabel() {
        return selectedLabel;
    }

    void setSelectedLabel(String label) {
        this.selectedLabel = label;
    }

    boolean isEmpty() {
        return list.getModel().getSize() == 0;
    }

    void update(CompletionSnapshot snapshot) {
        DefaultListModel<CompletionItem> model = new DefaultListModel<>();
        for (CompletionItem item : snapshot.getItems()) {
            model.addElement(item);
        }

        Point previousViewPosition = scrollPane.getViewport().getViewPosition();
        int previousSelectedIndex = selectedIndex;

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
        applySelection();

        if (foundSelectedLabel && previousSelectedIndex == selectedIndex && previousViewPosition != null) {
            scrollPane.getViewport().setViewPosition(previousViewPosition);
        }
    }

    void moveSelection(int delta) {
        if (isEmpty()) return;
        moveTo(selectedIndex + delta);
    }

    void moveTo(int index) {
        if (isEmpty()) return;
        selectedIndex = Math.max(0, Math.min(index, list.getModel().getSize() - 1));
        applySelection();
    }

    void moveToFirst() {
        moveTo(0);
    }

    void moveToLast() {
        if (isEmpty()) return;
        moveTo(list.getModel().getSize() - 1);
    }

    private void applySelection() {
        if (isEmpty()) return;
        selectedIndex = Math.max(0, Math.min(selectedIndex, list.getModel().getSize() - 1));
        list.setSelectedIndex(selectedIndex);
        centerSelection();
        CompletionItem item = get(selectedIndex);
        selectedLabel = item != null ? item.getLabel() : null;
    }

    private void centerSelection() {
        int total = list.getModel().getSize();
        if (total == 0) return;

        int visibleRows = Math.min(MAX_VISIBLE_ROWS, total);
        int halfVisible = visibleRows / 2;
        int target = Math.max(0, Math.min(selectedIndex - halfVisible, total - visibleRows));

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
}
