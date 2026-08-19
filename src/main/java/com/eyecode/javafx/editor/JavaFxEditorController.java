package com.eyecode.javafx.editor;

import com.eyecode.editor.intelligence.caret.CaretModel;
import com.eyecode.editor.intelligence.caret.DefaultCaretModel;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.events.DocumentChangeListener;
import com.eyecode.editor.intelligence.events.DocumentTextChangeEvent;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.EditorSelection;
import com.eyecode.editor.v2.command.ReplaceTextCommand;
import com.eyecode.editor.v2.completion.CompletionEngine;
import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;
import com.eyecode.editor.v2.completion.CompletionManager;
import com.eyecode.editor.v2.completion.CompletionSnapshot;
import com.eyecode.editor.v2.completion.ContextAwareCompletionProvider;
import com.eyecode.editor.v2.completion.JavaKeywordCompletionProvider;
import com.eyecode.editor.v2.completion.JavaSnippetProvider;
import com.eyecode.editor.v2.completion.JavaStandardLibraryProvider;
import com.eyecode.editor.v2.completion.insert.CompletionInsertionContext;
import com.eyecode.editor.v2.completion.insert.CompletionInsertionEngine;
import com.eyecode.editor.v2.completion.insert.CompletionPrefixResolver;
import com.eyecode.editor.v2.completion.insert.SnippetInsertionEngine;
import com.eyecode.editor.v2.completion.knowledge.JavaKnowledgeBaseProvider;
import com.eyecode.editor.v2.completion.semantic.SemanticCompletionProvider;
import com.eyecode.editor.v2.completion.semantic.SemanticSymbolRegistry;
import com.eyecode.editor.v2.diagnostics.DiagnosticSnapshot;
import com.eyecode.editor.v2.language.DefaultLanguageService;
import com.eyecode.editor.v2.language.LanguageManager;
import com.eyecode.language.semantic.DefinitionLocation;
import com.eyecode.workbench.editor.EditorManager;
import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.CodeArea;
import org.reactfx.Subscription;

import java.util.List;
import java.util.Optional;

/**
 * Keeps the {@link CodeArea} and the {@link EditorBuffer} model in sync.
 * <p>
 * Normal typing flows codeArea -> document; smart edits and undo/redo flow
 * document -> codeArea. Both directions are guarded against feedback loops.
 * Selection changes produced by smart editing (Ctrl+W expansion) are projected
 * onto the view through the {@link CaretModel}, the offset-based caret +
 * selection abstraction of the Core.
 */
public final class JavaFxEditorController {

    private final JavaFxEditor editor;
    private final EditorBuffer buffer;
    private final HighlightPipeline pipeline;
    private final LanguageManager languageManager;
    private final CompletionManager completionManager;
    private final CompletionPrefixResolver completionPrefixResolver;
    private final CompletionInsertionEngine completionInsertionEngine;
    private final SnippetInsertionEngine snippetInsertionEngine;
    private final JavaFxCompletionPopup completionPopup;
    private final Subscription changeSubscription;
    private final DocumentChangeListener documentChangeListener;
    private final EditorBuffer.CaretChangeListener caretChangeListener;
    private final EditorBuffer.SelectionChangeListener selectionChangeListener;
    private EditorManager manager;
    private String sessionId;
    private boolean syncing;

    public JavaFxEditorController(JavaFxEditor editor, EditorBuffer buffer) {
        this.editor = editor;
        this.buffer = buffer;
        this.pipeline = new HighlightPipeline(editor.getCodeArea());
        this.languageManager = new LanguageManager(new DefaultLanguageService());
        this.completionManager = new CompletionManager(new CompletionEngine(List.of(
                new JavaKeywordCompletionProvider(),
                new ContextAwareCompletionProvider(),
                new JavaKnowledgeBaseProvider(),
                new JavaStandardLibraryProvider(),
                new JavaSnippetProvider(),
                new SemanticCompletionProvider(new SemanticSymbolRegistry())
        )));
        this.completionPrefixResolver = new CompletionPrefixResolver();
        this.completionInsertionEngine = new CompletionInsertionEngine();
        this.snippetInsertionEngine = new SnippetInsertionEngine();
        this.completionPopup = new JavaFxCompletionPopup();

        this.changeSubscription = editor.getCodeArea()
                .multiPlainChanges()
                .subscribe(changes -> refreshFromEditor());

        this.documentChangeListener = this::refreshFromDocument;
        this.caretChangeListener = this::syncCaretToView;
        this.selectionChangeListener = this::syncSelectionToView;
        buffer.getDocument().addDocumentChangeListener(documentChangeListener);
        buffer.addCaretChangeListener(caretChangeListener);
        buffer.addSelectionChangeListener(selectionChangeListener);
        editor.setGoToDefinitionAction(this::goToDefinition);
        editor.setCompletionEventHandler(this::handleCompletionEvent);
        editor.getCodeArea().caretPositionProperty().addListener((obs, oldValue, newValue) -> syncCaretFromEditor());
        editor.getCodeArea().selectionProperty().addListener((obs, oldValue, newValue) -> syncSelectionFromEditor());
        editor.getCodeArea().focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) {
                completionPopup.hide();
            }
        });
        completionPopup.setOnAccept(this::acceptCompletion);
    }

    public void loadDocument() {
        EditorDocument document = buffer.getDocument();
        editor.setText(document.getText());
        refreshLanguageState(document);
    }

    private void refreshFromEditor() {
        if (syncing) {
            return;
        }
        EditorDocument document = buffer.getDocument();
        if (document != null) {
            document.setText(editor.getText());
            syncCaretFromEditor();
            syncSelectionFromEditor();
            refreshLanguageState(document);
            invokeCompletion(false);
        }
    }

    private void refreshFromDocument(DocumentTextChangeEvent event) {
        if (syncing) {
            return;
        }
        EditorDocument document = buffer.getDocument();
        String newText = document.getText();
        CodeArea codeArea = editor.getCodeArea();
        if (!newText.contentEquals(codeArea.getText())) {
            syncing = true;
            try {
                codeArea.replaceText(newText);
                codeArea.moveTo(offsetOf(buffer.getCaret()));
                codeArea.requestFollowCaret();
            } finally {
                syncing = false;
            }
        }
        refreshLanguageState(document);
        if (completionPopup.isShowing()) {
            invokeCompletion(false);
        }
    }

    private void syncCaretToView(EditorPosition position) {
        if (syncing) {
            return;
        }
        CodeArea codeArea = editor.getCodeArea();
        int offset = offsetOf(position);
        if (codeArea.getCaretPosition() != offset) {
            syncing = true;
            try {
                codeArea.moveTo(offset);
                codeArea.requestFollowCaret();
            } finally {
                syncing = false;
            }
        }
        refreshLanguageState(buffer.getDocument());
        if (completionPopup.isShowing()) {
            invokeCompletion(false);
        }
    }

    private void syncSelectionToView(EditorSelection selection) {
        if (syncing) {
            return;
        }
        CaretModel caretModel = new DefaultCaretModel(buffer);
        Optional<TextRange> range = caretModel.selection();
        CodeArea codeArea = editor.getCodeArea();
        syncing = true;
        try {
            if (range.isPresent()) {
                codeArea.selectRange(range.get().startOffset(), range.get().endOffset());
            } else {
                int offset = offsetOf(selection.getEnd());
                codeArea.selectRange(offset, offset);
            }
            codeArea.requestFollowCaret();
        } finally {
            syncing = false;
        }
        refreshLanguageState(buffer.getDocument());
    }

    private void syncCaretFromEditor() {
        if (syncing) {
            return;
        }
        EditorDocument document = buffer.getDocument();
        int offset = editor.getCodeArea().getCaretPosition();
        buffer.setCaretPosition(document.positionOf(offset));
    }

    private void syncSelectionFromEditor() {
        if (syncing) {
            return;
        }
        EditorDocument document = buffer.getDocument();
        IndexRange selection = editor.getCodeArea().getSelection();
        EditorPosition anchor = document.positionOf(selection.getStart());
        EditorPosition caret = document.positionOf(selection.getEnd());
        buffer.setSelection(new EditorSelection(anchor, caret));
    }

    private int offsetOf(EditorPosition position) {
        return buffer.getDocument().offsetOf(position);
    }

    public void bindNavigation(EditorManager manager, String sessionId) {
        this.manager = manager;
        this.sessionId = sessionId;
    }

    public boolean goToDefinition() {
        if (manager == null || sessionId == null) {
            return false;
        }
        syncCaretFromEditor();
        int caretOffset = editor.getCodeArea().getCaretPosition();
        Optional<DefinitionLocation> location = manager.resolveDefinition(sessionId, caretOffset);
        if (location.isEmpty()) {
            return false;
        }
        editor.revealOffset(location.get().declarationRange().startOffset());
        return true;
    }

    boolean handleCompletionEvent(KeyEvent event) {
        if (completionPopup.isShowing()) {
            if (event.getEventType() != KeyEvent.KEY_PRESSED) {
                return false;
            }
            if (event.getCode() == KeyCode.ESCAPE) {
                completionPopup.hide();
                return true;
            }
            if (event.getCode() == KeyCode.DOWN) {
                completionPopup.selectNext();
                return true;
            }
            if (event.getCode() == KeyCode.UP) {
                completionPopup.selectPrevious();
                return true;
            }
            if (event.getCode() == KeyCode.PAGE_DOWN) {
                completionPopup.selectPageDown();
                return true;
            }
            if (event.getCode() == KeyCode.PAGE_UP) {
                completionPopup.selectPageUp();
                return true;
            }
            if (event.getCode() == KeyCode.HOME) {
                completionPopup.selectFirst();
                return true;
            }
            if (event.getCode() == KeyCode.END) {
                completionPopup.selectLast();
                return true;
            }
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
                completionPopup.acceptSelected();
                return true;
            }
        }
        if (isManualCompletionKey(event)) {
            invokeCompletion(true);
            return true;
        }
        return false;
    }

    public void invokeCompletion(boolean manual) {
        refreshLanguageState(buffer.getDocument());
        if (isCompletionSuppressed()) {
            completionPopup.hide();
            buffer.setCompletionSnapshot(CompletionSnapshot.empty());
            return;
        }
        completionManager.refresh(buffer.getLanguageContext(), manual);
        CompletionSnapshot snapshot = completionManager.getSnapshot();
        buffer.setCompletionSnapshot(snapshot);
        if (snapshot.isEmpty()) {
            completionPopup.hide();
            return;
        }
        int caretOffset = editor.getCodeArea().getCaretPosition();
        if (completionPopup.isShowing()) {
            completionPopup.update(editor.getCodeArea(), snapshot, caretOffset);
        } else {
            completionPopup.show(editor.getCodeArea(), snapshot, caretOffset);
        }
    }

    private void acceptCompletion(CompletionItem item) {
        if (item == null) {
            return;
        }
        syncCaretFromEditor();
        syncSelectionFromEditor();
        String prefix = completionPrefixResolver.resolve(buffer.getLanguageContext());
        String before = buffer.getDocument().getText();
        EditorDocument document = buffer.getDocument();
        int caretOffset = document.offsetOf(buffer.getCaret());
        int newCaretOffset = caretOffset;
        document.beginBatch();
        try {
            if (item.getKind() == CompletionItemKind.SNIPPET) {
                SnippetInsertionEngine.SnippetResult result = snippetInsertionEngine.insert(
                        document,
                        buffer.getCaret(),
                        prefix,
                        item.getInsertText()
                );
                newCaretOffset = result.caretOffset();
            } else {
                completionInsertionEngine.insert(new CompletionInsertionContext(
                        document,
                        buffer.getCaret(),
                        item,
                        prefix
                ));
                newCaretOffset = Math.max(0, caretOffset - prefix.length()) + item.getInsertText().length();
            }
            document.endBatch();
        } catch (RuntimeException ex) {
            document.abortBatch();
            throw ex;
        }
        String after = document.getText();
        if (!before.equals(after)) {
            buffer.getCommandManager().recordGroup(new ReplaceTextCommand(before, after));
        }
        buffer.moveCaret(document.positionOf(newCaretOffset));
        buffer.setCompletionSelection(item);
        refreshLanguageState(document);
        completionPopup.hide();
    }

    private void refreshLanguageState(EditorDocument document) {
        buffer.setDiagnostics(DiagnosticSnapshot.empty());
        languageManager.refresh(buffer, pipeline.refresh(document));
        buffer.setLanguageContext(languageManager.getContext());
    }

    private boolean isCompletionSuppressed() {
        if (buffer.getLanguageContext() == null) {
            return true;
        }
        int offset = buffer.getDocument().offsetOf(buffer.getCaret());
        return buffer.getLanguageContext().getSyntax().getTokens().stream()
                .anyMatch(token -> token.startOffset() <= offset
                        && offset <= token.endOffset()
                        && (token.type() == com.eyecode.editor.v2.syntax.TokenType.COMMENT
                        || token.type() == com.eyecode.editor.v2.syntax.TokenType.STRING));
    }

    private boolean isManualCompletionKey(KeyEvent event) {
        return event.getEventType() == KeyEvent.KEY_PRESSED
                && event.getCode() == KeyCode.SPACE
                && event.isControlDown()
                && !event.isShiftDown()
                && !event.isAltDown()
                && !event.isMetaDown();
    }

    JavaFxCompletionPopup completionPopup() {
        return completionPopup;
    }

    public void syncToDocument() {
        if (buffer != null && buffer.getDocument() != null) {
            buffer.getDocument().setText(editor.getText());
        }
    }

    public void dispose() {
        completionPopup.hide();
        changeSubscription.unsubscribe();
        buffer.getDocument().removeDocumentChangeListener(documentChangeListener);
        buffer.removeCaretChangeListener(caretChangeListener);
        buffer.removeSelectionChangeListener(selectionChangeListener);
    }
}
