package com.eyecode.editor.v2.ui;

import com.eyecode.editor.v2.EditorBuffer;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;

public final class EditorView extends JPanel {

    private final EditorBuffer buffer;
    private final JTextArea textArea;
    private final JScrollPane scrollPane;
    private boolean refreshing;

    public EditorView(EditorBuffer buffer) {
        super(new BorderLayout());
        this.buffer = buffer;
        this.textArea = new JTextArea();
        this.scrollPane = new JScrollPane(textArea);

        textArea.setText(buffer.getDocument().getText());
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                syncToDocument();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                syncToDocument();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                syncToDocument();
            }
        });

        add(scrollPane, BorderLayout.CENTER);
    }

    public void refreshFromDocument() {
        refreshing = true;
        try {
            textArea.setText(buffer.getDocument().getText());
        } finally {
            refreshing = false;
        }
    }

    public EditorBuffer getBuffer() {
        return buffer;
    }

    public JTextArea getTextArea() {
        return textArea;
    }

    private void syncToDocument() {
        if (!refreshing) {
            buffer.getDocument().setText(textArea.getText());
        }
    }
}
