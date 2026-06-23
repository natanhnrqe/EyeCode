package com.eyecode.editor.v2.syntax;

import com.eyecode.editor.v2.EditorDocument;

public interface SyntaxAnalyzer {

    SyntaxSnapshot analyze(EditorDocument document);
}
