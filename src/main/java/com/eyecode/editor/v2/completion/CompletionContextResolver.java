package com.eyecode.editor.v2.completion;

import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.syntax.SyntaxToken;
import com.eyecode.editor.v2.syntax.TokenType;

import java.util.ArrayDeque;
import java.util.Deque;

public final class CompletionContextResolver {

    private CompletionContextResolver() {
    }

    public static CompletionContextKind resolve(LanguageContext context) {
        String text = context.getDocument().getText();
        int offset = Math.max(0, Math.min(context.getDocument().offsetOf(context.getCaret()), text.length()));
        int prefixStart = offset;
        while (prefixStart > 0 && Character.isJavaIdentifierPart(text.charAt(prefixStart - 1))) {
            prefixStart--;
        }
        if (prefixStart > 0 && text.charAt(prefixStart - 1) == '.') {
            return CompletionContextKind.MEMBER_ACCESS;
        }
        int lineStart = Math.max(0, text.lastIndexOf('\n', Math.max(0, offset - 1)) + 1);
        if (text.substring(lineStart, prefixStart).trim().startsWith("import")) {
            return CompletionContextKind.IMPORT;
        }
        if (prefixStart < offset && Character.isUpperCase(text.charAt(prefixStart))) {
            return CompletionContextKind.TYPE;
        }
        return CompletionContextKind.IDENTIFIER;
    }

    public static boolean isMethodBodyExpressionContext(LanguageContext context) {
        if (context == null || context.getSyntax() == null) {
            return false;
        }
        int offset = context.getDocument().offsetOf(context.getCaret());
        Deque<Integer> braces = new ArrayDeque<>();
        int openBrace = -1;
        var tokens = context.getSyntax().getTokens();
        for (int index = 0; index < tokens.size(); index++) {
            SyntaxToken token = tokens.get(index);
            if (token.startOffset() >= offset) {
                break;
            }
            if ("{".equals(token.text())) {
                braces.push(index);
            } else if ("}".equals(token.text()) && !braces.isEmpty()) {
                braces.pop();
            }
        }
        if (!braces.isEmpty()) {
            openBrace = braces.peek();
        }
        if (openBrace < 0) {
            return false;
        }
        for (int index = openBrace - 1; index >= 0; index--) {
            SyntaxToken token = tokens.get(index);
            if (token.type() == TokenType.WHITESPACE || token.type() == TokenType.COMMENT) {
                continue;
            }
            return ")".equals(token.text()) || "->".equals(token.text())
                    || "else".equals(token.text()) || "try".equals(token.text())
                    || "finally".equals(token.text()) || "do".equals(token.text());
        }
        return false;
    }
}
