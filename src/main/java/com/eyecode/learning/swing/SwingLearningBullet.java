package com.eyecode.learning.swing;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;

public final class SwingLearningBullet extends JPanel {

    private final JLabel marker;
    private final JLabel text;

    public SwingLearningBullet(String content) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(
                SwingLearningCardStyle.BODY_BULLET_TOP_GAP,
                SwingLearningCardStyle.BODY_BULLET_INDENT,
                SwingLearningCardStyle.BODY_BULLET_BOTTOM_GAP,
                0));
        setAlignmentX(Component.LEFT_ALIGNMENT);

        marker = new JLabel("·");
        marker.setFont(SwingLearningCardStyle.bodyBulletFont());
        marker.setForeground(SwingLearningCardStyle.BODY_BULLET_MARKER);

        text = new JLabel(content != null ? content : "");
        text.setFont(SwingLearningCardStyle.bodyBulletFont());
        text.setForeground(SwingLearningCardStyle.BODY_BULLET_COLOR);

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        row.add(marker);
        row.add(javax.swing.Box.createHorizontalStrut(6));
        row.add(text);
        row.add(javax.swing.Box.createHorizontalGlue());

        add(row, BorderLayout.CENTER);
    }

    public String getText() {
        return text.getText();
    }
}
