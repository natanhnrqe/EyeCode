package com.eyecode.editor.v2.language;

import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.syntax.SyntaxToken;
import com.eyecode.editor.v2.syntax.TokenType;

import java.util.Optional;

public final class LanguageContextQueries {

    private LanguageContextQueries() {
    }

    public static Optional<LanguageToken> getTokenAtCaret(LanguageContext context) {
        int offset = offsetForPosition(context, context.getCaret());
        return context.getSyntax().getTokens().stream()
                .filter(token -> offset >= token.startOffset() && offset <= token.endOffset())
                .filter(token -> token.type() != TokenType.WHITESPACE)
                .findFirst()
                .map(token -> new LanguageToken(token.type(), token.text(), token.startOffset(), token.endOffset()));
    }

    public static String getCurrentWordPrefix(LanguageContext context) {
        int offset = offsetForPosition(context, context.getCaret());
        String text = context.getDocument().getText();
        int safeOffset = Math.max(0, Math.min(offset, text.length()));
        int start = safeOffset;

        while (start > 0 && isWordPart(text.charAt(start - 1))) {
            start--;
        }

        return text.substring(start, safeOffset);
    }

    private static int offsetForPosition(LanguageContext context, EditorPosition position) {
        String text = context.getDocument().getText();
        int line = 0;
        int column = 0;

        for (int offset = 0; offset < text.length(); offset++) {
            if (line == position.line() && column == position.column()) {
                return offset;
            }

            char current = text.charAt(offset);
            if (current == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
        }

        return text.length();
    }

    private static boolean isWordPart(char c) {
        return Character.isJavaIdentifierPart(c);
    }
}
