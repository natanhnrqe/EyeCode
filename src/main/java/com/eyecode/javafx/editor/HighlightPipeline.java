package com.eyecode.javafx.editor;

import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.syntax.JavaSyntaxAnalyzer;
import com.eyecode.editor.v2.syntax.SyntaxSnapshot;
import org.fxmisc.richtext.CodeArea;

import java.util.Optional;

public final class HighlightPipeline {

    private final JavaSyntaxAnalyzer analyzer;
    private final JavaFxSyntaxRenderer renderer;
    private SyntaxSnapshot latestSnapshot;

    public HighlightPipeline(CodeArea codeArea) {
        this.analyzer = new JavaSyntaxAnalyzer();
        this.renderer = new JavaFxSyntaxRenderer(codeArea);
        this.latestSnapshot = new SyntaxSnapshot(java.util.List.of());
    }

    public SyntaxSnapshot refresh(EditorDocument document) {
        if (document == null) {
            latestSnapshot = new SyntaxSnapshot(java.util.List.of());
            return latestSnapshot;
        }
        latestSnapshot = analyzer.analyze(document);
        renderer.render(latestSnapshot);
        return latestSnapshot;
    }

    public SyntaxSnapshot refresh(EditorDocument document, Optional<?> change) {
        return refresh(document);
    }

    public SyntaxSnapshot latestSnapshot() {
        return latestSnapshot;
    }

    public void dispose() {
    }
}
