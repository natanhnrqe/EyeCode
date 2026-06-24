package com.eyecode.editor.v2.language;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.syntax.SyntaxSnapshot;

public final class LanguageManager {

    private final LanguageService service;
    private LanguageContext currentContext;

    public LanguageManager(LanguageService service) {
        this.service = service;
    }

    public void refresh(EditorBuffer buffer, SyntaxSnapshot syntax) {
        currentContext = service.buildContext(buffer, syntax);
    }

    public LanguageContext getContext() {
        return currentContext;
    }
}
