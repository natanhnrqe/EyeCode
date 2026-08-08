package com.eyecode.javafx.editor;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.PassthroughSmartEditStrategy;
import com.eyecode.editor.intelligence.pipeline.SmartEditingRegistry;
import com.eyecode.editor.intelligence.pipeline.SmartEditResult;
import com.eyecode.editor.intelligence.pipeline.TypingPipeline;
import com.eyecode.editor.v2.EditorBuffer;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

public final class JavaFxEditor extends HBox {

    private final EditorBuffer buffer;
    private final TypingPipeline smartEditingPipeline;
    private final JavaFxEditorInputAdapter inputAdapter;
    private final CodeArea codeArea;
    private final VirtualizedScrollPane<CodeArea> scrollPane;

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
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(new PassthroughSmartEditStrategy());
        return new TypingPipeline(registry);
    }

    private void installSmartEditingFilters() {
        codeArea.addEventFilter(KeyEvent.KEY_TYPED, this::handleSmartEditing);
        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, this::handleSmartEditing);
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
