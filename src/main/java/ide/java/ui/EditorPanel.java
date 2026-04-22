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
    private Style numberStyle;
    private Style methodStyle;

    private final String[] keywords = {
            "public", "class", "static", "void",
            "if", "else", "for", "while", "return",
            "new", "int", "double", "String", "boolean"
    };

    // Representa o arquivo atual em memoria (model)
    private Document document;

    // Callback usado para notificar as mudancas
    private Runnable onChangeCallback;

    private boolean isUpdating = false;

    public EditorPanel() {
        setLayout(new BorderLayout());

        textPane = new JTextPane();
        doc = textPane.getStyledDocument();

        setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(textPane);
        LineNumberPanel lineNumbers = new LineNumberPanel(textPane);
        scrollPane.setRowHeaderView(lineNumbers);
        add(scrollPane, BorderLayout.CENTER);



        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            lineNumbers.repaint();
        });

        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                lineNumbers.repaint();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                lineNumbers.repaint();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                lineNumbers.repaint();
            }
        });

        keywordStyle = doc.addStyle("Keyword", null);
        StyleConstants.setForeground(keywordStyle, new Color(204, 120, 50));
        StyleConstants.setBold(keywordStyle, true);

        normalStyle = doc.addStyle("Normal", null);
        StyleConstants.setForeground(normalStyle, new Color(169, 183, 198));

        stringStyle = doc.addStyle("String", null);
        StyleConstants.setForeground(stringStyle, new Color(106, 135, 89));

        commentStyle = doc.addStyle("Comment", null);
        StyleConstants.setForeground(commentStyle, new Color(128, 128, 128));

        numberStyle = doc.addStyle("Number", null);
        StyleConstants.setForeground(numberStyle, new Color(104,151,187));

        methodStyle = doc.addStyle("Method", null);
        StyleConstants.setForeground(methodStyle, new Color(255,198,109));

        applyDarkTheme();

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
               if (isUpdating) return;

               SwingUtilities.invokeLater(() -> {
                   if (isUpdating) return;

                   isUpdating = true;

                   try {
                       highlight();
                       updateDocument();
                   } finally {
                       isUpdating = false;
                   }
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

    private void highlight(){
        String text = textPane.getText();

        doc.setCharacterAttributes(0, text.length(), normalStyle, true);

        highlightComments(text);
        highlightStrings(text);
        highlightKeywords(text);
        highlightNumbers(text);
        highlightMethods(text);
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
        Pattern pattern = Pattern.compile("(.*?)");
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

    private void highlightNumbers(String text){
        Pattern pattern = Pattern.compile("\\b\\d+\\b");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()){
            doc.setCharacterAttributes(
                    matcher.start(),
                    matcher.end() - matcher.start(),
                    numberStyle,
                    false
            );
        }
    }

    private void highlightMethods(String text){
        Pattern pattern = Pattern.compile("\\b\\w+(?=\\()");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()){
            doc.setCharacterAttributes(
                    matcher.start(),
                    matcher.end() - matcher.start(),
                    methodStyle,
                    false
            );
        }
    }


    private void applyDarkTheme(){
        textPane.setBackground(new Color(43, 43 ,43));
        textPane.setForeground(new Color(169, 183, 198));
        textPane.setCaretColor(Color.WHITE);
        textPane.setSelectionColor(new Color(33, 66, 131));
    }

    public Runnable getOnChangeCallback() {
        return onChangeCallback;
    }

    public void setOnChangeCallback(Runnable onChangeCallback) {
        this.onChangeCallback = onChangeCallback;
    }

    public void setDocument(Document document){
        this.document = document;

        isUpdating = true;

        try {
            textPane.setText(document.getContent());highlight();
        }finally {
            isUpdating = false;
        }

    }
    public Document getDocument(){
        return document;
    }
}
