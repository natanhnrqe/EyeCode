package com.eyecode.ui.editor;

import com.eyecode.editor.Document;
import com.eyecode.ui.LineNumberPanel;

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
    private Style typeStyle;
    private Style classStyle;
    private Style annotationStyle;
    private Style constantStyle;
    private Object currentLineHighlight;
    private LineNumberPanel lineNumbers;
    private final List<Object> breakpointHighlights = new ArrayList<>();
    private JPopupMenu popup;
    private JList<Suggestion> suggestionList;
    private JWindow autocompleteWindow;


    private static final String[] keywords = {
            "public", "class", "static", "void",
            "if", "else", "for", "while", "return",
            "new", "record", "sealed", "permits", "var", "yield",
            "interface", "int", "private", "protected", "do", "boolean",
            "final", "import", "package"

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

        typeStyle = doc.addStyle("Type", null);
        StyleConstants.setForeground(typeStyle, new Color(78, 201, 176));

        classStyle = doc.addStyle("Class", null);
        StyleConstants.setForeground(classStyle, new Color(78, 201, 176 ));

        annotationStyle = doc.addStyle("Annotation", null);
        StyleConstants.setForeground(annotationStyle, new Color(187, 181, 41));

        constantStyle = doc.addStyle("Constant", null);
        StyleConstants.setForeground(constantStyle, new Color(199, 125, 187));

        keywordStyle = doc.addStyle("Keyword", null);
        StyleConstants.setForeground(keywordStyle, new Color(207, 109, 100));

        normalStyle = doc.addStyle("Normal", null);
        StyleConstants.setForeground(normalStyle, new Color(188, 190, 196));

        stringStyle = doc.addStyle("String", null);
        StyleConstants.setForeground(stringStyle, new Color(106, 171, 115));

        commentStyle = doc.addStyle("Comment", null);
        StyleConstants.setForeground(commentStyle, new Color(122, 126, 133));

        numberStyle = doc.addStyle("Number", null);
        StyleConstants.setForeground(numberStyle, new Color(42,172,184));

        methodStyle = doc.addStyle("Method", null);
        StyleConstants.setForeground(methodStyle, new Color(86,168,245));

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
                showAutocomplete(List.of(new Suggestion(
                        "out",
                        "FIELD",
                        "PrintStream System.out",
                        "Standart output stream",
                        100)
                ));
                return;
            }

            if (line.contains("System.out.")){

                String prefix = getPrefixAfterDot(line);

                List<Suggestion> methods = List.of(

                        new Suggestion("println",
                                "METHOD",
                                "void println(String x)",
                                "Prints text with line break",
                                100

                        ),
                        new Suggestion("print",
                                "METHOD",
                                "void print(String x)",
                                "Print text",
                                100)
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

            matches.addAll(
                    getVariableSuggestion(fullText, word)
            );

            matches.addAll(
                    getBasicSuggestion(word)
            );

            sortSuggestion(matches);

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


        highlightStrings(text);
        highlightStrings(text);

        highlightAnnotations(text);
        highlightConstants(text);
        highlightNumbers(text);

        highlightTypes(text);
        highlightClasses(text);

        highlightKeywords(text);
        highlightMethods(text);
    }

    private void highlightTypes(String text){

        String[] type = {
                "int", "long", "double",
                "float", "boolean", "char",
                "byte", "short", "void"
        };

        for (String t : type){

            Pattern p = Pattern.compile("\\b" + t + "\\b");

            Matcher m = p.matcher(text);

            while (m.find()) {
                doc.setCharacterAttributes(
                        m.start(),
                        m.end() - m.start(),
                        typeStyle,
                        false
                );
            }
        }
    }

    private void highlightClasses(String text) {

        String[] classes = {
                "String", "System", "Math",
                "List", "ArrayList", "Map",
                "HashMap", "Set", "HashSet",
                "LocalDate", "LocalDateTime"
        };

        for (String c : classes) {

            Pattern p =
                    Pattern.compile("\\b" + c + "\\b");

            Matcher m = p.matcher(text);

            while (m.find()) {
                doc.setCharacterAttributes(
                        m.start(),
                        m.end() - m.start(),
                        classStyle,
                        false
                );
            }
        }
    }

    private void highlightAnnotations(
            String text) {

        Pattern p =
                Pattern.compile("@\\w+");

        Matcher m = p.matcher(text);

        while (m.find()) {

            doc.setCharacterAttributes(
                    m.start(),
                    m.end() - m.start(),
                    annotationStyle,
                    false
            );
        }
    }

    private void highlightConstants(
            String text) {

        Pattern p =
                Pattern.compile(
                        "\\b(true|false|null)\\b");

        Matcher m = p.matcher(text);

        while (m.find()) {

            doc.setCharacterAttributes(
                    m.start(),
                    m.end() - m.start(),
                    constantStyle,
                    false
            );
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
        int i = 0;

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
                    new FullLineHighlightPaint(new Color(39,41,42))
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
                        new FullLineHighlightPaint(new Color(70, 15, 40))
                );

                breakpointHighlights.add(tag);

            } catch (Exception ignored) {}
        }
    }


    private void applyDarkTheme(){
        textPane.setBackground(new Color(30, 30, 30));
        textPane.setForeground(new Color(188, 190, 196));
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

    private List<Suggestion> getMethodsForType(String type){

        switch (type) {

            case "String":
                return List.of(

                        new Suggestion(
                                "length()",
                                "METHOD",
                                "int length()",
                                "Returns text length",
                                100
                                ),

                        new Suggestion(
                                "substring()",
                                "METHOD",
                                "String substring(int a, int b)",
                                "Returns parts of text ",
                                95
                        ),

                        new Suggestion(
                                "toUpperCase()",
                                "METHOD",
                                "String toUpperCase()",
                                "Uppercase text ",
                                90
                        )
                );
            case "ArrayList":
                return List.of(

                        new Suggestion(
                                "add()",
                                "METHOD",
                                "boolean add(E a)",
                                "Adds item",
                                100
                        ),

                        new Suggestion(
                                "size()",
                                "METHOD",
                                "int size()",
                                "Returns quantity ",
                                95
                        )
                );
        }
        return new ArrayList<>();
    }

    private List<Suggestion> getBasicSuggestion(String prefix){

        List<Suggestion> list = new ArrayList<>();

        addPrimitiveSuggestions(list, prefix);
        addCoreClassSuggestions(list, prefix);
        addCollectionsSuggestions(list, prefix);
        addDateTimeSuggestions(list, prefix);
        addIoFilesSuggestions(list, prefix);
        addKeywordsSuggestions(list, prefix);
        addStreamsFunctionalSuggestions(list, prefix);
        addWrapperClassSuggestions(list, prefix);


        return list;
    }

    private List<Suggestion> getVariableSuggestion(String fullText, String prefix){

        Map<String, String> vars = extractVariables(fullText);

        List<Suggestion> list = new ArrayList<>();

        for (String name : vars.keySet()) {

            if (name.startsWith(prefix)) {
                String type = vars.get(name);

                list.add(new Suggestion(
                        name,
                        "VARIABLE",
                        "variable " + type,
                        "Detected variable",
                        90
                ));
            }
        }
        return list;
    }



    private void addIfMatch(
            List<Suggestion> list,
            String prefix,
            String text,
            String type,
            String detail,
            String desc,
            int priority
    ){
        if (text.startsWith(prefix)) {
            list.add(new Suggestion(text, type, detail, desc, priority));
        }

    }

    private void addPrimitiveSuggestions(List<Suggestion> list, String prefix){

        // Tipos primitivos
        addIfMatch(list, prefix, "int", "TYPE",
                "primitive", "32-bit integer", 95);

        addIfMatch(list, prefix, "long", "TYPE",
                "primitive", "64-bit integer", 95);

        addIfMatch(list, prefix, "double", "TYPE",
                "primitive", "64-bit decimal", 95);

        addIfMatch(list, prefix, "float", "TYPE",
                "primitive", "32-bit decimal", 90);

        addIfMatch(list, prefix, "boolean", "TYPE",
                "primitive", "true or false", 95);

        addIfMatch(list, prefix, "char", "TYPE",
                "primitive", "Unicode character", 90);

        addIfMatch(list, prefix, "byte", "TYPE",
                "primitive", "8-bit integer", 90);

        addIfMatch(list, prefix, "short", "TYPE",
                "primitive", "16-bit integer", 90);


    }

    private void addWrapperClassSuggestions(List<Suggestion> list, String prefix){

        addIfMatch(list, prefix, "Integer", "CLASS",
                "wrapper", "Wrapper for int", 88);

        addIfMatch(list, prefix, "Long", "CLASS",
                "wrapper", "Wrapper for long", 88);

        addIfMatch(list, prefix, "Double", "CLASS",
                "wrapper", "Wrapper for double", 88);

        addIfMatch(list, prefix, "Float", "CLASS",
                "wrapper", "Wrapper for float", 85);

        addIfMatch(list, prefix, "Boolean", "CLASS",
                "wrapper", "Wrapper for boolean", 88);

        addIfMatch(list, prefix, "Character", "CLASS",
                "wrapper", "Wrapper for char", 88);

        addIfMatch(list, prefix, "Byte", "CLASS",
                "wrapper", "Wrapper for byte", 80);

        addIfMatch(list, prefix, "Short", "CLASS",
                "wrapper", "Wrapper for short", 80);

    }

    private void addCoreClassSuggestions(List<Suggestion> list, String prefix){

        addIfMatch(list, prefix, "String", "CLASS",
                "java.lang", "Immutable text", 100);

        addIfMatch(list, prefix, "Object", "CLASS",
                "java.lang", "Root class of all objects", 90);

        addIfMatch(list, prefix, "System", "CLASS",
                "java.lang", "System utilities", 100);

        addIfMatch(list, prefix, "Math", "CLASS",
                "java.lang", "Math helpers", 95);

        addIfMatch(list, prefix, "Thread", "CLASS",
                "java.lang", "Concurrent thread", 85);

        addIfMatch(list, prefix, "Runnable", "INTERFACE",
                "java.lang", "Runnable task", 82);

        addIfMatch(list, prefix, "Exception", "CLASS",
                "java.lang", "Checked exception", 85);

        addIfMatch(list, prefix, "RuntimeException", "CLASS",
                "java.lang", "Unchecked exception", 85);

        addIfMatch(list, prefix, "StringBuilder", "CLASS",
                "java.lang", "Mutable text builder", 92);

        addIfMatch(list, prefix, "StringBuffer", "CLASS",
                "java.lang", "Synchronized text builder", 80);

        addIfMatch(list, prefix, "StringBuffer", "CLASS",
                "java.lang", "Synchronized text builder", 80);

        addIfMatch(list, prefix, "Class", "CLASS",
                "reflection", "Runtime type info", 75);

        addIfMatch(list, prefix, "Enum", "CLASS",
                "java.lang", "Enum base type", 80);
    }

    private void addCollectionsSuggestions(List<Suggestion> list, String prefix){

        addIfMatch(list, prefix, "List", "INTERFACE",
                "java.util", "Ordered collection", 98);

        addIfMatch(list, prefix, "ArrayList", "CLASS",
                "java.util", "Dynamic array list", 100);

        addIfMatch(list, prefix, "LinkedList", "CLASS",
                "java.util", "Linked list", 82);

        addIfMatch(list, prefix, "Set", "INTERFACE",
                "java.util", "Unique elements", 94);

        addIfMatch(list, prefix, "HashSet", "CLASS",
                "java.util", "Set implementation", 90);

        addIfMatch(list, prefix, "TreeSet", "CLASS",
                "java.util", "Sorted set", 82);

        addIfMatch(list, prefix, "Map", "INTERFACE",
                "java.util", "Key value pairs", 97);

        addIfMatch(list, prefix, "HashMap", "CLASS",
                "java.util", "Map implementation", 100);

        addIfMatch(list, prefix, "TreeMap", "CLASS",
                "java.util", "Sorted map", 82);

        addIfMatch(list, prefix, "Queue", "INTERFACE",
                "java.util", "FIFO structure", 85);

        addIfMatch(list, prefix, "Deque", "INTERFACE",
                "java.util", "Double ended queue", 80);

        addIfMatch(list, prefix, "PriorityQueue", "CLASS",
                "java.util", "Priority queue", 78);

        addIfMatch(list, prefix, "Collections", "CLASS",
                "java.util", "Collection helpers", 75);

        addIfMatch(list, prefix, "Array", "CLASS",
                "java.util", "Array helpers", 92);

        addIfMatch(list, prefix, "Optional", "CLASS",
                "java.util", "Nullable wrapper", 95);

    }

    private void addStreamsFunctionalSuggestions(List<Suggestion> list, String prefix){

        addIfMatch(list, prefix, "Stream", "INTERFACE",
                "java.util.stream", "Stream pipeline", 95);

        addIfMatch(list, prefix, "Collectors", "CLASS",
                "java.util.stream", "Terminal collectors", 92);

        addIfMatch(list, prefix, "Predicate", "INTERFACE",
                "functional", "Boolean lambda", 85);

        addIfMatch(list, prefix, "Function", "INTERFACE",
                "functional", "Map input to output", 88);

        addIfMatch(list, prefix, "Consumer", "INTERFACE",
                "functional", "Consumes value", 84);

        addIfMatch(list, prefix, "Supplier", "INTERFACE",
                "functional", "Supplies value", 84);

        addIfMatch(list, prefix, "Comparator", "INTERFACE",
                "java.util", "Compares value", 88);
    }

    private void addDateTimeSuggestions(List<Suggestion> list, String prefix){

        addIfMatch(list, prefix, "LocalDate", "CLASS",
                "java.time", "Date only", 92);

        addIfMatch(list, prefix, "LocalTime", "CLASS",
                "java.time", "Time only", 88);

        addIfMatch(list, prefix, "LocalDateTime", "CLASS",
                "java.time", "Date and Time", 95);

        addIfMatch(list, prefix, "Instant", "CLASS",
                "java.time", "UTC timestamp", 84);

        addIfMatch(list, prefix, "Duration", "CLASS",
                "java.time", "Time span", 82);

        addIfMatch(list, prefix, "Period", "CLASS",
                "java.time", "Date span", 80);

        addIfMatch(list, prefix, "DateTimeFormatter", "CLASS",
                "java.time", "Format dates", 86);

        addIfMatch(list, prefix, "ZoneId", "CLASS",
                "java.time", "Timezone id", 78);
    }

    private void addIoFilesSuggestions(List<Suggestion> list, String prefix){

        addIfMatch(list, prefix, "Path", "INTERFACE",
                "nio.file", "Filesystem path", 90);

        addIfMatch(list, prefix, "Paths", "CLASS",
                "nio.file", "Path factory", 80);

        addIfMatch(list, prefix, "Files", "CLASS",
                "nio.file", "File utilities", 95);

        addIfMatch(list, prefix, "File", "CLASS",
                "io", "Legacy file api", 78);

        addIfMatch(list, prefix, "Scanner", "CLASS",
                "util", "Text scanner", 90);

        addIfMatch(list, prefix, "BufferedReader", "CLASS",
                "io", "Buffered reader", 82);

        addIfMatch(list, prefix, "BufferedWriter", "CLASS",
                "io", "Buffered writer", 80);

        addIfMatch(list, prefix, "InputStream", "CLASS",
                "io", "Byte input", 75);

        addIfMatch(list, prefix, "OutputStream", "CLASS",
                "io", "Byte output", 75);

    }


    private void addKeywordsSuggestions(List<Suggestion> list, String prefix){

        addIfMatch(list, prefix, "public", "KEYWORD",
                "access modifier", "Visible everywhere", 100);

        addIfMatch(list, prefix, "private", "KEYWORD",
                "access modifier", "Visible inside class", 100);

        addIfMatch(list, prefix, "protected", "KEYWORD",
                "access modifier", "visible to subclasses", 95);

        addIfMatch(list, prefix, "static", "KEYWORD",
                "class modifier", "Belongs to class, not instance", 95);

        addIfMatch(list, prefix, "final", "KEYWORD",
                "modifier", "Immutable/final", 92);

        addIfMatch(list, prefix, "abstract", "KEYWORD",
                "modifier", "Abstract type/member", 88);



        addIfMatch(list, prefix, "class", "KEYWORD",
                "declaration", "Declares a class", 100);

        addIfMatch(list, prefix, "interface", "KEYWORD",
                "declaration", "Declares interface", 95);

        addIfMatch(list, prefix, "enum", "KEYWORD",
                "declaration", "Declares enum", 88);

        addIfMatch(list, prefix, "record", "KEYWORD",
                "java21", "Immutable data carrier", 100);

        addIfMatch(list, prefix, "sealed", "KEYWORD",
                "java21", "Restrict inheritance", 95);

        addIfMatch(list, prefix, "permits", "KEYWORD",
                "java21", "Sealed subclasses", 88);

        addIfMatch(list, prefix, "non-sealed", "KEYWORD",
                "java21", "Reopens hierarchy", 82);


        addIfMatch(list, prefix, "if", "KEYWORD",
                "flow control", "Conditional", 100);

        addIfMatch(list, prefix, "else", "KEYWORD",
                "flow control", "Alternative branch", 95);

        addIfMatch(list, prefix, "switch", "KEYWORD",
                "flow control", "Multi branch", 92);

        addIfMatch(list, prefix, "case", "KEYWORD",
                "flow control", "Switch branch", 88);

        addIfMatch(list, prefix, "default", "KEYWORD",
                "flow control", "Default branch", 82);


        addIfMatch(list, prefix, "for", "KEYWORD",
                "loop", "For loop", 100);

        addIfMatch(list, prefix, "while", "KEYWORD",
                "loop", "While loop", 95);

        addIfMatch(list, prefix, "do", "KEYWORD",
                "loop", "Do while loop", 82);

        addIfMatch(list, prefix, "break", "KEYWORD",
                "loop", "Exits loop", 82);

        addIfMatch(list, prefix, "continue", "KEYWORD",
                "loop", "Next iteration", 95);


        addIfMatch(list, prefix, "try", "KEYWORD",
                "exceptions", "Try block", 90);

        addIfMatch(list, prefix, "catch", "KEYWORD",
                "exceptions", "Catch exception", 90);

        addIfMatch(list, prefix, "finally", "KEYWORD",
                "exceptions", "Always runs", 82);

        addIfMatch(list, prefix, "throw", "KEYWORD",
                "exceptions", "Throw exception", 82);

        addIfMatch(list, prefix, "throws", "KEYWORD",
                "exceptions", "Declares throws", 82);


        addIfMatch(list, prefix, "return", "KEYWORD",
                "flow statement", "Returns a value", 100);

        addIfMatch(list, prefix, "void", "KEYWORD",
                "return type", "Returns nothing", 95);

        addIfMatch(list, prefix, "new", "KEYWORD",
                "operator", "Creates object", 100);

        addIfMatch(list, prefix, "this", "KEYWORD",
                "reference", "Current instance", 92);

        addIfMatch(list, prefix, "super", "KEYWORD",
                "reference", "Parent instance", 88);

        addIfMatch(list, prefix, "instanceof", "KEYWORD",
                "operator", "Type check", 82);

        addIfMatch(list, prefix, "var", "KEYWORD",
                "local inference", "Inferred local type", 95);

    }


    private void sortSuggestion(List<Suggestion> list){
        list.sort((a,b) ->
        Integer.compare(b.getPriority(), a.getPriority())
        );

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
