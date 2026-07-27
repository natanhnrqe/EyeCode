package com.eyecode.learning.swing;

import com.eyecode.ui.designsystem.ColorManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class SwingCodeBlock extends JPanel {

    private final JLabel headerLabel;
    private final JTextArea codeArea;
    private final JButton copyBtn;
    private final JScrollPane codeScroll;

    public SwingCodeBlock(String language, String code) {
        super(new BorderLayout());
        setOpaque(true);
        setBackground(SwingLearningCardStyle.CODE_BG);
        setBorder(BorderFactory.createLineBorder(SwingLearningCardStyle.CODE_BORDER, 1));

        codeArea = new JTextArea(code != null ? code : "");
        codeArea.setFont(SwingLearningCardStyle.codeTextFont());
        codeArea.setForeground(SwingLearningCardStyle.CODE_TEXT_COLOR);
        codeArea.setBackground(SwingLearningCardStyle.CODE_BG);
        codeArea.setBorder(SwingLearningCardStyle.codeAreaBorder());
        codeArea.setEditable(false);
        codeArea.setFocusable(false);
        codeArea.setLineWrap(false);
        codeArea.setWrapStyleWord(false);
        codeArea.setTabSize(4);

        codeScroll = new JScrollPane(codeArea);
        codeScroll.setBorder(BorderFactory.createEmptyBorder());
        codeScroll.setOpaque(false);
        codeScroll.getViewport().setOpaque(false);
        codeScroll.getViewport().setBackground(SwingLearningCardStyle.CODE_BG);
        codeScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        codeScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        codeScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                SwingLearningCardStyle.CODE_MAX_VISIBLE_HEIGHT));
        codeScroll.getHorizontalScrollBar().setPreferredSize(
                new Dimension(0, SwingLearningCardStyle.SCROLLBAR_WIDTH));
        codeScroll.getVerticalScrollBar().setPreferredSize(
                new Dimension(SwingLearningCardStyle.SCROLLBAR_WIDTH, 0));
        codeScroll.getHorizontalScrollBar().setUnitIncrement(20);
        codeScroll.getVerticalScrollBar().setUnitIncrement(20);

        headerLabel = new JLabel(language != null ? language : "");
        headerLabel.setFont(SwingLearningCardStyle.codeHeaderTextFont());
        headerLabel.setForeground(SwingLearningCardStyle.CODE_LANGUAGE_COLOR);
        headerLabel.setOpaque(true);
        headerLabel.setBackground(SwingLearningCardStyle.CODE_HEADER_BG);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(
                SwingLearningCardStyle.CODE_HEADER_PADDING_TOP,
                SwingLearningCardStyle.CODE_HEADER_PADDING_LEFT,
                SwingLearningCardStyle.CODE_HEADER_PADDING_BOTTOM,
                SwingLearningCardStyle.CODE_HEADER_PADDING_RIGHT));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(true);
        header.setBackground(SwingLearningCardStyle.CODE_HEADER_BG);
        header.add(headerLabel, BorderLayout.WEST);

        copyBtn = createCopyButton();
        header.add(copyBtn, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(codeScroll, BorderLayout.CENTER);
    }

    private JButton createCopyButton() {
        JButton btn = new JButton("Copy") {
            private boolean hover = false;
            private boolean pressed = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) {
                        if (isEnabled()) { hover = true; repaint(); }
                    }
                    @Override public void mouseExited(MouseEvent e) {
                        hover = false; pressed = false; repaint();
                    }
                    @Override public void mousePressed(MouseEvent e) {
                        if (isEnabled()) { pressed = true; repaint(); }
                    }
                    @Override public void mouseReleased(MouseEvent e) {
                        pressed = false; repaint();
                    }
                });
            }

            @Override
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                setForeground(enabled
                        ? SwingLearningCardStyle.CODE_BUTTON_FG
                        : ColorManager.TEXT_DISABLED);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    if (isEnabled() && pressed) {
                        g2.setColor(ColorManager.ACCENT_SELECTION);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                    } else if (isEnabled() && hover) {
                        g2.setColor(ColorManager.ACCENT_HOVER_BG);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                    } else {
                        g2.setColor(SwingLearningCardStyle.CODE_HEADER_BG);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                    }
                } finally { g2.dispose(); }
                super.paintComponent(g);
            }

            @Override public boolean isOpaque() { return false; }
        };
        btn.setFont(SwingLearningCardStyle.codeButtonFont());
        btn.setForeground(SwingLearningCardStyle.CODE_BUTTON_FG);
        btn.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        btn.setBorderPainted(false);
        btn.setFocusable(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        btn.addActionListener(e -> copy());
        return btn;
    }

    public String code() {
        return codeArea.getText();
    }

    public String language() {
        return headerLabel.getText();
    }

    public void setLanguage(String language) {
        headerLabel.setText(language != null ? language : "");
    }

    public void setCode(String code) {
        codeArea.setText(code != null ? code : "");
        codeArea.setCaretPosition(0);
    }

    public void copy() {
        String text = codeArea.getText();
        if (text == null || text.isEmpty()) {
            return;
        }
        java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(text);
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }

    public void setCopyAction(Runnable action) {
        for (java.awt.event.ActionListener al : copyBtn.getActionListeners()) {
            copyBtn.removeActionListener(al);
        }
        if (action != null) {
            copyBtn.addActionListener(e -> action.run());
        } else {
            copyBtn.addActionListener(e -> copy());
        }
    }

    public JScrollPane codeScroll() {
        return codeScroll;
    }
}
