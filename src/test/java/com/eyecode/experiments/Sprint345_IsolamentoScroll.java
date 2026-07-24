package com.eyecode.experiments;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.caret.CaretSynchronizationManager;
import com.eyecode.editor.v2.ui.RichEditorView;
import com.eyecode.learning.ui.SwingLearningHoverSurface;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Point;

/**
 * Sprint 34.5 — Isolamento do Scroll
 *
 * Use as flags estáticas para desligar componentes um a um e testar o scroll.
 *
 * Flags disponíveis:
 *   RichEditorView.SKIP_HOVER          — desliga hover registration
 *   CaretSynchronizationManager.SYNC_ENABLED  — desliga sync de caret
 *
 * Testes sugeridos:
 *   T1: SKIP_HOVER=true  (sem hover listeners no textPane)
 *   T2: SYNC_ENABLED=false (sem caret sync)
 *   T3: Ambos false
 *   T4: Dump do viewport (já incluso abaixo)
 */
public final class Sprint345_IsolamentoScroll {

    public static void main(String[] args) {

        // ============ CONFIGURAÇÃO DOS TESTES ============

        // T1: true → não cria hover surface
        boolean testSkipHover = false;

        // T1b: false → surface existe mas não registra listeners no textPane
        boolean testSkipListeners = false;

        // T2: false → caret sync desligado
        boolean testSkipCaretSync = true;

        // ============ APLICA FLAGS ============
        if (testSkipHover) {
            RichEditorView.SKIP_HOVER = true;
            System.out.println("[CFG] Hover surface não será criada (SKIP_HOVER)");
        }
        if (testSkipListeners) {
            SwingLearningHoverSurface.REGISTER_LISTENERS = false;
            System.out.println("[CFG] Hover surface criada, mas sem listeners no textPane");
        }
        if (testSkipCaretSync) {
            CaretSynchronizationManager.SYNC_ENABLED = false;
            System.out.println("[CFG] CaretSync desligado");
        }

        // ============ LAF ============
        try {
            Class.forName("com.formdev.flatlaf.FlatDarkLaf");
            javax.swing.UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {

            // ============ CRIA EDITOR ============
            EditorDocument doc = new EditorDocument();
            doc.setText("""
                    interface MyInterface {
                        void doSomething();
                    }

                    class HelloWorld implements MyInterface {
                        private int x;

                        public HelloWorld() {
                            this.x = 42;
                        }

                        public void doSomething() {
                            System.out.println("hello world");
                            for (int i = 0; i < 10; i++) {
                                System.out.println("line " + i);
                            }
                        }
                    }

                    class AnotherClass {
                        // filler to make the file scrollable
                        private String a, b, c, d, e, f, g, h, i, j;
                        private String k, l, m, n, o, p, q, r, s, t;
                        private String u, v, w, x, y, z;
                    }
                    """);

            EditorBuffer buffer = new EditorBuffer(doc);
            RichEditorView editor = new RichEditorView(buffer);

            JFrame frame = new JFrame("Sprint 34.5 — Isolamento Scroll");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.add(editor);

            // ============ DUMP DO VIEWPORT (após mostrar) ============
            SwingUtilities.invokeLater(() -> {
                JTextPane textPane = editor.getTextPane();
                JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, textPane);

                if (scrollPane != null) {
                    var viewport = scrollPane.getViewport();
                    System.out.println("========== DUMP VIEWPORT ==========");
                    System.out.println("textPane.getPreferredSize() = " + textPane.getPreferredSize());
                    System.out.println("textPane.getSize()          = " + textPane.getSize());
                    System.out.println("viewport.getViewSize()     = " + viewport.getViewSize());
                    System.out.println("viewport.getViewPosition() = " + viewport.getViewPosition());
                    System.out.println("viewport.getExtentSize()   = " + viewport.getExtentSize());
                    System.out.println("scrollPane.isValid()       = " + scrollPane.isValid());
                    System.out.println("textPane.getText().length()= " + textPane.getDocument().getLength());
                    System.out.println("===================================");

                    // T3: Forçar viewport manualmente
                    System.out.println(">>> Forçando viewport para (0, 100)...");
                    viewport.setViewPosition(new Point(0, 100));
                    System.out.println(">>> Nova posição: " + viewport.getViewPosition());

                    SwingUtilities.invokeLater(() -> {
                        System.out.println(">>> Verificação atrasada: " + viewport.getViewPosition());
                        System.out.println(">>> Teste de scroll com a roda do mouse agora.");
                    });
                } else {
                    System.out.println("[ERRO] JScrollPane não encontrado");
                }
            });

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            System.out.println("=== SPRINT 34.5 READY ===");
            System.out.println("T1 skipHover=" + testSkipHover
                    + " | T1b skipListeners=" + testSkipListeners
                    + " | T2 skipCaretSync=" + testSkipCaretSync);
            System.out.println("1. Gire a roda do mouse para testar scroll");
            System.out.println("2. Observe se o viewport se moveu (dump acima)");
        });
    }
}
