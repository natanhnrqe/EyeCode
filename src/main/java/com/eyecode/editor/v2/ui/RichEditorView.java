package com.eyecode.editor.v2.ui;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.EditorSelection;
import com.eyecode.editor.v2.caret.CaretSynchronizationManager;
import com.eyecode.editor.v2.completion.CompletionEngine;
import com.eyecode.editor.v2.completion.CompletionManager;
import com.eyecode.editor.v2.completion.JavaKeywordCompletionProvider;
import com.eyecode.editor.v2.completion.JavaSnippetProvider;
import com.eyecode.editor.v2.completion.JavaStandardLibraryProvider;
import com.eyecode.editor.v2.completion.insert.CompletionInsertionContext;
import com.eyecode.editor.v2.completion.insert.CompletionInsertionEngine;
import com.eyecode.editor.v2.completion.insert.CompletionPrefixResolver;
import com.eyecode.editor.v2.completion.knowledge.JavaKnowledgeBaseProvider;
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
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
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
    private final JPanel searchPanel;
    private final JTextField searchField;
    private final JTextField replaceField;
    private final JLabel searchStatusLabel;
    private final List<SearchMatch> searchMatches;
    private final List<Object> searchHighlightTags;
    private final Highlighter.HighlightPainter searchHighlightPainter;
    private final Highlighter.HighlightPainter currentSearchHighlightPainter;
    private SyntaxSnapshot latestSyntaxSnapshot;
    private boolean refreshing;
    private boolean suppressPopup;
    private boolean disposed;
    private boolean syncingFromSwing;
    private boolean renderPending;
    private int currentSearchIndex = -1;
    private boolean replaceMode;

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
                        new JavaKnowledgeBaseProvider(),
                        new JavaStandardLibraryProvider(),
                        new JavaSnippetProvider(),
                        new SemanticCompletionProvider(new SemanticSymbolRegistry())
                )
        ));
        this.completionPopup = new CompletionPopup();
        this.completionInsertionEngine = new CompletionInsertionEngine();
        this.completionPrefixResolver = new CompletionPrefixResolver();
        this.autoPairs = new ArrayList<>();
        this.searchMatches = new ArrayList<>();
        this.searchHighlightTags = new ArrayList<>();
        this.searchHighlightPainter = new DefaultHighlighter.DefaultHighlightPainter(ColorManager.ACCENT_HOVER_BG);
        this.currentSearchHighlightPainter = new DefaultHighlighter.DefaultHighlightPainter(ColorManager.AUTOCOMPLETE_SELECTION_BG);
        this.searchField = new JTextField(24);
        this.replaceField = new JTextField(24);
        this.searchStatusLabel = new JLabel("0/0");
        this.searchPanel = createSearchPanel();
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

        add(searchPanel, BorderLayout.NORTH);
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

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 4));
        panel.setBackground(ColorManager.TOOLBAR_BG);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorManager.BORDER_DIVIDER));
        panel.setVisible(false);

        JLabel findLabel = new JLabel("Find");
        findLabel.setForeground(ColorManager.TEXT_SECONDARY);
        findLabel.setFont(TypographyManager.UI_SMALL());

        JLabel replaceLabel = new JLabel("Replace");
        replaceLabel.setForeground(ColorManager.TEXT_SECONDARY);
        replaceLabel.setFont(TypographyManager.UI_SMALL());

        searchField.setFont(TypographyManager.UI_CODE());
        searchField.setBackground(ColorManager.INPUT_BG);
        searchField.setForeground(ColorManager.TEXT_PRIMARY);
        searchField.setCaretColor(ColorManager.EDITOR_CARET);
        searchField.setSelectionColor(ColorManager.EDITOR_SELECTION);
        searchField.setSelectedTextColor(ColorManager.TEXT_PRIMARY);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorManager.BORDER),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)
        ));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { refreshSearchHighlights(); }
            @Override
            public void removeUpdate(DocumentEvent e) { refreshSearchHighlights(); }
            @Override
            public void changedUpdate(DocumentEvent e) { refreshSearchHighlights(); }
        });

        replaceField.setFont(TypographyManager.UI_CODE());
        replaceField.setBackground(ColorManager.INPUT_BG);
        replaceField.setForeground(ColorManager.TEXT_PRIMARY);
        replaceField.setCaretColor(ColorManager.EDITOR_CARET);
        replaceField.setSelectionColor(ColorManager.EDITOR_SELECTION);
        replaceField.setSelectedTextColor(ColorManager.TEXT_PRIMARY);
        replaceField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorManager.BORDER),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)
        ));

        InputMap searchInputMap = searchField.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap searchActionMap = searchField.getActionMap();
        searchInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "searchNext");
        searchActionMap.put("searchNext", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { navigateSearch(1); }
        });
        searchInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "searchPrevious");
        searchActionMap.put("searchPrevious", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { navigateSearch(-1); }
        });
        searchInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "searchClose");
        searchActionMap.put("searchClose", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { closeSearch(); }
        });

        InputMap replaceInputMap = replaceField.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap replaceActionMap = replaceField.getActionMap();
        replaceInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "replaceNext");
        replaceActionMap.put("replaceNext", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { replaceNext(); }
        });
        replaceInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "searchClose");
        replaceActionMap.put("searchClose", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { closeSearch(); }
        });

        searchStatusLabel.setForeground(ColorManager.TEXT_TERTIARY);
        searchStatusLabel.setFont(TypographyManager.UI_SMALL());

        JButton closeButton = new JButton("x");
        closeButton.setFocusable(false);
        closeButton.setBackground(ColorManager.TOOLBAR_BG);
        closeButton.setForeground(ColorManager.TEXT_SECONDARY);
        closeButton.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        closeButton.addActionListener(e -> closeSearch());

        JButton replaceButton = createSmallButton("Replace");
        replaceButton.addActionListener(e -> replaceNext());
        JButton replaceAllButton = createSmallButton("Replace All");
        replaceAllButton.addActionListener(e -> replaceAll());

        JPanel findRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        findRow.setOpaque(false);
        findRow.add(findLabel);
        findRow.add(searchField);
        findRow.add(searchStatusLabel);
        findRow.add(closeButton);

        JPanel replaceRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        replaceRow.setOpaque(false);
        replaceRow.add(replaceLabel);
        replaceRow.add(replaceField);
        replaceRow.add(replaceButton);
        replaceRow.add(replaceAllButton);

        panel.add(findRow, BorderLayout.NORTH);
        panel.add(replaceRow, BorderLayout.SOUTH);
        return panel;
    }

    private JButton createSmallButton(String text) {
        JButton button = new JButton(text);
        button.setFocusable(false);
        button.setFont(TypographyManager.UI_SMALL());
        button.setBackground(ColorManager.SURFACE_BG);
        button.setForeground(ColorManager.TEXT_SECONDARY);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorManager.BORDER),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        return button;
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

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "editorUndo");
        actionMap.put("editorUndo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undo();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "editorRedo");
        actionMap.put("editorRedo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                redo();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "editorOpenSearch");
        actionMap.put("editorOpenSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openSearch();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK), "editorOpenReplace");
        actionMap.put("editorOpenReplace", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openReplace();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, InputEvent.CTRL_DOWN_MASK), "editorToggleLineComment");
        actionMap.put("editorToggleLineComment", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleLineComments();
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

    private void openSearch() {
        openSearch(false);
    }

    private void openReplace() {
        openSearch(true);
    }

    private void openSearch(boolean replace) {
        completionPopup.hide();
        replaceMode = replace;
        if (!searchPanel.isVisible()) {
            searchPanel.setVisible(true);
            revalidate();
            repaint();
        }

        String selectedText = textPane.getSelectedText();
        if (selectedText != null && !selectedText.contains("\n") && !selectedText.isEmpty()) {
            searchField.setText(selectedText);
        }
        searchField.requestFocusInWindow();
        searchField.selectAll();
        refreshSearchHighlights();
    }

    private void closeSearch() {
        clearSearchHighlights();
        searchMatches.clear();
        currentSearchIndex = -1;
        searchStatusLabel.setText("0/0");
        searchPanel.setVisible(false);
        revalidate();
        repaint();
        textPane.requestFocusInWindow();
    }

    private void refreshSearchHighlights() {
        if (disposed || !searchPanel.isVisible()) {
            return;
        }

        clearSearchHighlights();
        searchMatches.clear();
        String query = searchField.getText();
        if (query == null || query.isEmpty()) {
            currentSearchIndex = -1;
            searchStatusLabel.setText("0/0");
            return;
        }

        String text = buffer.getDocument().getText();
        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();
        int offset = 0;
        while (offset <= lowerText.length() - lowerQuery.length()) {
            int matchStart = lowerText.indexOf(lowerQuery, offset);
            if (matchStart < 0) {
                break;
            }
            int matchEnd = matchStart + query.length();
            searchMatches.add(new SearchMatch(matchStart, matchEnd));
            offset = matchEnd;
        }

        if (searchMatches.isEmpty()) {
            currentSearchIndex = -1;
            searchStatusLabel.setText("0/0");
            return;
        }

        currentSearchIndex = Math.max(0, Math.min(currentSearchIndex, searchMatches.size() - 1));
        applySearchHighlights();
        updateSearchStatus();
    }

    private void navigateSearch(int direction) {
        if (searchMatches.isEmpty()) {
            refreshSearchHighlights();
        }
        if (searchMatches.isEmpty()) {
            return;
        }

        int count = searchMatches.size();
        currentSearchIndex = (currentSearchIndex + direction + count) % count;
        applySearchHighlights();
        updateSearchStatus();
        SearchMatch match = searchMatches.get(currentSearchIndex);
        textPane.setCaretPosition(match.start);
        textPane.moveCaretPosition(match.end);
    }

    private void applySearchHighlights() {
        clearSearchHighlights();
        Highlighter highlighter = textPane.getHighlighter();
        for (int i = 0; i < searchMatches.size(); i++) {
            SearchMatch match = searchMatches.get(i);
            try {
                Object tag = highlighter.addHighlight(
                        match.start,
                        match.end,
                        i == currentSearchIndex ? currentSearchHighlightPainter : searchHighlightPainter
                );
                searchHighlightTags.add(tag);
            } catch (BadLocationException ignored) {
            }
        }
    }

    private void clearSearchHighlights() {
        Highlighter highlighter = textPane.getHighlighter();
        for (Object tag : searchHighlightTags) {
            highlighter.removeHighlight(tag);
        }
        searchHighlightTags.clear();
    }

    private void updateSearchStatus() {
        if (searchMatches.isEmpty()) {
            searchStatusLabel.setText("0/0");
        } else {
            searchStatusLabel.setText((currentSearchIndex + 1) + "/" + searchMatches.size());
        }
    }

    private void replaceNext() {
        String query = searchField.getText();
        String replacement = replaceField.getText();
        if (query == null || query.isEmpty()) return;

        if (searchMatches.isEmpty()) {
            refreshSearchHighlights();
        }
        if (searchMatches.isEmpty()) return;

        if (currentSearchIndex < 0 || currentSearchIndex >= searchMatches.size()) {
            currentSearchIndex = 0;
        }

        SearchMatch match = searchMatches.get(currentSearchIndex);
        buffer.deleteText(match.start, match.end);
        if (replacement != null && !replacement.isEmpty()) {
            buffer.insertText(match.start, replacement);
        }
        autoPairs.clear();
        refreshFromDocument();

        int newCaret = match.start + (replacement == null ? 0 : replacement.length());
        buffer.moveCaret(toPosition(buffer.getDocument().getText(), newCaret));

        refreshSearchHighlights();
        if (!searchMatches.isEmpty()) {
            int count = searchMatches.size();
            currentSearchIndex = (currentSearchIndex + 1 + count) % count;
            if (currentSearchIndex >= searchMatches.size()) {
                currentSearchIndex = 0;
            }
            applySearchHighlights();
            updateSearchStatus();
            SearchMatch nextMatch = searchMatches.get(currentSearchIndex);
            textPane.setCaretPosition(nextMatch.start);
            textPane.moveCaretPosition(nextMatch.end);
        }
    }

    private void replaceAll() {
        String query = searchField.getText();
        String replacement = replaceField.getText();
        if (query == null || query.isEmpty()) return;

        refreshSearchHighlights();
        if (searchMatches.isEmpty()) return;

        String text = buffer.getDocument().getText();
        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();
        int replacementLength = replacement == null ? 0 : replacement.length();

        StringBuilder builder = new StringBuilder();
        int offset = 0;
        int count = 0;
        while (offset <= lowerText.length() - lowerQuery.length()) {
            int matchStart = lowerText.indexOf(lowerQuery, offset);
            if (matchStart < 0) {
                builder.append(text, offset, text.length());
                break;
            }
            builder.append(text, offset, matchStart);
            if (replacement != null) {
                builder.append(replacement);
            }
            offset = matchStart + query.length();
            count++;
        }
        if (offset < text.length() && lowerText.indexOf(lowerQuery, offset) < 0) {
            builder.append(text, offset, text.length());
        }

        buffer.replaceText(builder.toString());
        autoPairs.clear();
        refreshFromDocument();

        String finalText = buffer.getDocument().getText();
        buffer.moveCaret(toPosition(finalText, Math.min(finalText.length(), 0)));

        refreshSearchHighlights();
        if (searchMatches.isEmpty()) {
            currentSearchIndex = -1;
        }
        updateSearchStatus();
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

    private void toggleLineComments() {
        String text = buffer.getDocument().getText();
        int selectionStart = textPane.getSelectionStart();
        int selectionEnd = textPane.getSelectionEnd();
        boolean hasSelection = selectionStart != selectionEnd;
        int blockStart = Math.min(selectionStart, selectionEnd);
        int blockEnd = Math.max(selectionStart, selectionEnd);
        int effectiveEnd = hasSelection && blockEnd > blockStart && text.charAt(blockEnd - 1) == '\n'
                ? blockEnd - 1
                : blockEnd;

        List<LineCommentEdit> edits = collectLineCommentEdits(text, blockStart, effectiveEnd);
        if (edits.isEmpty()) {
            return;
        }

        StringBuilder updated = new StringBuilder(text);
        for (int i = edits.size() - 1; i >= 0; i--) {
            LineCommentEdit edit = edits.get(i);
            if (edit.removeLength > 0) {
                updated.delete(edit.offset, edit.offset + edit.removeLength);
            }
            if (!edit.insertText.isEmpty()) {
                updated.insert(edit.offset, edit.insertText);
            }
        }

        int newSelectionStart = mapOffsetThroughCommentEdits(selectionStart, edits);
        int newSelectionEnd = mapOffsetThroughCommentEdits(selectionEnd, edits);
        if (hasSelection) {
            replaceDocumentText(updated.toString(), newSelectionStart, newSelectionEnd);
        } else {
            replaceDocumentText(updated.toString(), newSelectionStart);
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

    private void replaceDocumentText(String newText, int selectionStart, int selectionEnd) {
        buffer.replaceText(newText);
        autoPairs.clear();
        refreshFromDocument();
        int safeStart = Math.max(0, Math.min(selectionStart, newText.length()));
        int safeEnd = Math.max(0, Math.min(selectionEnd, newText.length()));
        textPane.setCaretPosition(safeStart);
        textPane.moveCaretPosition(safeEnd);
        buffer.setCaretPosition(toPosition(newText, safeEnd));
        buffer.setSelection(new EditorSelection(toPosition(newText, safeStart), toPosition(newText, safeEnd)));
    }

    private List<LineCommentEdit> collectLineCommentEdits(String text, int startOffset, int endOffset) {
        List<LineCommentEdit> edits = new ArrayList<>();
        int lineStart = findLineStart(text, startOffset);
        int safeEnd = Math.max(0, Math.min(endOffset, text.length()));

        while (lineStart <= safeEnd && lineStart <= text.length()) {
            LineRange line = currentLineRange(text, lineStart);
            int indentEnd = findIndentEnd(text, line.contentStart, line.contentEnd);
            if (text.startsWith("//", indentEnd)) {
                int removeLength = indentEnd + 2 < line.contentEnd && text.charAt(indentEnd + 2) == ' '
                        ? 3
                        : 2;
                edits.add(LineCommentEdit.remove(indentEnd, removeLength));
            } else {
                edits.add(LineCommentEdit.insert(indentEnd, "// "));
            }

            if (line.nextLineStart >= text.length()) {
                break;
            }
            lineStart = line.nextLineStart;
        }

        return edits;
    }

    private int findIndentEnd(String text, int lineStart, int lineEnd) {
        int offset = lineStart;
        while (offset < lineEnd) {
            char current = text.charAt(offset);
            if (current != ' ' && current != '\t') {
                break;
            }
            offset++;
        }
        return offset;
    }

    private int mapOffsetThroughCommentEdits(int offset, List<LineCommentEdit> edits) {
        int mapped = offset;
        for (LineCommentEdit edit : edits) {
            int delta = edit.insertText.length() - edit.removeLength;
            if (edit.offset < offset) {
                mapped += delta;
            } else if (edit.offset == offset && delta > 0) {
                mapped += delta;
            }
        }
        return mapped;
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

    private static final class LineCommentEdit {
        private final int offset;
        private final int removeLength;
        private final String insertText;

        private LineCommentEdit(int offset, int removeLength, String insertText) {
            this.offset = offset;
            this.removeLength = removeLength;
            this.insertText = insertText;
        }

        private static LineCommentEdit insert(int offset, String text) {
            return new LineCommentEdit(offset, 0, text);
        }

        private static LineCommentEdit remove(int offset, int length) {
            return new LineCommentEdit(offset, length, "");
        }
    }

    private static final class SearchMatch {
        private final int start;
        private final int end;

        private SearchMatch(int start, int end) {
            this.start = start;
            this.end = end;
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
        clearSearchHighlights();
    }

    public void undo() {
        applyHistoryAction(true);
    }

    public void redo() {
        applyHistoryAction(false);
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
            refreshSearchHighlights();
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

    private void applyHistoryAction(boolean undo) {
        if (disposed) return;
        if (undo && !buffer.canUndo()) return;
        if (!undo && !buffer.canRedo()) return;

        String beforeText = buffer.getDocument().getText();
        int beforeCaret = textPane.getCaretPosition();
        if (undo) {
            buffer.undo();
        } else {
            buffer.redo();
        }

        autoPairs.clear();
        completionPopup.hide();
        String afterText = buffer.getDocument().getText();
        int afterCaret = mapCaretAfterTextChange(beforeText, afterText, beforeCaret);
        refreshFromDocument();
        buffer.moveCaret(toPosition(afterText, afterCaret));
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
            refreshSearchHighlights();
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

    private int mapCaretAfterTextChange(String before, String after, int caretOffset) {
        int safeCaret = Math.max(0, Math.min(caretOffset, before.length()));
        int prefix = 0;
        int maxPrefix = Math.min(before.length(), after.length());
        while (prefix < maxPrefix && before.charAt(prefix) == after.charAt(prefix)) {
            prefix++;
        }

        int suffix = 0;
        int beforeRemaining = before.length() - prefix;
        int afterRemaining = after.length() - prefix;
        while (suffix < beforeRemaining
                && suffix < afterRemaining
                && before.charAt(before.length() - 1 - suffix) == after.charAt(after.length() - 1 - suffix)) {
            suffix++;
        }

        int oldChangeEnd = before.length() - suffix;
        int newChangeEnd = after.length() - suffix;
        int oldChangeLength = oldChangeEnd - prefix;
        int newChangeLength = newChangeEnd - prefix;

        if (safeCaret > oldChangeEnd) {
            return Math.max(0, Math.min(after.length(), safeCaret - oldChangeLength + newChangeLength));
        }
        if (safeCaret >= prefix) {
            int offsetIntoChange = safeCaret - prefix;
            if (oldChangeLength == 0 && offsetIntoChange == 0) {
                return newChangeEnd;
            }
            return Math.max(0, Math.min(after.length(), prefix + Math.min(offsetIntoChange, newChangeLength)));
        }
        return Math.max(0, Math.min(after.length(), safeCaret));
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
