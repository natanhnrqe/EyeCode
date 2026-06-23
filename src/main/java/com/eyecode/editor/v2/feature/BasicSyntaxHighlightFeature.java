package com.eyecode.editor.v2.feature;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.syntax.JavaSyntaxAnalyzer;
import com.eyecode.editor.v2.syntax.SyntaxAnalyzer;
import com.eyecode.editor.v2.syntax.SyntaxSnapshot;

public final class BasicSyntaxHighlightFeature implements EditorFeature {

    private final SyntaxAnalyzer analyzer = new JavaSyntaxAnalyzer();
    private EditorBuffer buffer;
    private SyntaxSnapshot snapshot = new SyntaxSnapshot(java.util.List.of());

    @Override
    public void attach(EditorBuffer buffer) {
        this.buffer = buffer;
        rebuildSnapshot();
    }

    @Override
    public void detach() {
        this.buffer = null;
        this.snapshot = new SyntaxSnapshot(java.util.List.of());
    }

    @Override
    public void onDocumentChanged() {
        rebuildSnapshot();
    }

    @Override
    public void onCaretMoved(EditorPosition position) {
        // Syntax metadata is independent of caret movement for now.
    }

    public SyntaxSnapshot getSnapshot() {
        return snapshot;
    }

    private void rebuildSnapshot() {
        if (buffer == null) return;
        snapshot = analyzer.analyze(buffer.getDocument());
    }
}
