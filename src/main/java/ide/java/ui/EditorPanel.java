package ide.java.ui;

import ide.java.editor.Document;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditorPanel extends JPanel {

    // Componente principal da edicao de texto
    private JTextPane textPane;
    private StyledDocument doc;
    private Style keywordStyle;
    private Style normalStyle;
    private Style stringStyle;
    private Style commentStyle;

    private final String[] keywords = {
            "public", "class", "static", "void",
            "if", "else", "for", "while", "return",
            "new", "int", "double", "String", "boolean"
    };

    // Representa o arquivo atual em memoria (model)
    private Document document;

    // Callback usado para notificar as mudancas
    private Runnable onChangeCallback;

    public EditorPanel() {
        setLayout(new BorderLayout());

        textPane = new JTextPane();
        doc = textPane.getStyledDocument();

        setLayout(new BorderLayout());
        add(new JScrollPane(textPane), BorderLayout.CENTER);

        keywordStyle = doc.addStyle("Keyword", null);
        StyleConstants.setForeground(keywordStyle, Color.BLUE);
        StyleConstants.setBold(keywordStyle, true);

        normalStyle = doc.addStyle("Normal", null);
        StyleConstants.setForeground(normalStyle, Color.WHITE);

        stringStyle = doc.addStyle("String", null);
        StyleConstants.setForeground(normalStyle, new Color(0, 200, 0));

        commentStyle = doc.addStyle("Comment", null);
        StyleConstants.setForeground(commentStyle, Color.GRAY);

        // Listener que detecta QUALQUER mudanca no texto
        setupDocumentListener();

    }

    private void setupDocumentListener(){
        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            private void update(){
                SwingUtilities.invokeLater(() -> {
                    highlight();
                    updateDocument();
                });
            }
        });
    }

    // Sincroniza o conteudo da UI com o Documente
    private void updateDocument(){
        if (document != null){
            // Atualiza o conteudo do model com o texto atual do editor
            document.setContent(textPane.getText());

            // Notifica outras partes do sistema (ex: adicionar "*" na aba)
            if (onChangeCallback != null){
                onChangeCallback.run();
            }
        }

    }
    private void highlightKeywords(String text){
        for (String keyword : keywords) {
            Pattern pattern = Pattern.compile("\\b" + keyword + "\\b");
            Matcher matcher = pattern.matcher(text);

            while (matcher.find()){
                doc.setCharacterAttributes(
                        matcher.start(),
                        matcher.end() - matcher.start(),
                        keywordStyle,
                        false
                );
            }
        }
    }

    private void highlightStrings(String text){
        Pattern pattern = Pattern.compile("\"(.*?)\"");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()){
            doc.setCharacterAttributes(
                    matcher.start(),
                    matcher.end() - matcher.start(),
                    stringStyle,
                    false
            );
        }
    }

    private void highlightComments(String text){
        Pattern pattern = Pattern.compile("//.*");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()){
            doc.setCharacterAttributes(
                    matcher.start(),
                    matcher.end() - matcher.start(),
                    commentStyle,
                    false
            );
        }
    }

    private void highlight(){
        String text = textPane.getText();

        doc.setCharacterAttributes(0, text.length(), normalStyle, true);

        highlightKeywords(text);
        highlightStrings(text);
        highlightComments(text);
    }

    public Runnable getOnChangeCallback() {
        return onChangeCallback;
    }

    public void setOnChangeCallback(Runnable onChangeCallback) {
        this.onChangeCallback = onChangeCallback;
    }

    public void setDocument(Document document){
        this.document = document;

        textPane.setText(document.getContent());
        highlight();
    }

    public Document getDocument(){
        return document;
    }
}
