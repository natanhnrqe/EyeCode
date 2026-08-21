package com.eyecode.language.documentation;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.language.Token;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.JavaTokenType;

import java.util.Set;

/** Finds a conservative declaration anchor in JDK source text. */
public final class JdkSourceDeclarationLocator {

    private static final Set<String> TYPE_KEYWORDS = Set.of("class", "interface", "enum", "record");
    private final JavaLexerService lexerService;

    public JdkSourceDeclarationLocator() {
        this(new JavaLexerService());
    }

    JdkSourceDeclarationLocator(JavaLexerService lexerService) {
        this.lexerService = lexerService;
    }

    public int find(String source, String simpleName) {
        if (source == null || simpleName == null || simpleName.isBlank()) {
            return 0;
        }
        var tokens = lexerService.lex(DocumentSnapshot.oneShot(source)).tokens();
        for (int index = 0; index < tokens.size(); index++) {
            Token keyword = tokens.get(index);
            if (keyword.type() != JavaTokenType.KEYWORD || !TYPE_KEYWORDS.contains(keyword.text())) {
                continue;
            }
            int nameIndex = index + 1;
            while (nameIndex < tokens.size()
                    && (tokens.get(nameIndex).type() == JavaTokenType.WHITESPACE
                    || tokens.get(nameIndex).type() == JavaTokenType.COMMENT)) {
                nameIndex++;
            }
            if (nameIndex >= tokens.size()) {
                continue;
            }
            Token name = tokens.get(nameIndex);
            if (name.type() == JavaTokenType.IDENTIFIER && simpleName.equals(name.text())) {
                return keyword.startOffset();
            }
        }
        return 0;
    }
}
