package com.eyecode.experiments;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.ui.RichEditorView;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class ExperimentD_RichEditorNoHover {

    public static void main(String[] args) {
        RichEditorView.SKIP_HOVER = true;

        try {
            Class.forName("com.formdev.flatlaf.FlatDarkLaf");
            javax.swing.UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Exp D — RichEditorView without Hover");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);

            EditorDocument doc = new EditorDocument();
            doc.setText("""
                    interface MyInterface {
                        void doSomething();
                    }

                    class HelloWorld implements MyInterface {
                        public void doSomething() {
                            System.out.println("hello");
                        }
                    }
                    """);
            EditorBuffer buffer = new EditorBuffer(doc);

            RichEditorView editor = new RichEditorView(buffer);
            frame.add(editor);

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            System.out.println("=== EXP D READY (RichEditor, NO hover) ===");
            System.out.println("1. Does scrolling work?");
            System.out.println("2. Does caret movement work?");
            System.out.println("3. Focus events?");
        });
    }
}
