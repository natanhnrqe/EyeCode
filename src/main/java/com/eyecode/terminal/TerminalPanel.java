package com.eyecode.terminal;

import com.eyecode.ui.EyeCodeTerminalSettings;
import com.eyecode.ui.scroll.ModernScrollBarUI;
import com.jediterm.pty.PtyProcessTtyConnector;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.ui.JediTermWidget;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;

import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;

public class TerminalPanel extends JPanel {

    private JediTermWidget terminal;

    public TerminalPanel() {

        setLayout(new BorderLayout());

        JScrollPane scrollPane = new JScrollPane(terminal);

        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());

        scrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());

        startTerminal();
    }

    private void startTerminal(){

        try {

            /**
             * Executando o shell.
             */
            String[] command = {"powershell.exe"};

            /**
             * Cria PTY(pseudoterminal).
             */
            PtyProcess process =
                    new PtyProcessBuilder(command).
                            setEnvironment(System.getenv())
                            .setDirectory(
                                    System.getProperty("user.dir")
                            )
                            .start();

            /**
             * Cria o terminal.
             */
            terminal = new JediTermWidget(80, 20, new EyeCodeTerminalSettings());

            TtyConnector connector = new PtyProcessTtyConnector(
                    process, StandardCharsets.UTF_8
            );

            terminal.setTtyConnector(connector);

            terminal.start();

            add(terminal, BorderLayout.CENTER);

            setBackground(new java.awt.Color(
                    30,
                    30,
                    30
            ));

            terminal.getComponent().setBorder(
                    BorderFactory.createEmptyBorder(
                            5,
                            5,
                            5,
                            5
                    )
            );




        } catch (Exception e) {
            e.printStackTrace();
        }
    }




}
