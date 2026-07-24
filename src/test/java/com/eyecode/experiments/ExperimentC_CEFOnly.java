package com.eyecode.experiments;

import com.eyecode.learning.browser.LearningChromiumCard;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public final class ExperimentC_CEFOnly {

    public static void main(String[] args) {
        try {
            Class.forName("com.formdev.flatlaf.FlatDarkLaf");
            javax.swing.UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Exp C — CEF Alone (no hover)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 700);
            frame.setLayout(new BorderLayout());

            /* — Editor pane for diagnostics (shows what events happen) — */
            JTextPane logPane = new JTextPane();
            logPane.setEditable(false);
            frame.add(new JScrollPane(logPane), BorderLayout.WEST);

            /* — CEF card — */
            LearningChromiumCard cefCard = new LearningChromiumCard();
            cefCard.setPreferredSize(new java.awt.Dimension(500, 600));
            frame.add(cefCard, BorderLayout.CENTER);

            /* — Instrument EVERYTHING — */

            // 1. mouseMoved on CEF's parent
            cefCard.addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    log("CEF mouseMoved: (" + e.getX() + "," + e.getY() + ")");
                }
            });

            // 2. MouseWheel on CEF
            cefCard.addMouseWheelListener(e -> {
                log("CEF MouseWheel: rotation=" + e.getWheelRotation() + " consumed=" + e.isConsumed());
            });

            // 3. Focus on CEF
            cefCard.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    log("CEF focusGained");
                }
                @Override
                public void focusLost(FocusEvent e) {
                    log("CEF focusLost");
                }
            });

            // 4. Hierarchy changes
            cefCard.addHierarchyListener(e -> {
                log("CEF hierarchyChanged: " + e.getChangeFlags());
            });

            // 5. Component moved/resized
            cefCard.addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    log("CEF resized");
                }
                @Override
                public void componentMoved(java.awt.event.ComponentEvent e) {
                    log("CEF moved");
                }
                @Override
                public void componentShown(java.awt.event.ComponentEvent e) {
                    log("CEF shown");
                }
            });

            // Load content
            cefCard.loadHtml("<html><body><h1>CEF Test</h1><p>This is rendered by Chromium.</p></body></html>");

            /* — Log display writer — */
            var doc = logPane.getDocument();
            java.util.function.Consumer<String> logger = msg -> {
                try {
                    String line = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
                            + " " + msg + "\n";
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        try {
                            doc.insertString(doc.getLength(), line, null);
                            logPane.setCaretPosition(doc.getLength());
                        } catch (Exception ignored) {}
                    });
                } catch (Exception ignored) {}
            };

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Initial load
            cefCard.loadHtml("""
                    <html>
                    <body style="background:#1e1e1e;color:#ccc;padding:32px">
                    <h1>CEF Experiment</h1>
                    <p style="font-size:18px">Move mouse over this area.<br/>Watch the log panel on the left.</p>
                    </body>
                    </html>
                    """);

            System.out.println("=== EXP C READY ===");
            System.out.println("Watch the log panel for synthetic mouseMoved events.");
        });
    }

    private static void log(String msg) {
        System.out.println("[CEF] " + msg);
    }
}
