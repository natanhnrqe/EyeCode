package com.eyecode.editor.v2.language;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.syntax.SyntaxSnapshot;

public final class DefaultLanguageService implements LanguageService {

    @Override
    public LanguageContext buildContext(EditorBuffer buffer, SyntaxSnapshot syntax) {
        return new LanguageContext(
                buffer.getDocument(),
                buffer.getCaret(),
                buffer.getSelection(),
                syntax,
                buffer.getDiagnostics()
        );
    }
}
