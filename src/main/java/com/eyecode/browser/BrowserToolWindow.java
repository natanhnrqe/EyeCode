package com.eyecode.browser;

import com.eyecode.ui.ToolWindowButton;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.UIConstants;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

public final class BrowserToolWindow extends JPanel {

    private static final int BROWSER_WIDTH = 480;

    private final BrowserPanel browserPanel;
    private final ToolWindowButton browserButton;
    private final JPanel browserWrapper;
    private boolean open;

    public BrowserToolWindow(BrowserPanel browserPanel) {
        super(new BorderLayout());
        this.browserPanel = browserPanel;
        setOpaque(false);

        JPanel rightBar = new JPanel();
        rightBar.setLayout(new BoxLayout(rightBar, BoxLayout.Y_AXIS));
        rightBar.setOpaque(false);
        rightBar.setPreferredSize(new Dimension(UIConstants.TOOLWINDOW_WIDTH, 0));
        rightBar.setMinimumSize(new Dimension(UIConstants.TOOLWINDOW_WIDTH, 0));
        rightBar.setBorder(BorderFactory.createEmptyBorder(SpacingSystem.MD, 0, SpacingSystem.MD, 0));

        browserButton = new ToolWindowButton(null, "Browser");
        browserButton.addActionListener(e -> toggle());
        rightBar.add(browserButton);
        rightBar.add(Box.createVerticalGlue());

        browserWrapper = new JPanel(new BorderLayout());
        browserWrapper.setOpaque(false);
        browserWrapper.add(browserPanel, BorderLayout.CENTER);
        browserWrapper.setPreferredSize(new Dimension(BROWSER_WIDTH, 0));
        browserWrapper.setMinimumSize(new Dimension(200, 0));
        browserWrapper.setVisible(false);

        add(rightBar, BorderLayout.WEST);
        add(browserWrapper, BorderLayout.CENTER);
    }

    public void toggle() {
        open = !open;
        browserWrapper.setVisible(open);
        browserButton.setSelectedState(open);
        revalidate();
        repaint();
    }

    public void open() {
        if (!open) toggle();
    }

    public void close() {
        if (open) toggle();
    }

    public boolean isOpen() {
        return open;
    }

    public void openIfNecessary() {
        if (!open) toggle();
    }
}
