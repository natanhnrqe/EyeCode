package com.eyecode.learning.ui;

import com.eyecode.learning.content.LearningPage;
import com.eyecode.learning.document.LearningDocumentView;
import com.eyecode.learning.model.ConceptType;
import com.eyecode.learning.model.DifficultyLevel;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.ui.components.LearningButton;
import com.eyecode.learning.ui.components.LearningSubtitle;
import com.eyecode.learning.ui.components.LearningTitle;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.IconManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class LearningHoverPopup {

    private static final int CURSOR_OFFSET = 12;
    private static final int CARD_WIDTH = 400;
    private static final int CARD_HEIGHT = 480;
    private static final int PADDING = 16;

    private JWindow window;
    private LearningDocumentView documentView;

    public void show(Component owner, LearningConcept concept, Point screenLocation) {
        ensureWindow(owner);

        LearningPage page = concept != null ? concept.getPage() : null;
        boolean hasPage = page != null && page.getSections() != null && !page.getSections().isEmpty();

        if (hasPage) {
            showCard(concept, page);
        } else {
            showMini(concept);
        }

        window.setLocation(adjustedPosition(screenLocation, window.getWidth(), window.getHeight()));
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

    public Window getWindow() {
        return window;
    }

    private void ensureWindow(Component owner) {
        if (window != null) return;

        Window win = SwingUtilities.getWindowAncestor(owner);
        window = new JWindow(win);
        window.setFocusableWindowState(false);
        window.setBackground(new Color(0, 0, 0, 0));
    }

    private void buildCard(LearningConcept concept, JComponent body) {
        JPanel header = createHeader(concept);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(4, 0, 8, 0));
        footer.add(new LearningButton("Abrir documenta\u00e7\u00e3o"));

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(header, BorderLayout.NORTH);
        center.add(body, BorderLayout.CENTER);
        center.add(footer, BorderLayout.SOUTH);

        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ColorManager.CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(ColorManager.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        wrapper.setOpaque(false);
        wrapper.add(center, BorderLayout.CENTER);

        window.getContentPane().removeAll();
        window.getContentPane().add(wrapper, BorderLayout.CENTER);
    }

    private void showCard(LearningConcept concept, LearningPage page) {
        documentView = new LearningDocumentView();
        documentView.setPage(page);

        buildCard(concept, documentView);
        window.setSize(CARD_WIDTH, CARD_HEIGHT);
    }

    private void showMini(LearningConcept concept) {
        if (concept == null) return;

        JTextArea desc = new JTextArea(concept.getDescription());
        desc.setFont(desc.getFont().deriveFont(12f));
        desc.setForeground(ColorManager.TEXT_SECONDARY);
        desc.setBackground(new Color(0, 0, 0, 0));
        desc.setEditable(false);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setBorder(new EmptyBorder(8, PADDING, 8, PADDING));
        desc.setOpaque(false);

        buildCard(concept, desc);
        window.pack();
    }

    private JPanel createHeader(LearningConcept concept) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(PADDING, PADDING, 6, PADDING));

        String typeLabel = typeDisplayName(concept.getType());
        String diffLabel = difficultyDisplayName(concept.getDifficulty());

        LearningTitle title = new LearningTitle(concept.getTitle(), IconManager.javaFile());
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        LearningSubtitle subtitle = new LearningSubtitle(typeLabel + " \u2022 " + diffLabel);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(subtitle);

        panel.add(textPanel, BorderLayout.CENTER);

        JSeparator sep = new JSeparator();
        sep.setForeground(ColorManager.BORDER_DIVIDER);

        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);
        outer.add(panel, BorderLayout.CENTER);
        outer.add(sep, BorderLayout.SOUTH);
        return outer;
    }

    private static String typeDisplayName(ConceptType type) {
        if (type == null) return "";
        String name = type.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }

    private static String difficultyDisplayName(DifficultyLevel d) {
        if (d == null) return "";
        return switch (d) {
            case BEGINNER -> "Iniciante";
            case INTERMEDIATE -> "Intermedi\u00e1rio";
            case ADVANCED -> "Avan\u00e7ado";
        };
    }

    private static Point adjustedPosition(Point mouse, int popupWidth, int popupHeight) {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle screen = gc.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);

        int x = mouse.x + CURSOR_OFFSET;
        int y = mouse.y + CURSOR_OFFSET;

        if (x + popupWidth > screen.x + screen.width - insets.right) {
            x = mouse.x - CURSOR_OFFSET - popupWidth;
        }
        if (y + popupHeight > screen.y + screen.height - insets.bottom) {
            y = mouse.y - CURSOR_OFFSET - popupHeight;
        }
        x = Math.max(screen.x + insets.left, x);
        y = Math.max(screen.y + insets.top, y);

        return new Point(x, y);
    }
}
