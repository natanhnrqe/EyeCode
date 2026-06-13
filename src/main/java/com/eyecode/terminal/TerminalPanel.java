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

            applyScrollBar(terminal);

            add(terminal, BorderLayout.CENTER);


            setBackground(new java.awt.Color(
                    25,
                    26,
                    28
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

    private void printComponents(Component component) {

        System.out.println(
                component.getClass().getName()
        );

        if (component instanceof Container container) {

            for (Component child :
                    container.getComponents()) {

                printComponents(child);
            }
        }
    }

    private void applyScrollBar(Component component) {

        if (component instanceof JScrollBar bar) {

            bar.setUI(
                    new ModernScrollBarUI()
            );
        }

        if (component instanceof Container container) {

            for (Component child :
                    container.getComponents()) {

                applyScrollBar(child);
            }
        }
    }



}
