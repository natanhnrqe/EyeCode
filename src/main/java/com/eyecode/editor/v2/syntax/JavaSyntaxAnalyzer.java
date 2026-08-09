package com.eyecode.editor.v2.syntax;

import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.language.Token;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.JavaTokenType;
import com.eyecode.language.java.LexerService;
import com.eyecode.language.java.LexerSnapshot;

import java.util.ArrayList;
import java.util.List;

public final class JavaSyntaxAnalyzer implements SyntaxAnalyzer {

    private final LexerService lexerService = new JavaLexerService();

    @Override
    public SyntaxSnapshot analyze(EditorDocument document) {
        LexerSnapshot snapshot = lexerService.lex(document.snapshot());
        List<Token> tokens = snapshot.tokens();
        List<SyntaxToken> result = new ArrayList<>();

        int index = 0;
        while (index < tokens.size()) {
            Token token = tokens.get(index);

            if (token.type() == JavaTokenType.EOF) {
                break;
            }

            if (isAnnotationStart(token, tokens, index)) {
                Token name = tokens.get(index + 1);
                result.add(new SyntaxToken(
                        TokenType.ANNOTATION,
                        token.startOffset(),
                        name.endOffset(),
                        token.text() + name.text()
                ));
                index += 2;
                continue;
            }

            com.eyecode.language.TokenType rawType = token.type();
            TokenType uiType = rawType instanceof JavaTokenType javaType
                    ? SyntaxTokenTypeMapper.map(javaType)
                    : null;
            if (uiType != null) {
                result.add(new SyntaxToken(
                        uiType,
                        token.startOffset(),
                        token.endOffset(),
                        token.text()
                ));
            }
            index++;
        }

        return new SyntaxSnapshot(result);
    }

    private static boolean isAnnotationStart(Token token, List<Token> tokens, int index) {
        return token.type() == JavaTokenType.AT
                && index + 1 < tokens.size()
                && tokens.get(index + 1).type() == JavaTokenType.IDENTIFIER;
    }
}
