package com.eyecode.ui;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
import com.eyecode.ui.designsystem.UIConstants;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StatusBar extends JPanel {

    private static final int DOT_SIZE = 10;
    private static final int SEPARATOR_HEIGHT = 14;

    private final JLabel statusIcon;
    private final JLabel statusLabel;
    private final JLabel pathLabel;
    private final JLabel positionLabel;
    private final JLabel encodingLabel;
    private final JLabel languageLabel;
    private final JLabel gitLabel;
    private final JLabel projectLabel;

    private File projectRoot;
    private Color currentDotColor = ColorManager.STATUS_READY;

    public StatusBar() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, UIConstants.STATUSBAR_HEIGHT));
        setBackground(ColorManager.WINDOW_BG);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ColorManager.BORDER_DIVIDER));

        statusIcon = new JLabel(new DotIcon(ColorManager.STATUS_READY));
        statusIcon.setBorder(BorderFactory.createEmptyBorder(0, SpacingSystem.LG, 0, 0));

        statusLabel = createLabel("Ready");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, SpacingSystem.XS, 0, 0));

        pathLabel = createLabel("");
        pathLabel.setForeground(ColorManager.TEXT_MUTED);

        positionLabel = createLabel("Ln 1, Col 1");
        encodingLabel = createLabel("UTF-8");
        languageLabel = createLabel("Plain Text");
        gitLabel = createLabel("main");
        projectLabel = createLabel("EyeCode");

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        left.setOpaque(false);
        left.add(statusIcon);
        left.add(statusLabel);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        center.setOpaque(false);
        center.add(pathLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        right.setOpaque(false);
        right.add(positionLabel);
        right.add(createSeparator());
        right.add(encodingLabel);
        right.add(createSeparator());
        right.add(languageLabel);
        right.add(createSeparator());
        right.add(gitLabel);
        right.add(createSeparator());
        right.add(projectLabel);

        add(left, BorderLayout.WEST);
        add(center, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
    }

    public void setProjectRoot(File root) {
        this.projectRoot = root;
        detectGitBranch();
    }

    public void updateCaretPosition(int line, int column) {
        positionLabel.setText("Ln " + line + ", Col " + column);
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
        projectLabel.setText(projectName == null || projectName.isBlank() ? "EyeCode" : projectName);
    }

    public void updateStatus(String status) {
        statusLabel.setText(status == null || status.isBlank() ? "Ready" : status);
    }

    public void setRunning(boolean running) {
        if (running) {
            statusLabel.setText("Running");
            setDotColor(ColorManager.STATUS_BUSY);
        } else {
            statusLabel.setText("Ready");
            setDotColor(ColorManager.STATUS_READY);
        }
    }

    private void setDotColor(Color color) {
        currentDotColor = color;
        statusIcon.setIcon(new DotIcon(color));
        statusIcon.repaint();
    }

    private void detectGitBranch() {
        if (projectRoot == null) return;
        try {
            File headFile = new File(projectRoot, ".git/HEAD");
            if (headFile.exists()) {
                String content = new String(java.nio.file.Files.readAllBytes(headFile.toPath())).trim();
                if (content.startsWith("ref: refs/heads/")) {
                    gitLabel.setText(content.substring(16));
                } else {
                    gitLabel.setText(content.substring(0, Math.min(7, content.length())));
                }
                return;
            }
        } catch (Exception ignored) {}
        gitLabel.setText("main");
    }

    private void updateLanguage(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            languageLabel.setText("Plain Text");
            return;
        }
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        languageLabel.setText(switch (extension) {
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
        sep.setBorder(BorderFactory.createEmptyBorder(0, SpacingSystem.LG, 0, SpacingSystem.LG));
        return sep;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(ColorManager.TEXT_SECONDARY);
        label.setFont(TypographyManager.UI_STATUS());
        return label;
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
