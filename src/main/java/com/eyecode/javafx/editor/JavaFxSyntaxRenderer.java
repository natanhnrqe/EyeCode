package com.eyecode.javafx.editor;

import com.eyecode.editor.v2.syntax.SyntaxRenderer;
import com.eyecode.editor.v2.syntax.SyntaxSnapshot;
import com.eyecode.editor.v2.syntax.SyntaxToken;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.List;

public final class JavaFxSyntaxRenderer implements SyntaxRenderer {

    private final CodeArea codeArea;

    public JavaFxSyntaxRenderer(CodeArea codeArea) {
        this.codeArea = codeArea;
    }

    @Override
    public void render(SyntaxSnapshot snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return;
        }
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        for (SyntaxToken token : snapshot.getTokens()) {
            int length = token.endOffset() - token.startOffset();
            String cssClass = "syntax-" + token.type().name().toLowerCase();
            builder.add(List.of(cssClass), length);
        }
        StyleSpans<Collection<String>> spans = builder.create();
        codeArea.setStyleSpans(0, spans);
    }
}