package com.eyecode.ui;

import javax.swing.*;
import java.awt.*;

public class TopBarPanel extends JPanel {

    private Runnable onRun;
    private Runnable onSave;
    private Runnable onOpenFolder;
    private Runnable onNewFile;

    private JButton runButton;
    private JButton saveButton;
    private JButton openFolderButton;
    private JButton newFileButton;


    public TopBarPanel() {

        setLayout(new BorderLayout());

        setPreferredSize(new Dimension(0, 42));

        setBackground(new Color(37, 37, 38));

        buildUi();
    }

    private void buildUi() {

        JPanel left =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                10,
                                6
                        )
                );

        left.setOpaque(false);

        JLabel logo = new JLabel("EyeCode");

        logo.setFont(
                new Font(
                        "JetBrains Mono",
                        Font.BOLD,
                        16
                )
        );

        logo.setForeground(
                new Color(220,220,220)
        );

        left.add(logo);

        JPanel right =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                8,
                                5
                        )
                );

        right.setOpaque(false);

        newFileButton =
                createToolbarButton("+");

        openFolderButton =
                createToolbarButton("📂");

        saveButton =
                createToolbarButton("💾");

        runButton =
                createToolbarButton("▶");

        right.add(newFileButton);
        right.add(openFolderButton);
        right.add(saveButton);
        right.add(runButton);

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.EAST);

        runButton.addActionListener(e -> {

            if (onRun != null) {
                onRun.run();
            }
        });

        saveButton.addActionListener(e -> {

            if (onSave != null) {
                onSave.run();
            }
        });

        openFolderButton.addActionListener(e -> {

            if (onOpenFolder != null) {
                onOpenFolder.run();
            }
        });

        newFileButton.addActionListener(e -> {

            if (onNewFile != null) {
                onNewFile.run();
            }
        });
    }

    private JButton createToolbarButton(String text) {

        JButton button = new JButton(text);

        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);

        button.setForeground(new Color(220, 220 ,220));

        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.setFont(new Font("Dialog", Font.PLAIN, 16));

        button.setPreferredSize(new Dimension(36, 30));

        return button;
    }

    public void setOnRun(Runnable onRun) {
        this.onRun = onRun;
    }

    public void setOnSave(Runnable onSave) {
        this.onSave = onSave;
    }

    public void setOnOpenFolder(Runnable onOpenFolder) {
        this.onOpenFolder = onOpenFolder;
    }

    public void setOnNewFile(Runnable onNewFile) {
        this.onNewFile = onNewFile;
    }
}
