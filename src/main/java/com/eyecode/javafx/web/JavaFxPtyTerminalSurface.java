package com.eyecode.javafx.web;

import com.eyecode.terminal.TerminalPanel;
import com.eyecode.terminal.TerminalStartupTrace;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.scene.layout.Pane;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.Objects;

public final class JavaFxPtyTerminalSurface extends Pane {
    private final SwingNode swingNode = new SwingNode();
    private TerminalPanel terminalPanel;
    private Path workspaceDirectory;
    private boolean terminalRequested;
    private boolean disposed;
    private long workspaceGeneration;
    private TerminalStartupTrace startupTrace;

    public JavaFxPtyTerminalSurface() {
        setPickOnBounds(false);
        swingNode.setManaged(false);
        swingNode.setVisible(false);
        getChildren().add(swingNode);
    }

    public void setWorkspaceDirectory(Path directory) {
        if (disposed) return;
        Path normalized = directory == null ? null : directory.toAbsolutePath().normalize();
        if (Objects.equals(workspaceDirectory, normalized)) return;
        workspaceDirectory = normalized;
        workspaceGeneration++;
        stopAndDetachTerminal();
        if (workspaceDirectory == null) {
            swingNode.setVisible(false);
        } else if (terminalRequested) {
            trace().mark("workspace availability detected");
            swingNode.setVisible(true);
            startForCurrentWorkspace(trace());
        }
    }

    public void showTerminal() {
        if (disposed) return;
        startupTrace = new TerminalStartupTrace();
        startupTrace.mark("terminal/show received");
        terminalRequested = true;
        if (workspaceDirectory == null) return;
        startupTrace.mark("workspace availability detected");
        swingNode.setVisible(true);
        startForCurrentWorkspace(startupTrace);
    }

    public void hideTerminal() {
        terminalRequested = false;
        swingNode.setVisible(false);
    }

    public void restartTerminal() {
        if (disposed || workspaceDirectory == null) return;
        terminalRequested = true;
        workspaceGeneration++;
        stopAndDetachTerminal();
        swingNode.setVisible(true);
        startupTrace = new TerminalStartupTrace();
        startupTrace.mark("terminal/restart received");
        startupTrace.mark("workspace availability detected");
        startForCurrentWorkspace(startupTrace);
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
        workspaceGeneration++;
        workspaceDirectory = null;
        terminalRequested = false;
        stopAndDetachTerminal();
        getChildren().clear();
    }

    private void startForCurrentWorkspace(TerminalStartupTrace trace) {
        Path directory = workspaceDirectory;
        long generation = workspaceGeneration;
        if (directory == null || terminalPanel != null) return;
        trace.mark("surface decided to create terminal");
        SwingUtilities.invokeLater(() -> {
            if (disposed || !terminalRequested || terminalPanel != null
                    || generation != workspaceGeneration || !directory.equals(workspaceDirectory)) return;
            trace.mark("EDT execution started");
            terminalPanel = new TerminalPanel(directory, trace);
            swingNode.setContent(terminalPanel);
            trace.mark("SwingNode.setContent complete");
            Platform.runLater(() -> trace.mark("native terminal ready for FX render"));
        });
    }

    private TerminalStartupTrace trace() {
        if (startupTrace == null) startupTrace = new TerminalStartupTrace();
        return startupTrace;
    }

    private void stopAndDetachTerminal() {
        TerminalPanel panel = terminalPanel;
        terminalPanel = null;
        if (panel == null) return;
        SwingUtilities.invokeLater(() -> {
            panel.stopTerminal();
            swingNode.setContent(null);
        });
    }
}