package com.eyecode.ui;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
import com.eyecode.ui.designsystem.UIConstants;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;

public class StatusBar extends JPanel {

    private static final int DOT_SIZE = 10;
    private static final int SEPARATOR_HEIGHT = 16;

    private final StatusBarItem logoItem;
    private final StatusBarItem projectItem;
    private final StatusBarItem statusItem;
    private final StatusBarItem languageItem;
    private final StatusBarItem encodingItem;
    private final StatusBarItem lineSeparatorItem;
    private final StatusBarItem positionItem;

    private final JLabel pathLabel;

    private File projectRoot;

    public StatusBar() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, UIConstants.STATUSBAR_HEIGHT));
        setBackground(ColorManager.WINDOW_BG);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ColorManager.BORDER_DIVIDER));

        // ── left panel ───────────────────────────────────────
        logoItem = new StatusBarItem("EyeCode");
        logoItem.setForeground(ColorManager.TEXT_PRIMARY);

        projectItem = new StatusBarItem("No project");

        statusItem = new StatusBarItem("Ready", new DotIcon(ColorManager.STATUS_READY));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(logoItem);
        leftPanel.add(createSeparator());
        leftPanel.add(projectItem);
        leftPanel.add(createSeparator());
        leftPanel.add(statusItem);

        // ── center (path) ────────────────────────────────────
        pathLabel = new JLabel();
        pathLabel.setFont(TypographyManager.UI_STATUS());
        pathLabel.setForeground(ColorManager.TEXT_MUTED);
        pathLabel.setBorder(BorderFactory.createEmptyBorder(0, SpacingSystem.SM, 0, 0));

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        centerPanel.setOpaque(false);
        centerPanel.add(pathLabel);

        // ── right panel ──────────────────────────────────────
        languageItem = new StatusBarItem("Plain Text");
        encodingItem = new StatusBarItem("UTF-8");
        lineSeparatorItem = new StatusBarItem("LF");
        positionItem = new StatusBarItem("Ln 1, Col 1");

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(languageItem);
        rightPanel.add(createSeparator());
        rightPanel.add(encodingItem);
        rightPanel.add(createSeparator());
        rightPanel.add(lineSeparatorItem);
        rightPanel.add(createSeparator());
        rightPanel.add(positionItem);

        // ── assemble ─────────────────────────────────────────
        add(leftPanel, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }

    // ── public API ───────────────────────────────────────────

    public void setProjectRoot(File root) {
        this.projectRoot = root;
    }

    public void updateCaretPosition(int line, int column) {
        positionItem.setText("Ln " + line + ", Col " + column);
    }

    public void updateFile(String fileName) {
        updateLanguage(fileName);
    }

    public void updatePath(File file) {
        if (file == null) {
            pathLabel.setText("");
            return;
        }

        String display = file.getPath().replace("\\", " > ");
        if (projectRoot != null) {
            try {
                Path rootPath = projectRoot.toPath().toAbsolutePath().normalize();
                Path filePath = file.toPath().toAbsolutePath().normalize();
                String relative = rootPath.relativize(filePath).toString().replace("\\", " > ");
                if (relative.length() < display.length()) {
                    display = relative;
                }
            } catch (Exception ignored) {}
        }

        if (display.length() > 80) {
            display = "..." + display.substring(display.length() - 77);
        }

        pathLabel.setText(display);
    }

    public void updateProject(String projectName) {
        projectItem.setText(projectName == null || projectName.isBlank() ? "No project" : projectName);
    }

    public void updateStatus(String status) {
        statusItem.setText(status == null || status.isBlank() ? "Ready" : status);
    }

    public void setRunning(boolean running) {
        if (running) {
            statusItem.setText("Running");
            statusItem.setIcon(new DotIcon(ColorManager.STATUS_BUSY));
        } else {
            statusItem.setText("Ready");
            statusItem.setIcon(new DotIcon(ColorManager.STATUS_READY));
        }
    }

    // ── internal ─────────────────────────────────────────────

    private void updateLanguage(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            languageItem.setText("Plain Text");
            return;
        }
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        languageItem.setText(switch (extension) {
            case "java" -> "Java";
            case "xml" -> "XML";
            case "md" -> "Markdown";
            case "json" -> "JSON";
            default -> extension.toUpperCase();
        });
    }

    private JComponent createSeparator() {
        JPanel sep = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(ColorManager.BORDER_DIVIDER);
                g.fillRect(0, 0, 1, SEPARATOR_HEIGHT);
            }
        };
        sep.setPreferredSize(new Dimension(1, SEPARATOR_HEIGHT));
        sep.setMaximumSize(new Dimension(1, SEPARATOR_HEIGHT));
        sep.setOpaque(false);
        sep.setBorder(BorderFactory.createEmptyBorder(0, SpacingSystem.XL, 0, SpacingSystem.XL));
        return sep;
    }

    private static class DotIcon implements Icon {
        private final Color color;

        DotIcon(Color color) {
            this.color = color;
        }

        @Override
        public int getIconWidth() {
            return DOT_SIZE;
        }

        @Override
        public int getIconHeight() {
            return DOT_SIZE;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x, y, DOT_SIZE, DOT_SIZE);
            g2.dispose();
        }
    }
}
