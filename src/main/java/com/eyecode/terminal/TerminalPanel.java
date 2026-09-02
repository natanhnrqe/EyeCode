package com.eyecode.terminal;

import com.eyecode.ui.EyeCodeTerminalSettings;
import com.eyecode.ui.TerminalTheme;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.ui.JediTermWidget;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;

import javax.swing.JScrollBar;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public final class TerminalPanel extends JPanel {
    private JediTermWidget terminal;
    private PtyProcess process;
    private Path workingDirectory;

    public TerminalPanel() {
        this(Path.of(System.getProperty("user.dir")));
    }

    public TerminalPanel(Path workingDirectory) {
        this(workingDirectory, null);
    }

    public TerminalPanel(Path workingDirectory, TerminalStartupTrace startupTrace) {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(TerminalTheme.SWING_BACKGROUND);
        if (startupTrace != null) startupTrace.mark("TerminalPanel construction");
        restart(workingDirectory, startupTrace);
    }

    public void restart(Path directory) {
        restart(directory, null);
    }

    private void restart(Path directory, TerminalStartupTrace startupTrace) {
        stopTerminal();
        workingDirectory = directory.toAbsolutePath().normalize();
        try {
            if (startupTrace != null) startupTrace.mark("PtyProcess creation start");
            process = new PtyProcessBuilder(shellCommand())
                    .setEnvironment(System.getenv())
                    .setDirectory(workingDirectory.toString())
                    .start();
            if (startupTrace != null) startupTrace.mark("PtyProcess creation complete");
            terminal = new JediTermWidget(80, 20, new EyeCodeTerminalSettings());
            if (startupTrace != null) startupTrace.mark("JediTermWidget creation complete");
            TtyConnector connector = new PtyProcessTerminalConnector(process, StandardCharsets.UTF_8);
            terminal.setTtyConnector(connector);
            add(terminal, BorderLayout.CENTER);
            terminal.start();
            applyTheme(terminal);
            if (startupTrace != null) startupTrace.mark("connector and widget initialization complete");
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to start terminal", exception);
        }
        revalidate();
        repaint();
    }

    public void stopTerminal() {
        if (terminal != null) {
            terminal.stop();
            terminal.close();
            remove(terminal);
            terminal = null;
        }
        if (process != null) {
            process.destroyForcibly();
            process = null;
        }
    }

    public Path workingDirectory() {
        return workingDirectory;
    }

    private static String[] shellCommand() {
        return System.getProperty("os.name", "").toLowerCase().contains("win")
                ? new String[] {"powershell.exe", "-NoLogo"}
                : new String[] {"sh", "-l"};
    }

    private void applyTheme(Component component) {
        component.setBackground(TerminalTheme.SWING_BACKGROUND);
        if (component instanceof JScrollBar scrollBar) {
            scrollBar.setUI(new EyeCodeTerminalScrollBarUI());
            scrollBar.setBackground(TerminalTheme.SWING_BACKGROUND);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyTheme(child);
            }
        }
    }
}