package ide.java.ui;

import ide.java.editor.Document;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class EditorPanel extends JPanel {

    // Componente principal da edicao de texto
    private JTextArea textArea;

    // Representa o arquivo atual em memoria (model)
    private Document document;

    // Callback usado para notificar as mudancas
    private Runnable onChangeCallback;

    public EditorPanel() {
        setLayout(new BorderLayout());

        textArea = new JTextArea();
        textArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        textArea.setBackground(Color.DARK_GRAY);
        textArea.setForeground(Color.WHITE);

        // Adiciona scroll automaticamente ao editor
        JScrollPane scrollPane = new JScrollPane(textArea);

        add(scrollPane, BorderLayout.CENTER);

        // Listener que detecta QUALQUER mudanca no texto
        setupDocumentListener();

    }

    public void setDocument(Document document){
        this.document = document;

        // Atualiza a UI com o conteudo do Document
        // Isso acontece ao abrir o arquivo ou trocar de aba
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

    // Sincroniza o conteudo da UI com o Documente
    private void updateDocument(){
        if (document != null){
            // Atualiza o conteudo do model com o texto atual do editor
            document.setContent(textArea.getText());

            // Notifica outras partes do sistema (ex: adicionar "*" na aba)
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
