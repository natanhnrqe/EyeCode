package com.eyecode.javafx.editor;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.SmartEditResult;
import com.eyecode.editor.intelligence.pipeline.TypingPipeline;
import com.eyecode.editor.intelligence.pipeline.strategy.SmartEditingStrategies;
import com.eyecode.editor.v2.EditorBuffer;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import java.util.function.BooleanSupplier;

public final class JavaFxEditor extends HBox {

    private final EditorBuffer buffer;
    private final TypingPipeline smartEditingPipeline;
    private final JavaFxEditorInputAdapter inputAdapter;
    private final CodeArea codeArea;
    private final VirtualizedScrollPane<CodeArea> scrollPane;
    private BooleanSupplier goToDefinitionAction;

    public JavaFxEditor(EditorBuffer buffer) {
        this(buffer, defaultSmartEditingPipeline());
    }

    public JavaFxEditor(EditorBuffer buffer, TypingPipeline smartEditingPipeline) {
        this.buffer = buffer;
        this.smartEditingPipeline = smartEditingPipeline != null
                ? smartEditingPipeline
                : defaultSmartEditingPipeline();
        this.inputAdapter = new JavaFxEditorInputAdapter();
        codeArea = new CodeArea();
        codeArea.getStyleClass().add("code-area");
        codeArea.setWrapText(false);
        codeArea.useInitialStyleForInsertionProperty().set(true);

        codeArea.setParagraphGraphicFactory(new JavaFxGutterFactory(codeArea));
        installSmartEditingFilters();

        scrollPane = new VirtualizedScrollPane<>(codeArea);
        scrollPane.getStyleClass().add("editor-scroll-pane");
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        getStyleClass().add("editor-root");
        setMaxWidth(Double.MAX_VALUE);
        setMaxHeight(Double.MAX_VALUE);

        getChildren().add(scrollPane);
    }

    private static TypingPipeline defaultSmartEditingPipeline() {
        return new TypingPipeline(SmartEditingStrategies.defaultRegistry());
    }

    private void installSmartEditingFilters() {
        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, this::handleGoToDefinition);
        codeArea.addEventFilter(KeyEvent.KEY_TYPED, this::handleSmartEditing);
        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, this::handleSmartEditing);
    }

    private void handleGoToDefinition(KeyEvent event) {
        handleGoToDefinitionShortcut(event);
    }

    boolean handleGoToDefinitionShortcut(KeyEvent event) {
        if (!isGoToDefinitionKey(event)) {
            return false;
        }
        if (goToDefinition()) {
            event.consume();
            return true;
        }
        return false;
    }

    private void handleSmartEditing(KeyEvent event) {
        int caretOffset = codeArea.getCaretPosition();
        javafx.scene.control.IndexRange selection = codeArea.getSelection();
        EditorInputEvent input = inputAdapter.adapt(
                event,
                caretOffset,
                buffer.getDocument().currentVersion(),
                new TextRange(selection.getStart(), selection.getEnd())
        );
        SmartEditResult result = smartEditingPipeline.process(input, new EditorCommandContext(buffer));
        if (result.isHandled()) {
            event.consume();
        }
    }

    private boolean isGoToDefinitionKey(KeyEvent event) {
        return event.getEventType() == KeyEvent.KEY_PRESSED
                && event.getCode() != null
                && event.getCode().getName().equalsIgnoreCase("B")
                && event.isControlDown()
                && !event.isShiftDown()
                && !event.isAltDown()
                && !event.isMetaDown();
    }

    public void setGoToDefinitionAction(BooleanSupplier goToDefinitionAction) {
        this.goToDefinitionAction = goToDefinitionAction;
    }

    public boolean goToDefinition() {
        return goToDefinitionAction != null && goToDefinitionAction.getAsBoolean();
    }

    public void revealOffset(int offset) {
        if (offset < 0) {
            return;
        }
        String text = codeArea.getText();
        int clamped = Math.min(offset, Math.max(0, text.length()));
        try {
            buffer.moveCaret(buffer.getDocument().positionOf(clamped));
        } catch (RuntimeException ignored) {
            return;
        }
        codeArea.moveTo(clamped);
        codeArea.requestFollowCaret();
    }

    public CodeArea getCodeArea() {
        return codeArea;
    }

    public VirtualizedScrollPane<CodeArea> getScrollPane() {
        return scrollPane;
    }

    public String getText() {
        return codeArea.getText();
    }

    public void setText(String text) {
        codeArea.replaceText(text);
    }
}
