package com.eyecode.javafx.editor;

import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.syntax.JavaSyntaxAnalyzer;
import com.eyecode.editor.v2.syntax.SyntaxSnapshot;
import org.fxmisc.richtext.CodeArea;

import java.util.Optional;

public final class HighlightPipeline {

    private final JavaSyntaxAnalyzer analyzer;
    private final JavaFxSyntaxRenderer renderer;

    public HighlightPipeline(CodeArea codeArea) {
        this.analyzer = new JavaSyntaxAnalyzer();
        this.renderer = new JavaFxSyntaxRenderer(codeArea);
    }

    public void refresh(EditorDocument document) {
        if (document == null) return;
        SyntaxSnapshot snapshot = analyzer.analyze(document);
        renderer.render(snapshot);
    }

    public void refresh(EditorDocument document, Optional<?> change) {
        refresh(document);
    }
}