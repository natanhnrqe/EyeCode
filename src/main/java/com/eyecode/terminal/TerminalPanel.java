package com.eyecode.terminal;

import com.eyecode.ui.EyeCodeTerminalSettings;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.ui.JediTermWidget;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;

import javax.swing.JPanel;
import java.awt.BorderLayout;
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
        setLayout(new BorderLayout());
        restart(workingDirectory);
    }

    public void restart(Path directory) {
        stopTerminal();
        workingDirectory = directory.toAbsolutePath().normalize();
        try {
            process = new PtyProcessBuilder(shellCommand())
                    .setEnvironment(System.getenv())
                    .setDirectory(workingDirectory.toString())
                    .start();
            terminal = new JediTermWidget(80, 20, new EyeCodeTerminalSettings());
            TtyConnector connector = new PtyProcessTerminalConnector(process, StandardCharsets.UTF_8);
            terminal.setTtyConnector(connector);
            add(terminal, BorderLayout.CENTER);
            terminal.start();
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
}
