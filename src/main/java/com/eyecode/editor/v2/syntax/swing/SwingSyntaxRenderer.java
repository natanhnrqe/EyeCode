package com.eyecode.editor.v2.syntax.swing;

import com.eyecode.editor.v2.syntax.DocumentStyleRegistry;
import com.eyecode.editor.v2.syntax.StyleDefinition;
import com.eyecode.editor.v2.syntax.SyntaxRenderer;
import com.eyecode.editor.v2.syntax.SyntaxSnapshot;
import com.eyecode.editor.v2.syntax.SyntaxToken;

import javax.swing.text.AttributeSet;
import javax.swing.text.StyledDocument;

public final class SwingSyntaxRenderer implements SyntaxRenderer {

    private final StyledDocument document;
    private final DocumentStyleRegistry registry;
    private final SwingStyleMapper styleMapper;

    public SwingSyntaxRenderer(StyledDocument document, DocumentStyleRegistry registry) {
        this.document = document;
        this.registry = registry;
        this.styleMapper = new SwingStyleMapper();
    }

    @Override
    public void render(SyntaxSnapshot snapshot) {
        if (snapshot == null || snapshot.isEmpty()) return;

        for (SyntaxToken token : snapshot.getTokens()) {
            StyleDefinition definition = registry.getFor(token.type());
            AttributeSet attributes = styleMapper.toAttributes(definition);
            document.setCharacterAttributes(
                    token.startOffset(),
                    token.endOffset() - token.startOffset(),
                    attributes,
                    true
            );
        }
    }
}
