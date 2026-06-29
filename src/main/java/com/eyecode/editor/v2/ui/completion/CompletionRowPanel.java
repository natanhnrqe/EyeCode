package com.eyecode.editor.v2.ui.completion;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.IconManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.*;
import java.awt.*;

final class CompletionRowPanel extends JPanel {

    private static final int ARC = 8;
    private static final int TYPE_WIDTH = 70;

    private final JLabel iconLabel = new JLabel();

    private final JLabel nameLabel = new JLabel();

    private final JLabel signatureLabel = new JLabel();

    private final JLabel returnTypeLabel = new JLabel();

    private final JLabel ownerLabel = new JLabel();

    private Color backgroundColor = ColorManager.AUTOCOMPLETE_BG;

    CompletionRowPanel() {

        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(6,10,6,10));

        //---------------- Icon ----------------

        JPanel iconPanel = new JPanel(new BorderLayout());
        iconPanel.setOpaque(false);
        iconPanel.setPreferredSize(new Dimension(20, 0));

        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);

        iconPanel.add(iconLabel, BorderLayout.CENTER);

        add(iconPanel, BorderLayout.WEST);

        //---------------- Labels ----------------

        nameLabel.setFont(TypographyManager.UI_CODE());

        signatureLabel.setFont(
                TypographyManager.UI_CODE().deriveFont(Font.PLAIN,11f));

        ownerLabel.setFont(TypographyManager.UI_SMALL());

        returnTypeLabel.setFont(TypographyManager.UI_SMALL());
        returnTypeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        Dimension typeSize = new Dimension(TYPE_WIDTH,18);

        returnTypeLabel.setPreferredSize(typeSize);
        returnTypeLabel.setMinimumSize(typeSize);
        returnTypeLabel.setMaximumSize(typeSize);

        //---------------- Left panel ----------------

        JPanel leftPanel = new JPanel();
        leftPanel.setOpaque(false);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));

        leftPanel.add(nameLabel);
        leftPanel.add(Box.createHorizontalStrut(6));
        leftPanel.add(signatureLabel);

        //---------------- Top row ----------------

        JPanel topRow = new JPanel(new BorderLayout(8,0));

        topRow.setOpaque(false);

        topRow.add(leftPanel, BorderLayout.CENTER);
        topRow.add(returnTypeLabel, BorderLayout.EAST);

        //---------------- Text panel ----------------

        JPanel textPanel = new JPanel();

        textPanel.setOpaque(false);

        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        ownerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(topRow);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(ownerLabel);

        //---------------- Root ----------------

        setLayout(new BorderLayout(4,0));

        add(iconLabel, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);
    }

    void setItem(
            CompletionItem item,
            String prefix,
            boolean selected,
            boolean hovered) {

        backgroundColor =
                selected
                        ? ColorManager.AUTOCOMPLETE_SELECTION_BG
                        : hovered
                            ? ColorManager.ACCENT_HOVER_BG
                            : ColorManager.AUTOCOMPLETE_BG;

        Color primary =
                selected
                        ? ColorManager.TEXT_PRIMARY
                        : ColorManager.AUTOCOMPLETE_FG;

        Color secondary =
                selected
                        ? ColorManager.TEXT_SECONDARY
                        : ColorManager.TEXT_TERTIARY;

        Color ownerColor =
                selected
                        ? ColorManager.TEXT_TERTIARY
                        : ColorManager.TEXT_MUTED;

        iconLabel.setIcon(
                IconManager.completion(item.getKind()));

        // futuramente aqui entra o highlight do prefix
        nameLabel.setText(item.getLabel());
        nameLabel.setForeground(primary);

        String signature = item.getSignature();

        if (signature == null || signature.isBlank()) {
            signature = item.getDetail();
        }

        signatureLabel.setVisible(signature != null && !signature.isBlank());
        signatureLabel.setText(signature == null ? "" : signature);
        signatureLabel.setForeground(secondary);

        String type = item.getReturnType();

        returnTypeLabel.setVisible(type != null && !type.isBlank());
        returnTypeLabel.setText(type == null ? "" : type);
        returnTypeLabel.setForeground(ColorManager.SYNTAX_TYPE);

        String owner = item.getOwner();

        ownerLabel.setVisible(owner != null && !owner.isBlank());
        ownerLabel.setText(owner == null ? "" : owner);
        ownerLabel.setForeground(ownerColor);

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g.create();

        try {

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(backgroundColor);

            g2.fillRoundRect(
                    2,
                    2,
                    getWidth()-4,
                    getHeight()-4,
                    ARC,
                    ARC);

        } finally {
            g2.dispose();
        }

        super.paintComponent(g);
    }
}