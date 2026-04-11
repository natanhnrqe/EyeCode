package ide.java.ui;

import ide.java.editor.Document;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class EditorPanel extends JPanel {

    private JTextArea textArea;
    private Document document;
    private Runnable onChangeCallback;

    public EditorPanel() {

        setLayout(new BorderLayout());

        textArea = new JTextArea();
        textArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        textArea.setBackground(Color.DARK_GRAY);
        textArea.setForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(textArea);

        add(scrollPane, BorderLayout.CENTER);

        setupDocumentListener();

    }

    public void setDocument(Document document){
        this.document = document;
        textArea.setText(document.getContent());
    }

    public Document getDocument(){
        return document;
    }

    private void setupDocumentListener(){
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateDocument();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateDocument();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateDocument();
            }
        });
    }

    private void updateDocument(){
        if (document != null){
            document.setContent(textArea.getText());

            if (onChangeCallback != null){
                onChangeCallback.run();
            }
        }

    }

    public JTextArea getTextArea() {
        return textArea;
    }

    public Runnable getOnChangeCallback() {
        return onChangeCallback;
    }

    public void setOnChangeCallback(Runnable onChangeCallback) {
        this.onChangeCallback = onChangeCallback;
    }
}
