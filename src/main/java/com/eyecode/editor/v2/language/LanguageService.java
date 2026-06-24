package com.eyecode.editor.v2.language;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.syntax.SyntaxSnapshot;

public interface LanguageService {

    LanguageContext buildContext(EditorBuffer buffer, SyntaxSnapshot syntax);
}
