package com.eyecode.javafx.web;

import com.eyecode.terminal.TerminalPanel;
import javafx.embed.swing.SwingNode;
import javafx.scene.layout.Pane;

import javax.swing.SwingUtilities;
import java.nio.file.Path;

public final class JavaFxPtyTerminalSurface extends Pane {
    private final SwingNode swingNode = new SwingNode();
    private TerminalPanel terminalPanel;
    private Path workingDirectory;
    private boolean disposed;

    public JavaFxPtyTerminalSurface() {
        setPickOnBounds(false);
        swingNode.setManaged(false);
        swingNode.setVisible(false);
        getChildren().add(swingNode);
    }

    public void showTerminal(Path directory) {
        if (disposed || directory == null) return;
        Path normalized = directory.toAbsolutePath().normalize();
        boolean restart = terminalPanel == null || !normalized.equals(workingDirectory);
        workingDirectory = normalized;
        if (restart) SwingUtilities.invokeLater(() -> {
            if (disposed) return;
            if (terminalPanel == null) {
                terminalPanel = new TerminalPanel(normalized);
                swingNode.setContent(terminalPanel);
            } else {
                terminalPanel.restart(normalized);
            }
        });
        swingNode.setVisible(true);
    }

    public void hideTerminal() {
        swingNode.setVisible(false);
    }

    public void restartTerminal(Path directory) {
        if (disposed || directory == null) return;
        workingDirectory = directory.toAbsolutePath().normalize();
        SwingUtilities.invokeLater(() -> {
            if (disposed) return;
            if (terminalPanel == null) {
                terminalPanel = new TerminalPanel(workingDirectory);
                swingNode.setContent(terminalPanel);
            } else {
                terminalPanel.restart(workingDirectory);
            }
        });
        swingNode.setVisible(true);
    }

    public void stopTerminal() {
        SwingUtilities.invokeLater(() -> {
            if (terminalPanel != null) terminalPanel.stopTerminal();
        });
    }

    public void updateBounds(double x, double y, double width, double height) {
        if (disposed) return;
        swingNode.resizeRelocate(x, y, Math.max(0, width), Math.max(0, height));
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        SwingUtilities.invokeLater(() -> {
            if (terminalPanel != null) terminalPanel.stopTerminal();
            terminalPanel = null;
            swingNode.setContent(null);
        });
        getChildren().clear();
    }
}
