package com.eyecode.editor.v2.language;

import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.EditorSelection;
import com.eyecode.editor.v2.diagnostics.DiagnosticSnapshot;
import com.eyecode.editor.v2.syntax.SyntaxSnapshot;

import java.util.Objects;

public final class LanguageContext {

    private final EditorDocument document;
    private final EditorPosition caret;
    private final EditorSelection selection;
    private final SyntaxSnapshot syntax;
    private final DiagnosticSnapshot diagnostics;

    public LanguageContext(EditorDocument document, EditorPosition caret, EditorSelection selection,
                           SyntaxSnapshot syntax, DiagnosticSnapshot diagnostics) {
        this.document = Objects.requireNonNull(document);
        this.caret = Objects.requireNonNull(caret);
        this.selection = Objects.requireNonNull(selection);
        this.syntax = Objects.requireNonNull(syntax);
        this.diagnostics = Objects.requireNonNull(diagnostics);
    }

    public EditorDocument getDocument() { return document; }

    public EditorPosition getCaret() { return caret; }

    public EditorSelection getSelection() { return selection; }

    public SyntaxSnapshot getSyntax() { return syntax; }

    public DiagnosticSnapshot getDiagnostics() { return diagnostics; }
}
