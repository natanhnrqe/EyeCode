package ide.java.ui.editor;

import ide.java.editor.Document;
import ide.java.ui.LineNumberPanel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private Object currentLineHighlight;
    private LineNumberPanel lineNumbers;
    private final List<Object> breakpointHighlights = new ArrayList<>();
    private JPopupMenu popup;
    private JList<Suggestion> suggestionList;
    private JWindow autocompleteWindow;


    private static final String[] keywords = {
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

        Action defaultEnter = textPane.getActionMap().get(DefaultEditorKit.insertBreakAction);
        Action defaultTab = textPane.getActionMap().get(DefaultEditorKit.insertTabAction);

        initAutocompleteWindow();

        InputMap im = textPane.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = textPane.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "tab-custom");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter-custom");

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "autocomplete-down");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "autocomplete-up");

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "autocomplete-close");

        am.put("enter-custom", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (isPopupVisible() && suggestionList.getSelectedValue() != null) {
                    Suggestion selected = suggestionList.getSelectedValue();
                    insertSuggestion(selected.getText());
                    return;
                }

                handleEnter();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "tab-custom");

        am.put("tab-custom", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (isPopupVisible() && suggestionList.getSelectedValue() != null) {
                    Suggestion selected = suggestionList.getSelectedValue();
                    insertSuggestion(selected.getText());
                    return;
                }

                try {
                    int caret = textPane.getCaretPosition();
                    javax.swing.text.Document doc = textPane.getDocument();

                    Element root = doc.getDefaultRootElement();
                    int line = root.getElementIndex(caret);
                    Element lineEl = root.getElement(line);

                    int start = lineEl.getStartOffset();

                    doc.insertString(start, "    ", null);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        am.put("autocomplete-down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isPopupVisible()){
                    int i = suggestionList.getSelectedIndex();

                    if (i < suggestionList.getModel().getSize() - 1){
                        suggestionList.setSelectedIndex(i + 1);
                        suggestionList.ensureIndexIsVisible(i + 1);
                    }
                }
            }
        });

        am.put("autocomplete-up", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isPopupVisible()){
                    int i = suggestionList.getSelectedIndex();

                    if (i > 0){
                        suggestionList.setSelectedIndex(i - 1);
                        suggestionList.ensureIndexIsVisible(i - 1);
                    }
                }
            }
        });

        am.put("autocomplete-close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hideAutocomplete();
            }
        });

        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2){
                    Suggestion selected = suggestionList.getSelectedValue();
                    insertSuggestion(selected.getText());
                }
            }
        });

        textPane.setMargin(new Insets(0,0,0,0));
        lineNumbers = new LineNumberPanel(textPane);

        lineNumbers.setListener(this::updateBreakpointHighlights);


        textPane.addCaretListener(e -> {
            highlightCurrentLine();
            int offset = textPane.getCaretPosition();
            Element root = textPane.getDocument().getDefaultRootElement();
            int line = root.getElementIndex(offset);

            lineNumbers.setCurrentLine(line);
        });

        doc = textPane.getStyledDocument();

        setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(textPane);

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

        suggestionList.setBackground(new Color(43, 43, 43));
        suggestionList.setForeground(new Color(169, 183, 198));
        suggestionList.setSelectionBackground(new Color(75, 110, 175));
        suggestionList.setSelectionForeground(Color.WHITE);

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

    private void initAutocompleteWindow() {
        autocompleteWindow = new JWindow();

        suggestionList = new JList<>();
        suggestionList.setCellRenderer(new SuggestionRenderer());

        JScrollPane scroll = new JScrollPane(suggestionList);
        scroll.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        autocompleteWindow.add(scroll);
        autocompleteWindow.setSize(320, 180);
    }

    private String getCurrentWord() throws BadLocationException{
        int caret = textPane.getCaretPosition();
        javax.swing.text.Document doc = textPane.getDocument();

        int start = caret;

        while (start > 0){
            String ch = doc.getText(start - 1, 1);
            if (!Character.isLetterOrDigit(ch.charAt(0))) break;
            start--;
        }
        return doc.getText(start, caret - start);
    }

    private void setupDocumentListener(){
        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onDocumentChange();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                onDocumentChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onDocumentChange();
            }

        });
    }

    private void onDocumentChange() {
        if (isUpdating) return;

        updateDocument();

        SwingUtilities.invokeLater(() -> {
            if (isUpdating) return;

            isUpdating = true;
            try {
                highlight();
            } finally {
                isUpdating = false;
            }
        });

        SwingUtilities.invokeLater(this::handleAutoComplete);
    }

    private void handleAutoComplete() {
        try {
            String fullText = getTextBeforeCaret();
            String line = getCurrentLineBeforeCaret();

            String obj = getObjectBeforeDot(line);

            if (obj != null) {
                Map<String, String> vars = extractVariables(fullText);

                String type = vars.get(obj);

                if (type != null) {
                    List<Suggestion> methods = getMethodsForType(type);

                    String prefix = getPrefixAfterDot(line);

                    List<Suggestion> filtered = filterByPrefix(methods, prefix);

                    if (!filtered.isEmpty()) {
                        showAutocomplete(filtered);
                    }else {
                        hideAutocomplete();
                    }
                    return;
                }
            }

            if (line.endsWith("System.")){
                showAutocomplete(List.of(new Suggestion("out", "METHOD", "void")));
                return;
            }

            if (line.contains("System.out.")){
                String prefix = getPrefixAfterDot(line);

                List<Suggestion> methods = List.of(
                        new Suggestion("println", "METHOD", "void"),
                        new Suggestion("print", "METHOD", "void")
                        );

                List<Suggestion> filtered = filterByPrefix(methods, prefix);

                if (!filtered.isEmpty()){
                    showAutocomplete(filtered);
                } else {
                    hideAutocomplete();
                }
                return;
            }

            String word = getCurrentWord();

            if (word.isEmpty()) {
                hideAutocomplete();
                return;
            }

            List<Suggestion> matches = new ArrayList<>();

            for (String kw : keywords) {
                if (kw.startsWith(word)) {
                    matches.add(new Suggestion(kw, "KEYWORD", getKeywordDetails(kw)));
                }
            }

            if (!matches.isEmpty()) {
                showAutocomplete(matches);
            }else {
                hideAutocomplete();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
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
        Pattern pattern = Pattern.compile("\".*?\"");
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

    private void highlightCurrentLine(){
        Highlighter highlighter = textPane.getHighlighter();

        if (currentLineHighlight != null){
            highlighter.removeHighlight(currentLineHighlight);
        }

        try {
            int caret = textPane.getCaretPosition();
            Element root = textPane.getDocument().getDefaultRootElement();
            int line = root.getElementIndex(caret);

            Element lineElement = root.getElement(line);

            currentLineHighlight = highlighter.addHighlight(
                    lineElement.getStartOffset(),
                    lineElement.getEndOffset(),
                    new FullLineHighlightPaint(new Color(60,63,65))
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateBreakpointHighlights() {
        Highlighter highlighter = textPane.getHighlighter();

        // limpa antigos
        for (Object h : breakpointHighlights) {
            highlighter.removeHighlight(h);
        }
        breakpointHighlights.clear();

        Element root = textPane.getDocument().getDefaultRootElement();

        for (int line : lineNumbers.getBreakpoints()) {
            try {
                Element el = root.getElement(line);

                Object tag = highlighter.addHighlight(
                        el.getStartOffset(),
                        el.getEndOffset(),
                        new FullLineHighlightPaint(new Color(72, 76, 78))
                );

                breakpointHighlights.add(tag);

            } catch (Exception ignored) {}
        }
    }


    private void applyDarkTheme(){
        textPane.setBackground(new Color(43, 43 ,43));
        textPane.setForeground(new Color(169, 183, 198));
        textPane.setCaretColor(Color.WHITE);
        textPane.setSelectionColor(new Color(33, 66, 131));
    }

    private void insertSuggestion(String suggestion) {
        try {
            if (suggestion == null) return;

            int caret = textPane.getCaretPosition();
            javax.swing.text.Document doc = textPane.getDocument();

            int start = caret;

            // volta até o início da palavra
            while (start > 0) {
                String ch = doc.getText(start - 1, 1);
                if (!Character.isLetterOrDigit(ch.charAt(0))) break;
                start--;
            }

            // remove a palavra atual
            doc.remove(start, caret - start);

            // insere a sugestão
            doc.insertString(start, suggestion, null);

            // move o cursor
            textPane.setCaretPosition(start + suggestion.length());

            // fecha o autocomplete
            hideAutocomplete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleEnter() {
        try {
            int caret = textPane.getCaretPosition();
            javax.swing.text.Document doc = textPane.getDocument();

            Element root = doc.getDefaultRootElement();
            int line = root.getElementIndex(caret);
            Element lineEl = root.getElement(line);

            int start = lineEl.getStartOffset();
            int end = lineEl.getEndOffset();

            String lineText = doc.getText(start, end - start);
            String beforeCaret = doc.getText(start, caret - start);
            String afterCaret = doc.getText(caret, end - caret);

            String indent = getIndentation(beforeCaret);

            // 🔥 CASO ESPECIAL: { }
            if (beforeCaret.trim().endsWith("{") && afterCaret.trim().startsWith("}")) {

                String innerIndent = indent + "    ";

                String insert = "\n" + innerIndent + "\n" + indent;

                doc.insertString(caret, insert, null);

                // posiciona cursor no meio
                textPane.setCaretPosition(caret + 1 + innerIndent.length());
                return;
            }

            // 🔥 CASO NORMAL: linha termina com {
            if (beforeCaret.trim().endsWith("{")) {
                indent += "    ";
            }

            doc.insertString(caret, "\n" + indent, null);
            textPane.setCaretPosition(caret + 1 + indent.length());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getIndentation(String text) {
        StringBuilder indent = new StringBuilder();

        for (char c : text.toCharArray()) {
            if (c == ' ' || c == '\t') {
                indent.append(c);
            } else {
                break;
            }
        }
        return indent.toString();
    }

    private void showAutocomplete(List<Suggestion> suggestions) {

        if (autocompleteWindow == null) {
            initAutocompleteWindow();
        }

        suggestionList.setListData(suggestions.toArray(new Suggestion[0]));
        suggestionList.setSelectedIndex(0);

        try {
            Rectangle r = textPane.modelToView2D(textPane.getCaretPosition()).getBounds();
            Point p = new Point(r.x, r.y + r.height);

            SwingUtilities.convertPointToScreen(p, textPane);

            autocompleteWindow.setLocation(p);
            autocompleteWindow.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getTextBeforeCaret() throws BadLocationException{
        int caret = textPane.getCaretPosition();
        return textPane.getDocument().getText(0, caret);
    }

    private String getCurrentLineBeforeCaret() throws BadLocationException{
        int caret = textPane.getCaretPosition();
        javax.swing.text.Document doc = textPane.getDocument();

        Element root = doc.getDefaultRootElement();
        int line = root.getElementIndex(caret);
        Element lineEl = root.getElement(line);

        int start = lineEl.getStartOffset();

        return doc.getText(start, caret - start).trim();
    }

    private Map<String, String> extractVariables(String text){
        Map<String, String> vars = new HashMap<>();

        Pattern pattern = Pattern.compile("\\b(String|int|double|boolean)\\s+(\\w+)");

        Matcher matcher = pattern.matcher(text);

        while (matcher.find()){
            String type = matcher.group(1);
            String name = matcher.group(2);

            vars.put(name, type);
        }
        return vars;
    }

    private String getObjectBeforeDot(String line){
        int dotIndex = line.lastIndexOf(".");

        if (dotIndex == -1) return null;

        String beforeDot = line.substring(0, dotIndex).trim();

        String[] parts = beforeDot.split("[^a-zA-Z0-9_]");

        if (parts.length == 0) return null;

        return parts[parts.length - 1];
    }

    private String getPrefixAfterDot(String line){
        int dotIndex = line.lastIndexOf(".");

        if (dotIndex == -1) return null;

        return line.substring(dotIndex + 1).trim();
    }

    private List<Suggestion> filterByPrefix(List<Suggestion> list, String prefix){

        if (prefix.isEmpty()) return list;

        List<Suggestion> result = new ArrayList<>();

        for (Suggestion s : list){
            if (s.getText().startsWith(prefix)){
                result.add(s);
            }
        }
        return result;
    }

    private List<Suggestion> getMethodsForType(String type){

        if (type.equals("String")) {
            return List.of(
                    new Suggestion("length()", "METHOD", "int"),
                    new Suggestion("substring()", "METHOD", "String"),
                    new Suggestion("toUpperCase()", "METHOD", "String"),
                    new Suggestion("charAt()", "METHOD", "char")
            );
        }
        return List.of();
    }

    private void hideAutocomplete() {
        if (autocompleteWindow != null) {
            autocompleteWindow.setVisible(false);
        }
    }

    private boolean isPopupVisible() {
        return autocompleteWindow != null && autocompleteWindow.isVisible();
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

    private String getKeywordDetails(String kw){
        return switch (kw) {
            case "public" -> "access modifier";
            case "private" -> "access modifier";
            case "protected" -> "access modifier";

            case "static" -> "class member modifier";
            case "final" -> "immutable modifier";
            case "abstract" -> "inheritance modifier";

            case "void" -> "no return type";

            case "int" -> "primitive type";
            case "double" -> "primitive type";
            case "boolean" -> "primitive type";
            case "char" -> "primitive type";

            case "class" -> "type declaration";
            case "new" -> "object instantiation";

            case "if", "else" -> "conditional statement";
            case "while", "for" -> "loop statement";

            case "return"-> "method return statement";

            default -> "keyword";
        };
    }
}



class FullLineHighlightPaint implements Highlighter.HighlightPainter{
    private final Color color;

    public FullLineHighlightPaint(Color color){
        this.color = color;
    }

    @Override
    public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
        try {
            Rectangle r = c.modelToView2D(p0).getBounds();

            g.setColor(color);
            g.fillRect(0, r.y, c.getWidth(), r.height);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
