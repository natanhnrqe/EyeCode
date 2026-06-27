package com.eyecode.editor.v2.ui;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.caret.CaretSynchronizationManager;
import com.eyecode.editor.v2.completion.CompletionEngine;
import com.eyecode.editor.v2.completion.CompletionManager;
import com.eyecode.editor.v2.completion.JavaKeywordCompletionProvider;
import com.eyecode.editor.v2.completion.insert.CompletionInsertionContext;
import com.eyecode.editor.v2.completion.insert.CompletionInsertionEngine;
import com.eyecode.editor.v2.completion.insert.CompletionPrefixResolver;
import com.eyecode.editor.v2.completion.semantic.SemanticCompletionProvider;
import com.eyecode.editor.v2.completion.semantic.SemanticSymbolRegistry;
import com.eyecode.editor.v2.diagnostics.DiagnosticManager;
import com.eyecode.editor.v2.diagnostics.EmptyDiagnosticEngine;
import com.eyecode.editor.v2.language.DefaultLanguageService;
import com.eyecode.editor.v2.language.LanguageManager;
import com.eyecode.editor.v2.syntax.DocumentStyleRegistry;
import com.eyecode.editor.v2.syntax.JavaSyntaxAnalyzer;
import com.eyecode.editor.v2.syntax.SyntaxSnapshot;
import com.eyecode.editor.v2.syntax.SyntaxToken;
import com.eyecode.editor.v2.syntax.TokenType;
import com.eyecode.editor.v2.syntax.swing.SwingSyntaxRenderer;
import com.eyecode.editor.v2.ui.completion.CompletionPopup;
import com.eyecode.editor.v2.ui.gutter.GutterPanel;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class RichEditorView extends JPanel {

    private static final String INDENTATION_UNIT = "    ";

    private final EditorBuffer buffer;
    private final JTextPane textPane;
    private final StyledDocument styledDocument;
    private final JScrollPane scrollPane;
    private final GutterPanel gutterPanel;
    private final JavaSyntaxAnalyzer analyzer;
    private final SwingSyntaxRenderer renderer;
    private final DocumentStyleRegistry registry;
    private final CaretSynchronizationManager caretSync;
    private final DiagnosticManager diagnosticManager;
    private final LanguageManager languageManager;
    private final CompletionManager completionManager;
    private final CompletionPopup completionPopup;
    private final CompletionInsertionEngine completionInsertionEngine;
    private final CompletionPrefixResolver completionPrefixResolver;
    private final DocumentListener documentListener;
    private final CaretListener caretListener;
    private final FocusAdapter focusListener;
    private final EditorDocument.TextChangeListener textChangeListener;
    private final List<AutoPair> autoPairs;
    private SyntaxSnapshot latestSyntaxSnapshot;
    private boolean refreshing;
    private boolean suppressPopup;
    private boolean disposed;
    private boolean syncingFromSwing;
    private boolean renderPending;

    public RichEditorView(EditorBuffer buffer) {
        super(new BorderLayout());
        this.buffer = buffer;
        this.textPane = new JTextPane() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                super.paintComponent(g);
            }
        };
        this.textPane.setFont(TypographyManager.UI_CODE());
        this.textPane.setBackground(ColorManager.EDITOR_BG);
        this.textPane.setForeground(ColorManager.EDITOR_FOREGROUND);
        this.textPane.setCaretColor(ColorManager.EDITOR_CARET);
        this.textPane.setSelectionColor(ColorManager.EDITOR_SELECTION);
        this.textPane.setSelectedTextColor(ColorManager.EDITOR_FOREGROUND);
        this.textPane.setMargin(new Insets(4, 8, 4, 8));
        this.styledDocument = textPane.getStyledDocument();
        this.scrollPane = new JScrollPane(textPane);
        this.scrollPane.setBackground(ColorManager.EDITOR_BG);
        this.scrollPane.getViewport().setBackground(ColorManager.EDITOR_BG);
        this.scrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        this.gutterPanel = new GutterPanel(textPane);
        this.analyzer = new JavaSyntaxAnalyzer();
        this.registry = new DocumentStyleRegistry();
        this.renderer = new SwingSyntaxRenderer(styledDocument, registry);
        this.caretSync = new CaretSynchronizationManager(textPane, buffer);
        this.diagnosticManager = new DiagnosticManager(new EmptyDiagnosticEngine());
        this.languageManager = new LanguageManager(new DefaultLanguageService());
        this.completionManager = new CompletionManager(new CompletionEngine(
                List.of(
                        new JavaKeywordCompletionProvider(),
                        new SemanticCompletionProvider(new SemanticSymbolRegistry())
                )
        ));
        this.completionPopup = new CompletionPopup();
        this.completionInsertionEngine = new CompletionInsertionEngine();
        this.completionPrefixResolver = new CompletionPrefixResolver();
        this.autoPairs = new ArrayList<>();
        installCompletionActions();
        this.completionPopup.setOnSelect(event -> {
            if (disposed) return;
            buffer.setCompletionSelection(event.getSelectedItem());
            if (event.getSelectedItem() != null) {
                suppressPopup = true;
                String currentPrefix = completionPrefixResolver.resolve(buffer.getLanguageContext());
                int caretOffset = toOffset(buffer.getDocument().getText(), buffer.getCaret());
                CompletionInsertionContext insertionContext = new CompletionInsertionContext(
                        buffer.getDocument(),
                        buffer.getCaret(),
                        event.getSelectedItem(),
                        currentPrefix
                );
                completionInsertionEngine.insert(insertionContext);
                autoPairs.clear();
                refreshFromDocument();
                int newCaretOffset = Math.max(0, caretOffset - currentPrefix.length()
                        + event.getSelectedItem().getInsertText().length());
                buffer.setCaretPosition(toPosition(buffer.getDocument().getText(), newCaretOffset));
                suppressPopup = false;
            }
            completionPopup.hide();
        });

        insertDocumentText(buffer.getDocument().getText());
        styledDocument.setParagraphAttributes(0, styledDocument.getLength(), createDefaultParagraphStyle(), false);
        this.documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                syncInsert(e.getOffset(), e.getLength());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                syncDelete(e.getOffset(), e.getLength());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        };
        styledDocument.addDocumentListener(documentListener);

        this.textChangeListener = (oldText, newText) -> {
            if (disposed || syncingFromSwing) return;
            if (!newText.contentEquals(getCurrentText())) {
                SwingUtilities.invokeLater(() -> refreshFromDocument());
            }
        };
        buffer.getDocument().addTextChangeListener(textChangeListener);

        add(scrollPane, BorderLayout.CENTER);
        scrollPane.setRowHeaderView(gutterPanel);
        this.caretListener = event -> {
            gutterPanel.refresh();
            if (completionPopup.isVisible()) {
                refreshLanguageContext();
                if (isCompletionContextValid()) {
                    completionPopup.move(textPane, textPane.getCaretPosition());
                } else {
                    completionPopup.hide();
                }
            }
        };
        textPane.addCaretListener(caretListener);
        this.focusListener = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                completionPopup.hide();
            }
        };
        textPane.addFocusListener(focusListener);
        renderSyntax();
        refreshDiagnostics();
        refreshLanguageContext();
        refreshCompletions();
    }

    private javax.swing.text.SimpleAttributeSet createDefaultParagraphStyle() {
        javax.swing.text.SimpleAttributeSet style = new javax.swing.text.SimpleAttributeSet();
        StyleConstants.setForeground(style, ColorManager.EDITOR_FOREGROUND);
        StyleConstants.setFontFamily(style, TypographyManager.UI_CODE().getFamily());
        StyleConstants.setFontSize(style, TypographyManager.UI_CODE().getSize());
        return style;
    }

    private void installCompletionActions() {
        InputMap inputMap = textPane.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = textPane.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "completionAcceptOrEnter");
        actionMap.put("completionAcceptOrEnter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!acceptCompletion()) {
                    handleEnterIndentation();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "completionAcceptOrTab");
        actionMap.put("completionAcceptOrTab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!acceptCompletion()) {
                    handleTabIndentation();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK), "editorOutdentLine");
        actionMap.put("editorOutdentLine", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleShiftTabOutdent();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "editorSmartBackspace");
        actionMap.put("editorSmartBackspace", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleBackspace();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK), "editorDuplicateLine");
        actionMap.put("editorDuplicateLine", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                duplicateLineOrSelection();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                "editorDeleteLine");
        actionMap.put("editorDeleteLine", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteCurrentLine();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK), "editorMoveLineUp");
        actionMap.put("editorMoveLineUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveCurrentLineUp();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK), "editorMoveLineDown");
        actionMap.put("editorMoveLineDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveCurrentLineDown();
            }
        });

        installPopupAction(inputMap, actionMap, KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
                "completionUp", () -> completionPopup.selectPrevious());
        installPopupAction(inputMap, actionMap, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
                "completionDown", () -> completionPopup.selectNext());
        installPopupAction(inputMap, actionMap, KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0),
                "completionPageUp", () -> completionPopup.selectPageUp());
        installPopupAction(inputMap, actionMap, KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0),
                "completionPageDown", () -> completionPopup.selectPageDown());
        installPopupAction(inputMap, actionMap, KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0),
                "completionHome", () -> completionPopup.selectFirst());
        installPopupAction(inputMap, actionMap, KeyStroke.getKeyStroke(KeyEvent.VK_END, 0),
                "completionEnd", () -> completionPopup.selectLast());

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "completionHide");
        actionMap.put("completionHide", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                completionPopup.hide();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK), "completionForceOpen");
        actionMap.put("completionForceOpen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                forceOpenCompletions();

            }
        });
    
        installPairAction(inputMap, actionMap, '(', ')', "editorPairParentheses");
        installPairAction(inputMap, actionMap, '{', '}', "editorPairBraces");
        installPairAction(inputMap, actionMap, '[', ']', "editorPairBrackets");
        installPairAction(inputMap, actionMap, '"', '"', "editorPairDoubleQuotes");
        installPairAction(inputMap, actionMap, '\'', '\'', "editorPairSingleQuotes");
        installClosingDelimiterAction(inputMap, actionMap, ')', "editorCloseParentheses");
        installClosingDelimiterAction(inputMap, actionMap, '}', "editorCloseBraces");
        installClosingDelimiterAction(inputMap, actionMap, ']', "editorCloseBrackets");
    }

    private void installPopupAction(InputMap inputMap, ActionMap actionMap, KeyStroke keyStroke,
                                    String actionName, Runnable popupAction) {
        Object defaultActionKey = inputMap.get(keyStroke);
        Action defaultAction = defaultActionKey == null ? null : actionMap.get(defaultActionKey);

        inputMap.put(keyStroke, actionName);
        actionMap.put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (completionPopup.isVisible()) {
                    popupAction.run();
                } else {
                    runDefaultAction(defaultAction, e);
                }
            }
        });
    }

    private void installPairAction(InputMap inputMap, ActionMap actionMap, char opening, char closing,
                                   String actionName) {
        inputMap.put(KeyStroke.getKeyStroke(opening), actionName);
        actionMap.put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleDelimiterInsertion(opening, closing);
            }
        });
    }

    private void installClosingDelimiterAction(InputMap inputMap, ActionMap actionMap, char closing,
                                               String actionName) {
        inputMap.put(KeyStroke.getKeyStroke(closing), actionName);
        actionMap.put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleClosingDelimiter(closing);
            }
        });
    }

    private boolean acceptCompletion() {
        if (!completionPopup.isVisible()) {
            return false;
        }

        completionPopup.acceptSelected();
        return true;
    }

    private void runDefaultAction(Action action, ActionEvent event) {
        if (action != null) {
            action.actionPerformed(new ActionEvent(textPane, event.getID(), event.getActionCommand(),
                    event.getWhen(), event.getModifiers()));
        }
    }

    private void handleTabIndentation() {
        insertIndentationUnit();
    }

    private void handleDelimiterInsertion(char opening, char closing) {
        int caretOffset = textPane.getCaretPosition();
        String text = buffer.getDocument().getText();
        int safeOffset = Math.max(0, Math.min(caretOffset, text.length()));

        if (isQuote(opening) && skipClosingDelimiter(opening, safeOffset, text)) {
            return;
        }

        if (isQuote(opening) && isImmediatelyBeforeQuote(opening, safeOffset, text)) {
            buffer.moveCaret(toPosition(text, safeOffset + 1));
            return;
        }

        if (isQuote(opening) && shouldInsertSingleQuoteOnly(opening, safeOffset, text)) {
            executeInsert(safeOffset, String.valueOf(opening), safeOffset + 1);
            return;
        }

        executePairInsert(safeOffset, opening, closing);
    }

    private void handleClosingDelimiter(char closing) {
        int caretOffset = textPane.getCaretPosition();
        String text = buffer.getDocument().getText();
        int safeOffset = Math.max(0, Math.min(caretOffset, text.length()));
        if (!skipClosingDelimiter(closing, safeOffset, text)) {
            executeInsert(safeOffset, String.valueOf(closing), safeOffset + 1);
        }
    }

    private void handleBackspace() {
        if (deleteAutoPair()) {
            return;
        }

        int selectionStart = textPane.getSelectionStart();
        int selectionEnd = textPane.getSelectionEnd();
        if (selectionStart != selectionEnd) {
            executeDelete(selectionStart, selectionEnd, selectionStart);
            return;
        }

        int caretOffset = textPane.getCaretPosition();
        if (caretOffset > 0) {
            executeDelete(caretOffset - 1, caretOffset, caretOffset - 1);
        }
    }

    private void duplicateLineOrSelection() {
        String text = buffer.getDocument().getText();
        int selectionStart = textPane.getSelectionStart();
        int selectionEnd = textPane.getSelectionEnd();
        if (selectionStart != selectionEnd) {
            int blockStart = Math.min(selectionStart, selectionEnd);
            int blockEnd = Math.max(selectionStart, selectionEnd);
            String selectedText = text.substring(blockStart, blockEnd);
            String insertion = selectedText.endsWith("\n") || blockEnd >= text.length()
                    ? selectedText
                    : selectedText + "\n";
            executeInsert(blockEnd, insertion, blockEnd + insertion.length());
            return;
        }

        int caretOffset = textPane.getCaretPosition();
        LineRange line = currentLineRange(text, caretOffset);
        String lineText = text.substring(line.contentStart, line.contentEnd);
        String duplicate = "\n" + lineText;
        int insertOffset = line.contentEnd;
        executeInsert(insertOffset, duplicate, insertOffset + duplicate.length());
    }

    private void deleteCurrentLine() {
        String text = buffer.getDocument().getText();
        if (text.isEmpty()) {
            return;
        }

        int caretOffset = textPane.getCaretPosition();
        LineRange line = currentLineRange(text, caretOffset);
        int deleteStart = line.contentStart;
        int deleteEnd = line.nextLineStart;
        int caretAfterDelete = deleteStart;

        if (deleteEnd > text.length() && line.contentStart > 0) {
            deleteStart = line.contentStart - 1;
            deleteEnd = text.length();
            caretAfterDelete = deleteStart;
        } else if (deleteEnd > text.length()) {
            deleteEnd = text.length();
            caretAfterDelete = 0;
        }

        executeDelete(deleteStart, deleteEnd, Math.min(caretAfterDelete, buffer.getDocument().getText().length()));
    }

    private void moveCurrentLineUp() {
        String text = buffer.getDocument().getText();
        int caretOffset = textPane.getCaretPosition();
        LineRange current = currentLineRange(text, caretOffset);
        if (current.contentStart == 0) {
            return;
        }

        LineRange previous = currentLineRange(text, current.contentStart - 1);
        String previousText = text.substring(previous.contentStart, previous.contentEnd);
        String currentText = text.substring(current.contentStart, current.contentEnd);
        String newText = text.substring(0, previous.contentStart)
                + currentText
                + "\n"
                + previousText
                + text.substring(current.contentEnd);
        int caretAfterMove = previous.contentStart + (caretOffset - current.contentStart);
        replaceDocumentText(newText, caretAfterMove);
    }

    private void moveCurrentLineDown() {
        String text = buffer.getDocument().getText();
        int caretOffset = textPane.getCaretPosition();
        LineRange current = currentLineRange(text, caretOffset);
        if (current.fullEnd >= text.length()) {
            return;
        }

        LineRange next = currentLineRange(text, current.fullEnd);
        String currentText = text.substring(current.contentStart, current.contentEnd);
        String nextText = text.substring(next.contentStart, next.contentEnd);
        String newText = text.substring(0, current.contentStart)
                + nextText
                + "\n"
                + currentText
                + text.substring(next.contentEnd);
        int caretAfterMove = current.contentStart + nextText.length() + 1 + (caretOffset - current.contentStart);
        replaceDocumentText(newText, caretAfterMove);
    }

    private void handleEnterIndentation() {
        int caretOffset = textPane.getCaretPosition();
        String text = buffer.getDocument().getText();
        int safeOffset = Math.max(0, Math.min(caretOffset, text.length()));
        int lineStart = findLineStart(text, safeOffset);
        String baseIndent = leadingIndent(text, lineStart);

        boolean betweenBraces = safeOffset > 0
                && safeOffset < text.length()
                && text.charAt(safeOffset - 1) == '{'
                && text.charAt(safeOffset) == '}';
        boolean afterOpeningBrace = !betweenBraces
                && text.substring(lineStart, safeOffset).stripTrailing().endsWith("{");

        String insertText;
        int caretAfterInsert;
        if (betweenBraces) {
            insertText = "\n" + baseIndent + INDENTATION_UNIT + "\n" + baseIndent;
            caretAfterInsert = safeOffset + 1 + baseIndent.length() + INDENTATION_UNIT.length();
        } else {
            String nextIndent = baseIndent + (afterOpeningBrace ? INDENTATION_UNIT : "");
            insertText = "\n" + nextIndent;
            caretAfterInsert = safeOffset + insertText.length();
        }

        executeInsert(safeOffset, insertText, caretAfterInsert);
    }

    private void handleShiftTabOutdent() {
        int caretOffset = textPane.getCaretPosition();
        String text = buffer.getDocument().getText();
        int safeOffset = Math.max(0, Math.min(caretOffset, text.length()));
        int lineStart = findLineStart(text, safeOffset);
        if (!text.startsWith(INDENTATION_UNIT, lineStart)) {
            return;
        }

        int caretAfterDelete = Math.max(lineStart, safeOffset - INDENTATION_UNIT.length());
        executeDelete(lineStart, lineStart + INDENTATION_UNIT.length(), caretAfterDelete);
    }

    private void insertIndentationUnit() {
        int caretOffset = textPane.getCaretPosition();
        executeInsert(caretOffset, INDENTATION_UNIT, caretOffset + INDENTATION_UNIT.length());
    }

    private void executeInsert(int offset, String text, int caretAfterInsert) {
        buffer.insertText(offset, text);
        updateAutoPairsForInsert(offset, text.length());
        refreshFromDocument();
        buffer.moveCaret(toPosition(buffer.getDocument().getText(), caretAfterInsert));
    }

    private void executePairInsert(int offset, char opening, char closing) {
        buffer.insertText(offset, "" + opening + closing);
        updateAutoPairsForInsert(offset, 2);
        autoPairs.add(new AutoPair(offset, offset + 1, opening, closing));
        refreshFromDocument();
        buffer.moveCaret(toPosition(buffer.getDocument().getText(), offset + 1));
    }

    private void executeDelete(int start, int end, int caretAfterDelete) {
        if (start >= end) {
            return;
        }

        buffer.deleteText(start, end);
        updateAutoPairsForDelete(start, end);
        refreshFromDocument();
        buffer.moveCaret(toPosition(buffer.getDocument().getText(), caretAfterDelete));
    }

    private void replaceDocumentText(String newText, int caretOffset) {
        buffer.replaceText(newText);
        autoPairs.clear();
        refreshFromDocument();
        buffer.moveCaret(toPosition(buffer.getDocument().getText(), caretOffset));
    }

    private LineRange currentLineRange(String text, int offset) {
        int safeOffset = Math.max(0, Math.min(offset, text.length()));
        int contentStart = findLineStart(text, safeOffset);
        int newline = text.indexOf('\n', contentStart);
        int contentEnd = newline < 0 ? text.length() : newline;
        int nextLineStart = newline < 0 ? text.length() : newline + 1;
        return new LineRange(contentStart, contentEnd, nextLineStart);
    }

    private int findLineStart(String text, int offset) {
        int safeOffset = Math.max(0, Math.min(offset, text.length()));
        int lineStart = text.lastIndexOf('\n', Math.max(0, safeOffset - 1));
        return lineStart < 0 ? 0 : lineStart + 1;
    }

    private String leadingIndent(String text, int lineStart) {
        int offset = lineStart;
        while (offset < text.length()) {
            char current = text.charAt(offset);
            if (current != ' ' && current != '\t') {
                break;
            }
            offset++;
        }
        return text.substring(lineStart, offset);
    }

    private boolean shouldInsertSingleQuoteOnly(char quote, int offset, String text) {
        return isInsideStringToken(offset);
    }

    private boolean skipClosingDelimiter(char closing, int offset, String text) {
        AutoPair pair = findAutoPairClosingAt(offset, closing, text);
        if (pair == null) {
            return false;
        }

        buffer.moveCaret(toPosition(text, offset + 1));
        return true;
    }

    private boolean deleteAutoPair() {
        int caretOffset = textPane.getCaretPosition();
        String text = buffer.getDocument().getText();
        AutoPair pair = findAutoPairAroundCaret(caretOffset, text);
        if (pair == null) {
            return false;
        }

        executeDelete(pair.openOffset, pair.closeOffset + 1, pair.openOffset);
        return true;
    }

    private AutoPair findAutoPairClosingAt(int offset, char closing, String text) {
        for (AutoPair pair : autoPairs) {
            if (pair.closeOffset == offset
                    && pair.closing == closing
                    && pair.matches(text)) {
                return pair;
            }
        }
        return null;
    }

    private AutoPair findAutoPairAroundCaret(int offset, String text) {
        for (AutoPair pair : autoPairs) {
            if (pair.openOffset == offset - 1
                    && pair.closeOffset == offset
                    && pair.matches(text)) {
                return pair;
            }
        }
        return null;
    }

    private void updateAutoPairsForInsert(int offset, int length) {
        if (length <= 0 || autoPairs.isEmpty()) {
            return;
        }

        for (AutoPair pair : autoPairs) {
            if (offset <= pair.openOffset) {
                pair.openOffset += length;
                pair.closeOffset += length;
            } else if (offset <= pair.closeOffset) {
                pair.closeOffset += length;
            }
        }
    }

    private void updateAutoPairsForDelete(int start, int end) {
        if (start >= end || autoPairs.isEmpty()) {
            return;
        }

        int length = end - start;
        Iterator<AutoPair> iterator = autoPairs.iterator();
        while (iterator.hasNext()) {
            AutoPair pair = iterator.next();
            boolean deletesOpening = start <= pair.openOffset && pair.openOffset < end;
            boolean deletesClosing = start <= pair.closeOffset && pair.closeOffset < end;
            if (deletesOpening || deletesClosing) {
                iterator.remove();
            } else if (end <= pair.openOffset) {
                pair.openOffset -= length;
                pair.closeOffset -= length;
            } else if (pair.openOffset < start && end <= pair.closeOffset) {
                pair.closeOffset -= length;
            }
        }
    }

    private boolean isImmediatelyBeforeQuote(char quote, int offset, String text) {
        return offset < text.length() && text.charAt(offset) == quote;
    }

    private boolean isInsideStringToken(int offset) {
        if (latestSyntaxSnapshot == null || latestSyntaxSnapshot.isEmpty()) {
            return false;
        }

        for (SyntaxToken token : latestSyntaxSnapshot.getTokens()) {
            if (token.type() == TokenType.STRING
                    && offset > token.startOffset()
                    && offset < token.endOffset()) {
                return true;
            }
        }
        return false;
    }

    private boolean isQuote(char ch) {
        return ch == '"' || ch == '\'';
    }

    private static final class AutoPair {
        private int openOffset;
        private int closeOffset;
        private final char opening;
        private final char closing;

        private AutoPair(int openOffset, int closeOffset, char opening, char closing) {
            this.openOffset = openOffset;
            this.closeOffset = closeOffset;
            this.opening = opening;
            this.closing = closing;
        }

        private boolean matches(String text) {
            return openOffset >= 0
                    && closeOffset >= 0
                    && openOffset < text.length()
                    && closeOffset < text.length()
                    && text.charAt(openOffset) == opening
                    && text.charAt(closeOffset) == closing;
        }
    }

    private static final class LineRange {
        private final int contentStart;
        private final int contentEnd;
        private final int nextLineStart;
        private final int fullEnd;

        private LineRange(int contentStart, int contentEnd, int nextLineStart) {
            this.contentStart = contentStart;
            this.contentEnd = contentEnd;
            this.nextLineStart = nextLineStart;
            this.fullEnd = nextLineStart;
        }
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        styledDocument.removeDocumentListener(documentListener);
        textPane.removeCaretListener(caretListener);
        textPane.removeFocusListener(focusListener);
        buffer.getDocument().removeTextChangeListener(textChangeListener);
        caretSync.dispose();
        buffer.clearListeners();
        completionPopup.hide();
    }

    public void refreshFromDocument() {
        if (disposed) return;
        if (buffer.getDocument().getText().contentEquals(getCurrentText())) {
            return;
        }
        refreshing = true;
        caretSync.setRefreshing(true);
        try {
            styledDocument.remove(0, styledDocument.getLength());
            insertDocumentText(buffer.getDocument().getText());
            renderSyntax();
            refreshDiagnostics();
            refreshLanguageContext();
            refreshCompletions();
            gutterPanel.refresh();
        } catch (BadLocationException ex) {
            throw new IllegalStateException("Failed to refresh editor document", ex);
        } finally {
            caretSync.setRefreshing(false);
            refreshing = false;
        }
    }

    public EditorBuffer getBuffer() {
        return buffer;
    }

    public JTextPane getTextPane() {
        return textPane;
    }

    public StyledDocument getStyledDocument() {
        return styledDocument;
    }

    private void syncInsert(int offset, int length) {
        if (disposed || refreshing) return;
        try {
            String insertedText = styledDocument.getText(offset, length);
            syncingFromSwing = true;
            try {
                buffer.getDocument().insert(offset, insertedText);
                updateAutoPairsForInsert(offset, insertedText.length());
            } finally {
                syncingFromSwing = false;
            }
            scheduleRender();
        } catch (BadLocationException ex) {
            throw new IllegalStateException("Failed to sync insert", ex);
        }
    }

    private void syncDelete(int offset, int length) {
        if (disposed || refreshing) return;
        syncingFromSwing = true;
        try {
            buffer.getDocument().delete(offset, offset + length);
            updateAutoPairsForDelete(offset, offset + length);
        } finally {
            syncingFromSwing = false;
        }
        scheduleRender();
    }

    private void scheduleRender() {
        if (disposed || renderPending) return;
        renderPending = true;
        SwingUtilities.invokeLater(() -> {
            renderPending = false;
            if (disposed) return;
            renderSyntax();
            refreshDiagnostics();
            refreshLanguageContext();
            refreshCompletions();
            gutterPanel.refresh();
        });
    }

    private void insertDocumentText(String text) {
        if ((text == null ? "" : text).contentEquals(getCurrentText())) {
            return;
        }
        try {
            styledDocument.insertString(0, text == null ? "" : text, null);
        } catch (BadLocationException ex) {
            throw new IllegalStateException("Failed to initialize editor document", ex);
        }
    }

    private String getCurrentText() {
        try {
            return styledDocument.getText(0, styledDocument.getLength());
        } catch (BadLocationException ex) {
            throw new IllegalStateException("Failed to read editor document", ex);
        }
    }

    private void renderSyntax() {
        latestSyntaxSnapshot = analyzer.analyze(buffer.getDocument());
        renderer.render(latestSyntaxSnapshot);
    }

    private void refreshDiagnostics() {
        diagnosticManager.refresh(buffer.getDocument());
        buffer.setDiagnostics(diagnosticManager.getSnapshot());
    }

    private void refreshLanguageContext() {
        languageManager.refresh(buffer, latestSyntaxSnapshot);
        buffer.setLanguageContext(languageManager.getContext());
    }

    private void refreshCompletions() {
        refreshCompletions(false);
    }

    private void forceOpenCompletions() {
        if (disposed) return;
        renderSyntax();
        refreshDiagnostics();
        refreshLanguageContext();
        refreshCompletions(true);
    }

    private void refreshCompletions(boolean forceOpen) {
        if (disposed) return;
        completionManager.refresh(buffer.getLanguageContext());
        buffer.setCompletionSnapshot(completionManager.getSnapshot());

        if (suppressPopup) return;

        if ((!forceOpen && !isCompletionContextValid()) || buffer.getCompletionSnapshot().isEmpty()) {
            completionPopup.hide();
            return;
        }

        if (completionPopup.isVisible()) {
            completionPopup.update(buffer.getCompletionSnapshot());
            completionPopup.move(textPane, textPane.getCaretPosition());
        } else {
            completionPopup.show(textPane, buffer.getCompletionSnapshot(), textPane.getCaretPosition());
        }
    }

    private boolean isCompletionContextValid() {
        String prefix = completionPrefixResolver.resolve(buffer.getLanguageContext());
        if (prefix.length() < 1) return false;

        String text = buffer.getDocument().getText();
        int offset = toOffset(text, buffer.getCaret());
        if (offset == 0) return false;
        char charBefore = text.charAt(offset - 1);
        return Character.isJavaIdentifierPart(charBefore);
    }

    private int toOffset(String text, com.eyecode.editor.v2.EditorPosition position) {
        int line = 0;
        int column = 0;
        for (int offset = 0; offset < text.length(); offset++) {
            if (line == position.line() && column == position.column()) {
                return offset;
            }
            char current = text.charAt(offset);
            if (current == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
        }
        return text.length();
    }

    private EditorPosition toPosition(String text, int offset) {
        int safeOffset = Math.max(0, Math.min(offset, text.length()));
        int line = 0;
        int column = 0;
        for (int i = 0; i < safeOffset; i++) {
            if (text.charAt(i) == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
        }
        return new EditorPosition(line, column);
    }
}
